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
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;

class MessageListing extends JsonParser implements Listing {

    public static final String TAG = "MessageListing";

    private static final String[] MERGE_PROJECTION = {
            MessageActions._ID,
            MessageActions.COLUMN_ACCOUNT,
            MessageActions.COLUMN_ACTION,
            MessageActions.COLUMN_THING_ID,
            MessageActions.COLUMN_TEXT,
    };

    private static final int MERGE_INDEX_ACCOUNT = 1;
    private static final int MERGE_INDEX_ACTION = 2;
    private static final int MERGE_INDEX_THING_ID = 3;
    private static final int MERGE_INDEX_TEXT = 4;

    private static final String MERGE_SELECTION = MessageActions.COLUMN_ACCOUNT + "=? AND "
            + MessageActions.COLUMN_PARENT_THING_ID + "=?";

    private static final String MERGE_SORT = MessageActions._ID + " ASC";

    private final int listingType;
    private final String accountName;
    private final String thingId;
    private final int filter;
    private final String more;
    private final boolean mark;
    private final String cookie;
    private final SQLiteOpenHelper dbHelper;
    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(30);

    private long networkTimeMs;
    private long parseTimeMs;

    /** Returns a listing of messages for inbox or sent. */
    static MessageListing newInstance(String accountName, int filter, String more, boolean mark,
            String cookie) {
        return new MessageListing(null, Listing.TYPE_MESSAGE_LISTING, accountName,
                null, filter, more, mark, cookie);
    }

    /** Returns an instance for a message thread. */
    static MessageListing newThreadInstance(String accountName, String thingId, String cookie,
            SQLiteOpenHelper dbHelper) {
        return new MessageListing(dbHelper, Listing.TYPE_MESSAGE_THREAD_LISTING, accountName,
                thingId, 0, null, false, cookie);
    }

    private MessageListing(SQLiteOpenHelper dbHelper, int listingType, String accountName,
            String thingId, int filter, String more, boolean mark, String cookie) {
        this.dbHelper = dbHelper;
        this.listingType = listingType;
        this.accountName = accountName;
        this.thingId = thingId;
        this.filter = filter;
        this.more = more;
        this.mark = mark;
        this.cookie = cookie;
    }

    public ArrayList<ContentValues> getValues() throws IOException {
        long t1 = System.currentTimeMillis();
        HttpURLConnection conn = RedditApi.connect(getUrl(), cookie, true, false);
        InputStream input = new BufferedInputStream(conn.getInputStream());
        long t2 = System.currentTimeMillis();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingObject(reader);
            if (BuildConfig.DEBUG) {
                long t3 = System.currentTimeMillis();
                networkTimeMs = t2 - t1;
                parseTimeMs = t3 - t2;
            }
            return values;
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    private CharSequence getUrl() {
        switch (listingType) {
            case Listing.TYPE_MESSAGE_LISTING:
                return Urls.message(filter, more, mark);

            case Listing.TYPE_MESSAGE_THREAD_LISTING:
                return Urls.messageThread(thingId, Urls.TYPE_JSON);

            default:
                throw new IllegalArgumentException();
        }
    }

    public void performExtraWork(Context context) {
        if (mark) {
            Provider.clearNewMessageIndicator(context, accountName, false);
        }
    }

    public void addCursorExtras(Bundle bundle) {
    }

    public long getNetworkTimeMs() {
        return networkTimeMs;
    }

    public long getParseTimeMs() {
        return parseTimeMs;
    }

    public String getTargetTable() {
        return Messages.TABLE_NAME;
    }

    public boolean isAppend() {
        return false;
    }

    @Override
    public void onEntityStart(int index) {
        values.add(newContentValues(11));
    }

    private ContentValues newContentValues(int capacity) {
        ContentValues values = new ContentValues(capacity + 1);
        values.put(Messages.COLUMN_ACCOUNT, accountName);
        return values;
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_AUTHOR, reader.nextString());
    }

    @Override
    public void onBody(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_BODY, reader.nextString());
    }

    @Override
    public void onContext(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_CONTEXT, reader.nextString());
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_CREATED_UTC, reader.nextLong());
    }

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_KIND, Kinds.parseKind(reader.nextString()));
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_THING_ID, reader.nextString());
    }

    @Override
    public void onNew(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_NEW, reader.nextBoolean());
    }

    @Override
    public void onSubject(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_SUBJECT, reader.nextString());
    }

    @Override
    public void onSubreddit(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_SUBREDDIT, readTrimmedString(reader, null));
    }

    @Override
    public void onWasComment(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_WAS_COMMENT, reader.nextBoolean());
    }

    @Override
    public boolean shouldParseReplies() {
        return true;
    }

    @Override
    public void onParseEnd() {
        if (listingType == Listing.TYPE_MESSAGE_THREAD_LISTING) {
            mergeThreadActions();
        }
    }

    private void mergeThreadActions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(MessageActions.TABLE_NAME, MERGE_PROJECTION,
                MERGE_SELECTION, Array.of(accountName, thingId), null, null, MERGE_SORT);
        try {
            while (c.moveToNext()) {
                String actionAccountName = c.getString(MERGE_INDEX_ACCOUNT);
                int action = c.getInt(MERGE_INDEX_ACTION);
                String targetId = c.getString(MERGE_INDEX_THING_ID);
                String text = c.getString(MERGE_INDEX_TEXT);
                switch (action) {
                    case MessageActions.ACTION_INSERT:
                        insertThing(actionAccountName, targetId, text);
                        break;

                    default:
                        throw new IllegalStateException();
                }
            }
        } finally {
            c.close();
        }
    }

    private void insertThing(String actionAccountName, String targetId, String body) {
        // Allocate extra space for session ID later.
        ContentValues p = new ContentValues(4 + 1);
        p.put(Messages.COLUMN_ACCOUNT, actionAccountName);
        p.put(Messages.COLUMN_AUTHOR, actionAccountName);
        p.put(Messages.COLUMN_BODY, body);
        p.put(Messages.COLUMN_KIND, Kinds.KIND_MESSAGE);

        // Just append to the bottom for messages.
        values.add(p);
    }
}
