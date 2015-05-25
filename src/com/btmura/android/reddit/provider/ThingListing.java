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
import java.util.List;
import java.util.Map;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;

import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.net.ApiUrls;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi2;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;

class ThingListing extends JsonParser implements Listing {

    public static final String TAG = "ThingListing";

    private static final String[] HIDE_PROJECTION = {
            HideActions._ID,
            HideActions.COLUMN_ACTION,
            HideActions.COLUMN_AUTHOR,
            HideActions.COLUMN_CREATED_UTC,
            HideActions.COLUMN_DOMAIN,
            HideActions.COLUMN_DOWNS,
            HideActions.COLUMN_LIKES,
            HideActions.COLUMN_NUM_COMMENTS,
            HideActions.COLUMN_OVER_18,
            HideActions.COLUMN_PERMA_LINK,
            HideActions.COLUMN_SCORE,
            HideActions.COLUMN_SELF,
            HideActions.COLUMN_SUBREDDIT,
            HideActions.COLUMN_THING_ID,
            HideActions.COLUMN_TITLE,
            HideActions.COLUMN_THUMBNAIL_URL,
            HideActions.COLUMN_UPS,
            HideActions.COLUMN_URL,
    };

    private static final int HIDE_ACTION = 1;
    private static final int HIDE_AUTHOR = 2;
    private static final int HIDE_CREATED_UTC = 3;
    private static final int HIDE_DOMAIN = 4;
    private static final int HIDE_DOWNS = 5;
    private static final int HIDE_LIKES = 6;
    private static final int HIDE_NUM_COMMENTS = 7;
    private static final int HIDE_OVER_18 = 8;
    private static final int HIDE_PERMA_LINK = 9;
    private static final int HIDE_SCORE = 10;
    private static final int HIDE_SELF = 11;
    private static final int HIDE_SUBREDDIT = 12;
    private static final int HIDE_THING_ID = 13;
    private static final int HIDE_TITLE = 14;
    private static final int HIDE_THUMBNAIL_URL = 15;
    private static final int HIDE_UPS = 16;
    private static final int HIDE_URL = 17;

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

    private final Context context;
    private final SQLiteOpenHelper dbHelper;
    private final int sessionType;
    private final String accountName;
    private final String subreddit;
    private final String query;
    private final String profileUser;
    private final int filter;
    private final String more;
    private final MarkdownFormatter formatter = new MarkdownFormatter();

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(30);
    private Map<String, Integer> hideActionMap;
    private Map<String, Integer> saveActionMap;
    private Map<String, Integer> voteActionMap;
    private String moreThingId;

    static ThingListing newSearchInstance(Context context,
            SQLiteOpenHelper dbHelper,
            String accountName,
            String subreddit,
            String query,
            int filter,
            String more) {
        return new ThingListing(context,
                dbHelper,
                Sessions.TYPE_THING_SEARCH,
                accountName,
                subreddit,
                query,
                null,
                filter,
                more);
    }

    static ThingListing newSubredditInstance(Context context,
            SQLiteOpenHelper dbHelper,
            String accountName,
            String subreddit,
            int filter,
            String more) {
        return new ThingListing(context,
                dbHelper,
                Sessions.TYPE_SUBREDDIT,
                accountName,
                subreddit,
                null,
                null,
                filter,
                more);
    }

    static ThingListing newUserInstance(Context context,
            SQLiteOpenHelper dbHelper,
            String accountName,
            String profileUser,
            int filter,
            String more) {
        return new ThingListing(context,
                dbHelper,
                Sessions.TYPE_USER,
                accountName,
                null,
                null,
                profileUser,
                filter,
                more);
    }

    private ThingListing(Context context,
            SQLiteOpenHelper dbHelper,
            int sessionType,
            String accountName,
            String subreddit,
            String query,
            String profileUser,
            int filter,
            String more) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.sessionType = sessionType;
        this.accountName = accountName;
        this.subreddit = subreddit;
        this.query = query;
        this.profileUser = profileUser;
        this.filter = filter;
        this.more = more;
    }

    @Override
    public int getSessionType() {
        return sessionType;
    }

    @Override
    public String getSessionThingId() {
        return null;
    }

    @Override
    public List<ContentValues> getValues() throws IOException, AuthenticatorException, OperationCanceledException {
        HttpURLConnection conn = RedditApi2.connect(context, accountName, getUrl());
        InputStream input = null;
        try {
            input = new BufferedInputStream(conn.getInputStream());
            hideActionMap = HideMerger.getActionMap(dbHelper, accountName);
            saveActionMap = SaveMerger.getActionMap(dbHelper, accountName);
            voteActionMap = VoteMerger.getActionMap(dbHelper, accountName);

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
        if (!TextUtils.isEmpty(profileUser)) {
            return Urls.user(profileUser, filter, more, Urls.TYPE_JSON);
        }
        if (!TextUtils.isEmpty(query)) {
            return Urls.search(subreddit, query, filter, more);
        }
        return ApiUrls.subreddit(subreddit, filter, more);
    }

    @Override
    public void performExtraWork(Context context) {
    }

    @Override
    public String getTargetTable() {
        return Things.TABLE_NAME;
    }

    @Override
    public boolean isAppend() {
        return !TextUtils.isEmpty(more);
    }

    @Override
    public void onEntityStart(int index) {
        // Pass -1 and null since we don't know those until later
        values.add(newContentValues(-1, null, 19));
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_AUTHOR, readString(reader, ""));
    }

    @Override
    public void onBody(JsonReader reader, int index) throws IOException {
        CharSequence body = readFormattedString(reader);
        values.get(index).put(Things.COLUMN_BODY, body.toString());
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_CREATED_UTC, reader.nextLong());
    }

    @Override
    public void onDomain(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_DOMAIN, readString(reader, ""));
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_DOWNS, reader.nextInt());
    }

    @Override
    public void onHidden(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_HIDDEN, reader.nextBoolean());
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
        CharSequence title = readFormattedString(reader);
        values.get(index).put(Things.COLUMN_LINK_TITLE, title.toString());
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        String name = readString(reader, "");
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
        String url = readFormattedString(reader).toString();
        values.get(index).put(Things.COLUMN_PERMA_LINK, url);
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
        values.get(index).put(Things.COLUMN_SUBREDDIT, readString(reader, ""));
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        CharSequence title = readFormattedString(reader);
        values.get(index).put(Things.COLUMN_TITLE, title.toString());
    }

    @Override
    public void onThumbnail(JsonReader reader, int index) throws IOException {
        String thumbnail = readString(reader, null);
        if (!TextUtils.isEmpty(thumbnail) && thumbnail.startsWith("http")) {
            values.get(index).put(Things.COLUMN_THUMBNAIL_URL, thumbnail);
        }
    }

    @Override
    public void onUrl(JsonReader reader, int index) throws IOException {
        String url = readFormattedString(reader).toString();
        values.get(index).put(Things.COLUMN_URL, url);
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_UPS, reader.nextInt());
    }

    private CharSequence readFormattedString(JsonReader reader) throws IOException {
        return formatter.formatNoSpans(context, readString(reader, ""));
    }

    @Override
    public void onAfter(JsonReader reader) throws IOException {
        moreThingId = readString(reader, null);
    }

    @Override
    public void onParseEnd() {
        // TODO: Get the cursor for these operations when connecting to the
        // network to do things in parallel.
        if (!TextUtils.isEmpty(profileUser)) {
            switch (filter) {
                case Filter.PROFILE_HIDDEN:
                    mergeHideAction();
                    break;

                case Filter.PROFILE_SAVED:
                    mergeSaveActions();
                    break;

                case Filter.PROFILE_LIKED:
                case Filter.PROFILE_DISLIKED:
                    mergeVoteActions(filter);
                    break;
            }
        }

        doFinalMerge();
    }

    private ContentValues newContentValues(int kind, String thingId, int extraCapacity) {
        ContentValues v = new ContentValues(3 + extraCapacity);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        v.put(Things.COLUMN_KIND, kind);
        v.put(Things.COLUMN_THING_ID, thingId);
        return v;
    }

    private void mergeHideAction() {
        // Select pending hidden and unhidden things to add and remove on the first page.
        // On subsequent pages only get the unhidden things to remove items from pages.
        String selection = TextUtils.isEmpty(more)
                ? HideActions.SELECT_BY_ACCOUNT
                : HideActions.SELECT_UNHIDDEN_BY_ACCOUNT;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(HideActions.TABLE_NAME, HIDE_PROJECTION,
                selection, Array.of(accountName), null, null, HideActions.SORT_BY_ID);
        while (c.moveToNext()) {
            switch (c.getInt(HIDE_ACTION)) {
                case HideActions.ACTION_HIDE:
                    addHide(c);
                    break;

                case HideActions.ACTION_UNHIDE:
                    removeByThingId(c.getString(HIDE_THING_ID));
                    break;
            }
        }
        c.close();
    }

    private void mergeSaveActions() {
        // We throw all the saved things on the first page, so don't merge the
        // saves that would add new items if we're just scrolling further down.
        String selection = TextUtils.isEmpty(more)
                ? SaveActions.SELECT_BY_ACCOUNT
                : SaveActions.SELECT_UNSAVED_BY_ACCOUNT;

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
        boolean firstPage = TextUtils.isEmpty(more);
        String selection;
        switch (filter) {
            case Filter.PROFILE_LIKED:
                // If we're on the first page, then grab both likes, dislikes, and neutral votes.
                // The liked items will be prepended to the top and disliked and netural items will
                // be pruned.
                //
                // If we're just appending, then just grab the disliked and neutral items we need to
                // prune.
                selection = firstPage
                        ? VoteActions.SELECT_SHOWABLE_BY_ACCOUNT
                        : VoteActions.SELECT_SHOWABLE_NOT_UP_BY_ACCOUNT;
                break;

            case Filter.PROFILE_DISLIKED:
                selection = firstPage
                        ? VoteActions.SELECT_SHOWABLE_BY_ACCOUNT
                        : VoteActions.SELECT_SHOWABLE_NOT_DOWN_BY_ACCOUNT;
                break;

            default:
                throw new IllegalArgumentException();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(VoteActions.TABLE_NAME, VOTE_PROJECTION,
                selection, Array.of(accountName), null, null, VoteActions.SORT_BY_ID);
        while (c.moveToNext()) {
            int action = c.getInt(VOTE_ACTION);
            if (action == VoteActions.ACTION_VOTE_UP
                    && filter == Filter.PROFILE_LIKED
                    || action == VoteActions.ACTION_VOTE_DOWN
                    && filter == Filter.PROFILE_DISLIKED) {
                addVote(c);
            } else {
                removeByThingId(c.getString(VOTE_THING_ID));
            }
        }
        c.close();
    }

    private void addHide(Cursor c) {
        ContentValues v = new ContentValues(20);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        v.put(Things.COLUMN_AUTHOR, c.getString(HIDE_AUTHOR));
        v.put(Things.COLUMN_CREATED_UTC, c.getLong(HIDE_CREATED_UTC));
        v.put(Things.COLUMN_DOMAIN, c.getString(HIDE_DOMAIN));
        v.put(Things.COLUMN_DOWNS, c.getString(HIDE_DOWNS));
        v.put(Things.COLUMN_HIDDEN, true);
        v.put(Things.COLUMN_KIND, Kinds.KIND_LINK);
        v.put(Things.COLUMN_LIKES, c.getInt(HIDE_LIKES));
        v.put(Things.COLUMN_NUM_COMMENTS, c.getInt(HIDE_NUM_COMMENTS));
        v.put(Things.COLUMN_OVER_18, c.getInt(HIDE_OVER_18) != 0);
        v.put(Things.COLUMN_PERMA_LINK, c.getString(HIDE_PERMA_LINK));
        v.put(Things.COLUMN_SCORE, c.getInt(HIDE_SCORE));
        v.put(Things.COLUMN_SELF, c.getInt(HIDE_SELF));
        v.put(Things.COLUMN_SUBREDDIT, c.getString(HIDE_SUBREDDIT));
        v.put(Things.COLUMN_TITLE, c.getString(HIDE_TITLE));
        v.put(Things.COLUMN_THING_ID, c.getString(HIDE_THING_ID));
        v.put(Things.COLUMN_THUMBNAIL_URL, c.getString(HIDE_THUMBNAIL_URL));
        v.put(Things.COLUMN_UPS, c.getInt(HIDE_UPS));
        v.put(Things.COLUMN_URL, c.getString(HIDE_URL));
        values.add(0, v);
    }

    private void addSave(Cursor c) {
        ContentValues v = new ContentValues(20);
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
        ContentValues v = new ContentValues(20);
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

    private void doFinalMerge() {
        int count = values.size();
        for (int i = 0; i < count; i++) {
            ContentValues v = values.get(i);
            HideMerger.updateContentValues(v, hideActionMap);
            SaveMerger.updateContentValues(v, saveActionMap);
            VoteMerger.updateContentValues(v, voteActionMap);
        }
        appendLoadingMore();
    }

    private void appendLoadingMore() {
        if (!TextUtils.isEmpty(moreThingId)) {
            values.add(newContentValues(Kinds.KIND_MORE, moreThingId, 1));
        }
    }
}
