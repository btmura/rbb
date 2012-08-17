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
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.database.Comments;

class CommentListing extends JsonParser {

    public static final String TAG = "CommentListing";
    public static final boolean DEBUG = Debug.DEBUG;

    final ArrayList<ContentValues> values = new ArrayList<ContentValues>(360);

    private final Formatter formatter = new Formatter();
    private final Context context;
    private final String accountName;
    private final String cookie;
    private final String thingId;
    private final URL url;

    CommentListing(Context context, String accountName, String cookie, String thingId) {
        this.context = context;
        this.accountName = accountName;
        this.cookie = cookie;
        this.thingId = thingId;
        this.url = Urls.commentsUrl(thingId);
    }

    public void process() throws IOException {
        long t1 = System.currentTimeMillis();
        HttpURLConnection conn = NetApi.connect(context, url, cookie);
        InputStream input = new BufferedInputStream(conn.getInputStream());
        long t2 = System.currentTimeMillis();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingArray(reader);
            if (DEBUG) {
                long t3 = System.currentTimeMillis();
                Log.d(TAG, "net: " + (t2 - t1) + " parse: " + (t3 - t2));
            }
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    @Override
    public boolean shouldParseReplies() {
        return true;
    }

    @Override
    public void onEntityStart(int index) {
        ContentValues v = new ContentValues(15);
        v.put(Comments.COLUMN_SEQUENCE, index);
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
    public void onEntityEnd(int index) {
        values.get(index).put(Comments.COLUMN_ACCOUNT, accountName);
        values.get(index).put(Comments.COLUMN_SESSION_ID, thingId);
    }

    @Override
    public void onParseEnd() {
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
}