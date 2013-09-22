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
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;

class MessageListing extends JsonParser implements Listing {

    public static final String TAG = "MessageListing";

    private static final String[] MERGE_PROJECTION = {
            MessageActions._ID,
            MessageActions.COLUMN_ACTION,
            MessageActions.COLUMN_TEXT,
    };

    private static final int MERGE_ID = 0;
    private static final int MERGE_ACTION = 1;
    private static final int MERGE_TEXT = 2;

    private static final String MERGE_SELECTION = MessageActions.COLUMN_ACCOUNT + "=? AND "
            + MessageActions.COLUMN_PARENT_THING_ID + "=?";

    private final SQLiteOpenHelper dbHelper;
    private final int sessionType;
    private final String accountName;
    private final String thingId;
    private final int filter;
    private final String more;
    private final boolean mark;
    private final String cookie;

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(30);
    private Map<String, Integer> readActionMap;
    private String moreThingId;

    /** Returns a listing of messages for inbox or sent. */
    static MessageListing newInstance(SQLiteOpenHelper dbHelper,
            String accountName,
            int filter,
            String more,
            boolean mark,
            String cookie) {
        return new MessageListing(dbHelper,
                Sessions.TYPE_MESSAGES,
                accountName,
                null,
                filter,
                more,
                mark,
                cookie);
    }

    /** Returns an instance for a message thread. */
    static MessageListing newThreadInstance(SQLiteOpenHelper dbHelper,
            String accountName,
            String thingId,
            String cookie) {
        return new MessageListing(dbHelper,
                Sessions.TYPE_MESSAGE_THREAD,
                accountName,
                thingId,
                0,
                null,
                false,
                cookie);
    }

    private MessageListing(SQLiteOpenHelper dbHelper,
            int sessionType,
            String accountName,
            String thingId,
            int filter,
            String more,
            boolean mark,
            String cookie) {
        this.dbHelper = dbHelper;
        this.sessionType = sessionType;
        this.accountName = accountName;
        this.thingId = thingId;
        this.filter = filter;
        this.more = more;
        this.mark = mark;
        this.cookie = cookie;
    }

    @Override
    public int getSessionType() {
        return sessionType;
    }

    @Override
    public String getSessionThingId() {
        return thingId;
    }

    @Override
    public ArrayList<ContentValues> getValues() throws IOException {
        HttpURLConnection conn = RedditApi.connect(getUrl(), cookie, true, false);
        InputStream input = null;
        readActionMap = ReadMerger.getActionMap(dbHelper, accountName);
        try {
            input = new BufferedInputStream(conn.getInputStream());
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingObject(reader);
            return values;
        } finally {
            if (input != null) {
                input.close();
            }
            conn.disconnect();
        }
    }

    private CharSequence getUrl() {
        switch (sessionType) {
            case Sessions.TYPE_MESSAGES:
                return Urls.message(filter, more, mark);

            case Sessions.TYPE_MESSAGE_THREAD:
                return Urls.messageThread(thingId, Urls.TYPE_JSON);

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void performExtraWork(Context context) {
        if (mark) {
            Provider.clearNewMessageIndicator(context, accountName, false);
        }
    }

    @Override
    public String getTargetTable() {
        return Messages.TABLE_NAME;
    }

    @Override
    public boolean isAppend() {
        return !TextUtils.isEmpty(more);
    }

    @Override
    public void onEntityStart(int index) {
        values.add(newContentValues(14));
    }

    private ContentValues newContentValues(int capacity) {
        ContentValues values = new ContentValues(capacity);
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
    public void onDestination(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_DESTINATION, reader.nextString());
    }

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_KIND, Kinds.parseKind(reader.nextString()));
    }

    @Override
    public void onLinkTitle(JsonReader reader, int index) throws IOException {
        values.get(index).put(Messages.COLUMN_LINK_TITLE, reader.nextString());
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
        values.get(index).put(Messages.COLUMN_SUBREDDIT, readString(reader, null));
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
    public void onAfter(JsonReader reader) throws IOException {
        moreThingId = readString(reader, null);
    }

    @Override
    public void onParseEnd() {
        if (sessionType == Sessions.TYPE_MESSAGE_THREAD) {
            mergeThreadActions();
        }

        doFinalMerge();
    }

    private void mergeThreadActions() {
        if (values.isEmpty()) {
            return; // Something went wrong.
        }

        // Message threads are odd in that the thing ID doesn't refer to the
        // topmost message, so the actions may not match up with that ID.
        // So get the parent id from the first element.
        String parentId = values.get(0).getAsString(Messages.COLUMN_THING_ID);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(MessageActions.TABLE_NAME,
                MERGE_PROJECTION,
                MERGE_SELECTION,
                Array.of(accountName, parentId),
                null,
                null,
                SharedColumns.SORT_BY_ID);
        while (c.moveToNext()) {
            switch (c.getInt(MERGE_ACTION)) {
                case MessageActions.ACTION_INSERT:
                    insertMessage(c);
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
        c.close();
    }

    private void insertMessage(Cursor c) {
        ContentValues v = new ContentValues(5 + 1); // +1 for session id.
        v.put(Messages.COLUMN_ACCOUNT, accountName);
        v.put(Messages.COLUMN_AUTHOR, accountName);
        v.put(Messages.COLUMN_BODY, c.getString(MERGE_TEXT));
        v.put(Messages.COLUMN_KIND, Kinds.KIND_MESSAGE);
        v.put(Messages.COLUMN_MESSAGE_ACTION_ID, c.getLong(MERGE_ID));

        // Just append to the bottom for messages.
        values.add(v);
    }

    private void doFinalMerge() {
        if (sessionType == Sessions.TYPE_MESSAGES) {
            int count = values.size();
            for (int i = 0; i < count; i++) {
                ContentValues v = values.get(i);
                ReadMerger.updateContentValues(v, readActionMap);
            }
        }
        appendLoadingMore();
    }

    private void appendLoadingMore() {
        if (!TextUtils.isEmpty(moreThingId)) {
            ContentValues v = new ContentValues(3 + 1); // 1 for session id.
            v.put(Messages.COLUMN_ACCOUNT, accountName);
            v.put(Messages.COLUMN_KIND, Kinds.KIND_MORE);
            v.put(Messages.COLUMN_THING_ID, moreThingId);
            values.add(v);
        }
    }
}
