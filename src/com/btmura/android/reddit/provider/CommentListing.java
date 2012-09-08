/*
 * Copyright (C) 2012 Brian Muramatsu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmura.android.reddit.provider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;

class CommentListing extends JsonParser {

    public static final String TAG = "CommentListing";

    private static final String[] PROJECTION = {
            CommentActions._ID,
            CommentActions.COLUMN_THING_ID,
            CommentActions.COLUMN_TEXT,
    };

    private static final int INDEX_THING_ID = 1;
    private static final int INDEX_TEXT = 2;

    public final ArrayList<ContentValues> values = new ArrayList<ContentValues>(360);
    public long networkTimeMs;
    public long parseTimeMs;

    private final Formatter formatter = new Formatter();
    private final Context context;
    private final SQLiteOpenHelper dbHelper;
    private final String accountName;
    private final String sessionId;
    private final long sessionTimestamp;
    private final String thingId;

    public static CommentListing get(Context context, SQLiteOpenHelper dbHelper,
            String accountName, String sessionId, long sessionTimestamp, String thingId,
            String cookie) throws IOException {
        long t1 = System.currentTimeMillis();
        URL url = Urls.commentsUrl(thingId);
        HttpURLConnection conn = RedditApi.connect(url, cookie, false);
        InputStream input = new BufferedInputStream(conn.getInputStream());
        long t2 = System.currentTimeMillis();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            CommentListing listing = new CommentListing(context, dbHelper, accountName, sessionId,
                    sessionTimestamp, thingId);
            listing.parseListingArray(reader);
            if (BuildConfig.DEBUG) {
                long t3 = System.currentTimeMillis();
                listing.networkTimeMs = t2 - t1;
                listing.parseTimeMs = t3 - t2;
            }
            return listing;
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    private CommentListing(Context context, SQLiteOpenHelper dbHelper, String accountName,
            String sessionId, long sessionTimestamp, String thingId) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.accountName = accountName;
        this.sessionId = sessionId;
        this.sessionTimestamp = sessionTimestamp;
        this.thingId = thingId;
    }

    @Override
    public boolean shouldParseReplies() {
        return true;
    }

    @Override
    public void onEntityStart(int index) {
        ContentValues v = new ContentValues(15);
        v.put(Comments.COLUMN_ACCOUNT, accountName);
        v.put(Comments.COLUMN_SEQUENCE, index);
        v.put(Comments.COLUMN_SESSION_ID, sessionId);
        v.put(Comments.COLUMN_SESSION_TIMESTAMP, sessionTimestamp);
        values.add(v);
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_AUTHOR, readTrimmedString(reader, ""));
    }

    @Override
    public void onBody(JsonReader reader, int index) throws IOException {
        CharSequence body = formatter.formatNoSpans(context, readTrimmedString(reader, ""));
        values.get(index).put(Comments.COLUMN_BODY, body.toString());
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_CREATED_UTC, reader.nextLong());
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_DOWNS, reader.nextInt());
    }

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        ContentValues v = values.get(index);
        v.put(Comments.COLUMN_NESTING, replyNesting);

        String kind = reader.nextString();
        if ("more".equalsIgnoreCase(kind)) {
            v.put(Comments.COLUMN_KIND, Comments.KIND_MORE);
        } else if (index != 0) {
            v.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
        } else {
            v.put(Comments.COLUMN_KIND, Comments.KIND_HEADER);
        }
    }

    @Override
    public void onLikes(JsonReader reader, int index) throws IOException {
        int likes = 0;
        if (reader.peek() == JsonToken.BOOLEAN) {
            likes = reader.nextBoolean() ? 1 : -1;
        } else {
            reader.skipValue();
        }
        values.get(index).put(Comments.COLUMN_LIKES, likes);
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        String id = readTrimmedString(reader, "");
        values.get(index).put(Comments.COLUMN_THING_ID, id);
    }

    @Override
    public void onNumComments(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_NUM_COMMENTS, reader.nextInt());
    }

    @Override
    public void onSelfText(JsonReader reader, int index) throws IOException {
        CharSequence body = formatter.formatNoSpans(context, readTrimmedString(reader, ""));
        values.get(index).put(Comments.COLUMN_BODY, body.toString());
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        CharSequence title = formatter.formatNoSpans(context, readTrimmedString(reader, ""));
        values.get(index).put(Comments.COLUMN_TITLE, title.toString());
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_UPS, reader.nextInt());
    }

    @Override
    public void onParseEnd() {
        // We don't support loading more comments right now.
        removeMoreComments();

        // Merge pending replies that haven't been synced yet.
        mergeReplies();
    }

    private void removeMoreComments() {
        int size = values.size();
        for (int i = 0; i < size;) {
            ContentValues v = values.get(i);
            Integer type = v.getAsInteger(Comments.COLUMN_KIND);
            if (type.intValue() == Comments.KIND_MORE) {
                values.remove(i);
                size--;
            } else {
                i++;
            }
        }
    }

    private void mergeReplies() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(CommentActions.TABLE_NAME, PROJECTION,
                CommentActions.SELECTION_BY_ACCOUNT_AND_PARENT_THING_ID,
                Array.of(accountName, thingId),
                null, null, CommentActions.SORT_BY_ID);

        // TODO: Take into account the size of replies + values?

        try {
            while (c.moveToNext()) {
                int size = values.size();
                for (int i = 0; i < size; i++) {
                    ContentValues v = values.get(i);
                    String thingId = v.getAsString(Comments.COLUMN_THING_ID);

                    // This thing could be a placeholder we previously inserted.
                    if (TextUtils.isEmpty(thingId)) {
                        continue;
                    }

                    String replyId = c.getString(INDEX_THING_ID);
                    if (thingId.equals(replyId)) {
                        String body = c.getString(INDEX_TEXT);

                        // TODO: Remove duplicate logic from
                        // CommentListFragment.

                        // Nest an additional level if this is a response to a
                        // comment.
                        int nesting = v.getAsInteger(Comments.COLUMN_NESTING);
                        if (i != 0) {
                            nesting++;
                        }

                        // Use the same sequence so this appears below.
                        int sequence = v.getAsInteger(Comments.COLUMN_SEQUENCE);

                        ContentValues p = new ContentValues(8);
                        p.put(Comments.COLUMN_ACCOUNT, accountName);
                        p.put(Comments.COLUMN_AUTHOR, accountName);
                        p.put(Comments.COLUMN_BODY, body);
                        p.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
                        p.put(Comments.COLUMN_NESTING, nesting);
                        p.put(Comments.COLUMN_SEQUENCE, sequence);
                        p.put(Comments.COLUMN_SESSION_ID, sessionId);
                        p.put(Comments.COLUMN_SESSION_TIMESTAMP, sessionTimestamp);

                        // Insert the reply after this comment.
                        values.add(i + 1, p);
                        size++;
                        i++;
                    }
                }

            }
        } finally {
            c.close();
        }
    }
}