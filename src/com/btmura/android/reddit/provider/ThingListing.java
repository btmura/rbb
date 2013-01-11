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
import android.util.JsonToken;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;
import com.btmura.android.reddit.widget.FilterAdapter;

class ThingListing extends JsonParser implements Listing {

    public static final String TAG = "ThingListing";

    private static final String[] SAVE_PROJECTION = {
            SaveActions._ID,
            SaveActions.COLUMN_ACTION,
            SaveActions.COLUMN_AUTHOR,
            SaveActions.COLUMN_CREATED_UTC,
            SaveActions.COLUMN_DOMAIN,
            SaveActions.COLUMN_DOWNS,
            SaveActions.COLUMN_LIKES,
            SaveActions.COLUMN_NUM_COMMENTS,
            SaveActions.COLUMN_OVER_18,
            SaveActions.COLUMN_PERMA_LINK,
            SaveActions.COLUMN_SCORE,
            SaveActions.COLUMN_SELF,
            SaveActions.COLUMN_SUBREDDIT,
            SaveActions.COLUMN_THING_ID,
            SaveActions.COLUMN_TITLE,
            SaveActions.COLUMN_THUMBNAIL_URL,
            SaveActions.COLUMN_UPS,
            SaveActions.COLUMN_URL,
    };

    private static final int SAVE_ACTION = 1;
    private static final int SAVE_AUTHOR = 2;
    private static final int SAVE_CREATED_UTC = 3;
    private static final int SAVE_DOMAIN = 4;
    private static final int SAVE_DOWNS = 5;
    private static final int SAVE_LIKES = 6;
    private static final int SAVE_NUM_COMMENTS = 7;
    private static final int SAVE_OVER_18 = 8;
    private static final int SAVE_PERMA_LINK = 9;
    private static final int SAVE_SCORE = 10;
    private static final int SAVE_SELF = 11;
    private static final int SAVE_SUBREDDIT = 12;
    private static final int SAVE_THING_ID = 13;
    private static final int SAVE_TITLE = 14;
    private static final int SAVE_THUMBNAIL_URL = 15;
    private static final int SAVE_UPS = 16;
    private static final int SAVE_URL = 17;

    private static final String[] VOTE_PROJECTION = {
            VoteActions._ID,
            VoteActions.COLUMN_ACTION,
            VoteActions.COLUMN_AUTHOR,
            VoteActions.COLUMN_CREATED_UTC,
            VoteActions.COLUMN_DOMAIN,
            VoteActions.COLUMN_DOWNS,
            VoteActions.COLUMN_LIKES,
            VoteActions.COLUMN_NUM_COMMENTS,
            VoteActions.COLUMN_OVER_18,
            VoteActions.COLUMN_PERMA_LINK,
            VoteActions.COLUMN_SCORE,
            VoteActions.COLUMN_SELF,
            VoteActions.COLUMN_SUBREDDIT,
            VoteActions.COLUMN_THING_ID,
            VoteActions.COLUMN_TITLE,
            VoteActions.COLUMN_THUMBNAIL_URL,
            VoteActions.COLUMN_UPS,
            VoteActions.COLUMN_URL,
    };

    private static final int VOTE_ACTION = 1;
    private static final int VOTE_AUTHOR = 2;
    private static final int VOTE_CREATED_UTC = 3;
    private static final int VOTE_DOMAIN = 4;
    private static final int VOTE_DOWNS = 5;
    private static final int VOTE_LIKES = 6;
    private static final int VOTE_NUM_COMMENTS = 7;
    private static final int VOTE_OVER_18 = 8;
    private static final int VOTE_PERMA_LINK = 9;
    private static final int VOTE_SCORE = 10;
    private static final int VOTE_SELF = 11;
    private static final int VOTE_SUBREDDIT = 12;
    private static final int VOTE_THING_ID = 13;
    private static final int VOTE_TITLE = 14;
    private static final int VOTE_THUMBNAIL_URL = 15;
    private static final int VOTE_UPS = 16;
    private static final int VOTE_URL = 17;

    private final Formatter formatter = new Formatter();
    private final Context context;
    private final SQLiteOpenHelper dbHelper;
    private final String accountName;
    private final String subreddit;
    private final String query;
    private final String profileUser;
    private final int filter;
    private final String more;
    private final String cookie;

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(30);
    private long networkTimeMs;
    private long parseTimeMs;

    private String resolvedSubreddit;
    private String moreThingId;

    static ThingListing newSearchInstance(Context context, SQLiteOpenHelper dbHelper,
            String accountName, String subreddit, String query, String cookie) {
        return new ThingListing(context, dbHelper, accountName, subreddit, query, null, 0,
                null, cookie);
    }

    static ThingListing newSubredditInstance(Context context, SQLiteOpenHelper dbHelper,
            String accountName, String subreddit, int filter, String more, String cookie) {
        return new ThingListing(context, dbHelper, accountName, subreddit, null, null, filter,
                more, cookie);
    }

    static ThingListing newUserInstance(Context context, SQLiteOpenHelper dbHelper,
            String accountName, String profileUser, int filter, String more, String cookie) {
        return new ThingListing(context, dbHelper, accountName, null, null, profileUser, filter,
                more, cookie);
    }

    private ThingListing(Context context, SQLiteOpenHelper dbHelper, String accountName,
            String subreddit, String query, String profileUser, int filter, String more,
            String cookie) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.accountName = accountName;
        this.subreddit = subreddit;
        this.query = query;
        this.profileUser = profileUser;
        this.filter = filter;
        this.more = more;
        this.cookie = cookie;
    }

    public ArrayList<ContentValues> getValues() throws IOException {
        long t1 = System.currentTimeMillis();

        // Always follow redirects unless we are going to /r/random. We need to
        // catch this case so we can goto the JSON version.
        boolean followRedirects = true;

        CharSequence url;
        if (!TextUtils.isEmpty(profileUser)) {
            url = Urls.user(profileUser, filter, more, Urls.TYPE_JSON);
        } else if (!TextUtils.isEmpty(query)) {
            url = Urls.search(subreddit, query, more);
        } else {
            url = Urls.subreddit(subreddit, filter, more, Urls.TYPE_JSON);
            followRedirects = !Subreddits.isRandom(subreddit);
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getValues url: " + url);
        }

        HttpURLConnection conn = RedditApi.connect(url, cookie, followRedirects, false);

        // Handle the redirect if the request was for /r/random to use JSON.
        if (!followRedirects && conn.getResponseCode() == 302) {
            String location = conn.getHeaderField("Location");
            if (location.startsWith("/r/")) {
                location = location.substring(3);
            }
            if (location.length() > 1 && location.endsWith("/")) {
                location = location.substring(0, location.length() - 1);
            }

            url = Urls.subreddit(location, filter, more, Urls.TYPE_JSON);
            resolvedSubreddit = location;

            // Disconnect and reconnect to the proper URL.
            conn.disconnect();
            conn = RedditApi.connect(url, cookie, false, false);
        }

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

    public void performExtraWork(Context context) {
    }

    public void addCursorExtras(Bundle bundle) {
        if (resolvedSubreddit != null) {
            bundle.putString(ThingProvider.EXTRA_RESOLVED_SUBREDDIT, resolvedSubreddit);
        }
    }

    public long getNetworkTimeMs() {
        return networkTimeMs;
    }

    public long getParseTimeMs() {
        return parseTimeMs;
    }

    public String getTargetTable() {
        return Things.TABLE_NAME;
    }

    public boolean isAppend() {
        return !TextUtils.isEmpty(more);
    }

    @Override
    public void onEntityStart(int index) {
        // Pass -1 and null since we don't know those until later
        values.add(newContentValues(-1, null, 18));
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_AUTHOR, readTrimmedString(reader, ""));
    }

    @Override
    public void onBody(JsonReader reader, int index) throws IOException {
        CharSequence body = formatter.formatNoSpans(context, readTrimmedString(reader, ""));
        values.get(index).put(Things.COLUMN_BODY, body.toString());
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_CREATED_UTC, reader.nextLong());
    }

    @Override
    public void onDomain(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_DOMAIN, readTrimmedString(reader, ""));
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_DOWNS, reader.nextInt());
    }

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        int kindValue = Kinds.parseKind(reader.nextString());
        values.get(index).put(Things.COLUMN_KIND, kindValue);
    }

    @Override
    public void onLikes(JsonReader reader, int index) throws IOException {
        int likes = 0;
        if (reader.peek() == JsonToken.BOOLEAN) {
            likes = reader.nextBoolean() ? 1 : -1;
        } else {
            reader.skipValue();
        }
        values.get(index).put(Things.COLUMN_LIKES, likes);
    }

    @Override
    public void onLinkId(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_LINK_ID, reader.nextString());
    }

    @Override
    public void onLinkTitle(JsonReader reader, int index) throws IOException {
        CharSequence title = formatter.formatNoSpans(context, readTrimmedString(reader, ""));
        values.get(index).put(Things.COLUMN_LINK_TITLE, title.toString());
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        String name = readTrimmedString(reader, "");
        values.get(index).put(Things.COLUMN_THING_ID, name);
    }

    @Override
    public void onNumComments(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_NUM_COMMENTS, reader.nextInt());
    }

    @Override
    public void onOver18(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_OVER_18, reader.nextBoolean());
    }

    @Override
    public void onPermaLink(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_PERMA_LINK, readTrimmedString(reader, ""));
    }

    @Override
    public void onSaved(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SAVED, reader.nextBoolean());
    }

    @Override
    public void onScore(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SCORE, reader.nextInt());
    }

    @Override
    public void onIsSelf(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SELF, reader.nextBoolean());
    }

    @Override
    public void onSubreddit(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SUBREDDIT, readTrimmedString(reader, ""));
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        CharSequence title = formatter.formatNoSpans(context, readTrimmedString(reader, ""));
        values.get(index).put(Things.COLUMN_TITLE, title.toString());
    }

    @Override
    public void onThumbnail(JsonReader reader, int index) throws IOException {
        String thumbnail = readTrimmedString(reader, null);
        if (!TextUtils.isEmpty(thumbnail) && thumbnail.startsWith("http")) {
            values.get(index).put(Things.COLUMN_THUMBNAIL_URL, thumbnail);
        }
    }

    @Override
    public void onUrl(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_URL, readTrimmedString(reader, ""));
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_UPS, reader.nextInt());
    }

    @Override
    public void onAfter(JsonReader reader) throws IOException {
        moreThingId = readTrimmedString(reader, null);
    }

    @Override
    public void onParseEnd() {
        // TODO: Get the cursor for these operations when connecting to the
        // network to do things in parallel.
        if (!TextUtils.isEmpty(profileUser)) {
            switch (filter) {
                case FilterAdapter.PROFILE_SAVED:
                    mergeSaveActions();
                    break;

                case FilterAdapter.PROFILE_LIKED:
                case FilterAdapter.PROFILE_DISLIKED:
                    mergeVoteActions(filter);
                    break;
            }
        }

        if (!TextUtils.isEmpty(moreThingId)) {
            values.add(newContentValues(Kinds.KIND_MORE, moreThingId, 1));
        }
    }

    private ContentValues newContentValues(int kind, String thingId, int extraCapacity) {
        ContentValues v = new ContentValues(3 + extraCapacity);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        v.put(Things.COLUMN_KIND, kind);
        v.put(Things.COLUMN_THING_ID, thingId);
        return v;
    }

    private void mergeSaveActions() {
        // We throw all the saved things on the first page, so don't merge the
        // saves that would add new items if we're just scrolling further down.
        String selection;
        if (TextUtils.isEmpty(more)) {
            selection = SaveActions.SELECT_BY_ACCOUNT;
        } else {
            selection = SaveActions.SELECT_UNSAVED_BY_ACCOUNT;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(SaveActions.TABLE_NAME, SAVE_PROJECTION,
                selection, Array.of(accountName), null, null, SaveActions.SORT_BY_ID);
        while (c.moveToNext()) {
            switch (c.getInt(SAVE_ACTION)) {
                case SaveActions.ACTION_SAVE:
                    addSave(c);
                    break;

                case SaveActions.ACTION_UNSAVE:
                    removeByThingId(c.getString(SAVE_THING_ID));
                    break;
            }
        }
        c.close();
    }

    private void mergeVoteActions(int filter) {
        boolean hasMore = !TextUtils.isEmpty(more);
        String selection;
        switch (filter) {
            case FilterAdapter.PROFILE_LIKED:
                // If we're on the first page, then grab both likes and
                // dislikes. The liked items will be prepended to the top and
                // disliked items will be pruned.
                //
                // If we're just appending, then just grab the disliked items we
                // need to prune.
                selection = !hasMore ? VoteActions.SELECT_NOT_NEUTRAL_BY_ACCOUNT
                        : VoteActions.SELECT_DOWN_BY_ACCOUNT;
                break;

            case FilterAdapter.PROFILE_DISLIKED:
                selection = !hasMore ? VoteActions.SELECT_NOT_NEUTRAL_BY_ACCOUNT
                        : VoteActions.SELECT_UP_BY_ACCOUNT;
                break;

            default:
                throw new IllegalArgumentException();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(VoteActions.TABLE_NAME, VOTE_PROJECTION,
                selection, Array.of(accountName), null, null, SaveActions.SORT_BY_ID);
        while (c.moveToNext()) {
            int action = c.getInt(VOTE_ACTION);
            if (action == VoteActions.ACTION_VOTE_UP
                    && filter == FilterAdapter.PROFILE_LIKED
                    || action == VoteActions.ACTION_VOTE_DOWN
                    && filter == FilterAdapter.PROFILE_DISLIKED) {
                addVote(c);
            } else {
                removeByThingId(c.getString(VOTE_THING_ID));
            }
        }
        c.close();
    }

    private void addSave(Cursor c) {
        ContentValues v = new ContentValues(15);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        v.put(Things.COLUMN_AUTHOR, c.getString(SAVE_AUTHOR));
        v.put(Things.COLUMN_CREATED_UTC, c.getLong(SAVE_CREATED_UTC));
        v.put(Things.COLUMN_DOMAIN, c.getString(SAVE_DOMAIN));
        v.put(Things.COLUMN_DOWNS, c.getString(SAVE_DOWNS));
        v.put(Things.COLUMN_KIND, Kinds.KIND_LINK);
        v.put(Things.COLUMN_LIKES, c.getInt(SAVE_LIKES));
        v.put(Things.COLUMN_NUM_COMMENTS, c.getInt(SAVE_NUM_COMMENTS));
        v.put(Things.COLUMN_OVER_18, c.getInt(SAVE_OVER_18) != 0);
        v.put(Things.COLUMN_PERMA_LINK, c.getString(SAVE_PERMA_LINK));
        v.put(Things.COLUMN_SCORE, c.getInt(SAVE_SCORE));
        v.put(Things.COLUMN_SELF, c.getInt(SAVE_SELF));
        v.put(Things.COLUMN_SUBREDDIT, c.getString(SAVE_SUBREDDIT));
        v.put(Things.COLUMN_TITLE, c.getString(SAVE_TITLE));
        v.put(Things.COLUMN_THING_ID, c.getString(SAVE_THING_ID));
        v.put(Things.COLUMN_THUMBNAIL_URL, c.getString(SAVE_THUMBNAIL_URL));
        v.put(Things.COLUMN_UPS, c.getInt(SAVE_UPS));
        v.put(Things.COLUMN_URL, c.getString(SAVE_URL));
        values.add(0, v);
    }

    private void addVote(Cursor c) {
        ContentValues v = new ContentValues(15);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        v.put(Things.COLUMN_AUTHOR, c.getString(VOTE_AUTHOR));
        v.put(Things.COLUMN_CREATED_UTC, c.getLong(VOTE_CREATED_UTC));
        v.put(Things.COLUMN_DOMAIN, c.getString(VOTE_DOMAIN));
        v.put(Things.COLUMN_DOWNS, c.getString(VOTE_DOWNS));
        v.put(Things.COLUMN_KIND, Kinds.KIND_LINK);
        v.put(Things.COLUMN_LIKES, c.getInt(VOTE_LIKES));
        v.put(Things.COLUMN_NUM_COMMENTS, c.getInt(VOTE_NUM_COMMENTS));
        v.put(Things.COLUMN_OVER_18, c.getInt(VOTE_OVER_18) != 0);
        v.put(Things.COLUMN_PERMA_LINK, c.getString(VOTE_PERMA_LINK));
        v.put(Things.COLUMN_SCORE, c.getInt(VOTE_SCORE));
        v.put(Things.COLUMN_SELF, c.getInt(VOTE_SELF) != 0);
        v.put(Things.COLUMN_SUBREDDIT, c.getString(VOTE_SUBREDDIT));
        v.put(Things.COLUMN_TITLE, c.getString(VOTE_TITLE));
        v.put(Things.COLUMN_THING_ID, c.getString(VOTE_THING_ID));
        v.put(Things.COLUMN_THUMBNAIL_URL, c.getString(VOTE_THUMBNAIL_URL));
        v.put(Things.COLUMN_UPS, c.getInt(VOTE_UPS));
        v.put(Things.COLUMN_URL, c.getString(VOTE_URL));
        values.add(0, v);
    }

    private void removeByThingId(String targetThingId) {
        int size = values.size();
        for (int i = 0; i < size; i++) {
            String thingId = values.get(i).getAsString(Things.COLUMN_THING_ID);
            if (thingId.equals(targetThingId)) {
                values.remove(i);
                break;
            }
        }
    }
}
