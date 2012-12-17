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
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.JsonParser;
import com.btmura.android.reddit.widget.FilterAdapter;

class MessageListing extends JsonParser implements Listing {

    private static final String TAG = "MessageListing";

    private static final boolean DEBUG = BuildConfig.DEBUG && true;

    private final int listingType;
    private final String accountName;
    private final String thingId;
    private final String cookie;

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>();
    private long networkTimeMs;
    private long parseTimeMs;

    /** Returns a listing for an inbox. */
    static MessageListing newInboxInstance(String accountName, String cookie) {
        return new MessageListing(Sessions.TYPE_MESSAGE_INBOX_LISTING,
                accountName, null, cookie);
    }

    /** Returns a listing for sent messages. */
    static MessageListing newSentInstance(String accountName, String cookie) {
        return new MessageListing(Sessions.TYPE_MESSAGE_SENT_LISTING,
                accountName, null, cookie);
    }

    /** Returns an instance for a message thread. */
    static MessageListing newThreadInstance(String accountName, String thingId, String cookie) {
        return new MessageListing(Sessions.TYPE_MESSAGE_THREAD_LISTING,
                accountName, thingId, cookie);
    }

    private MessageListing(int listingType, String accountName, String thingId, String cookie) {
        this.listingType = listingType;
        this.accountName = accountName;
        this.thingId = thingId;
        this.cookie = cookie;
    }

    public int getType() {
        return listingType;
    }

    public ArrayList<ContentValues> getValues() throws IOException {
        long t1 = System.currentTimeMillis();
        HttpURLConnection conn = RedditApi.connect(getUrl(), cookie, false);
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

    private URL getUrl() {
        switch (listingType) {
            case Sessions.TYPE_MESSAGE_INBOX_LISTING:
                return Urls.messageUrl(FilterAdapter.MESSAGE_INBOX, null);

            case Sessions.TYPE_MESSAGE_SENT_LISTING:
                return Urls.messageUrl(FilterAdapter.MESSAGE_SENT, null);

            case Sessions.TYPE_MESSAGE_THREAD_LISTING:
                return Urls.messageThreadUrl(thingId);

            default:
                throw new IllegalArgumentException();
        }
    }

    public long getNetworkTimeMs() {
        return networkTimeMs;
    }

    public long getParseTimeMs() {
        return parseTimeMs;
    }

    @Override
    public void onEntityStart(int index) {
        values.add(newContentValues(9));
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
    public void onSubreddit(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_SUBREDDIT, readTrimmedString(reader, null));
    }

    @Override
    public void onWasComment(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_WAS_COMMENT, reader.nextBoolean());
    }

    @Override
    public void onEntityEnd(int index) {
        if (DEBUG) {
            Log.d(TAG, "values: " + values.get(index));
        }
    }

    @Override
    public boolean shouldParseReplies() {
        return true;
    }
}
