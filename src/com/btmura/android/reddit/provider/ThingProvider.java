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

import java.io.IOException;
import java.util.List;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.CommentLogic;
import com.btmura.android.reddit.app.CommentLogic.CursorCommentList;
import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;

/**
 * URI MATCHING PATTERNS:
 *
 * <pre>
 * /things
 * /comments
 * /messages
 * /actions/hides
 * /subreddits
 * /sessions
 * /actions/comments
 * /actions/messages
 * /actions/reads
 * /actions/saves
 * /actions/votes
 * </pre>
 */
public class ThingProvider extends BaseProvider {

    public static final String TAG = "ThingProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";
    static final String AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final String PATH_THINGS = "things";
    private static final String PATH_COMMENTS = "comments";
    private static final String PATH_MESSAGES = "messages";
    private static final String PATH_SUBREDDITS = "subreddits";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_COMMENT_ACTIONS = "actions/comments";
    private static final String PATH_HIDE_ACTIONS = "actions/hides";
    private static final String PATH_MESSAGE_ACTIONS = "actions/messages";
    private static final String PATH_READ_ACTIONS = "actions/reads";
    private static final String PATH_SAVE_ACTIONS = "actions/saves";
    private static final String PATH_VOTE_ACTIONS = "actions/votes";

    public static final Uri THINGS_URI = Uri.parse(AUTHORITY_URI + PATH_THINGS);
    public static final Uri COMMENTS_URI = Uri.parse(AUTHORITY_URI + PATH_COMMENTS);
    public static final Uri MESSAGES_URI = Uri.parse(AUTHORITY_URI + PATH_MESSAGES);
    public static final Uri SUBREDDITS_URI = Uri.parse(AUTHORITY_URI + PATH_SUBREDDITS);
    public static final Uri SESSIONS_URI = Uri.parse(AUTHORITY_URI + PATH_SESSIONS);
    public static final Uri COMMENT_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_COMMENT_ACTIONS);
    public static final Uri HIDE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_HIDE_ACTIONS);
    public static final Uri MESSAGE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_MESSAGE_ACTIONS);
    public static final Uri READ_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_READ_ACTIONS);
    public static final Uri SAVE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_SAVE_ACTIONS);
    public static final Uri VOTE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_VOTE_ACTIONS);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_THINGS = 1;
    private static final int MATCH_COMMENTS = 2;
    private static final int MATCH_MESSAGES = 3;
    private static final int MATCH_SUBREDDITS = 4;
    private static final int MATCH_SESSIONS = 5;
    private static final int MATCH_COMMENT_ACTIONS = 6;
    private static final int MATCH_HIDE_ACTIONS = 7;
    private static final int MATCH_MESSAGE_ACTIONS = 8;
    private static final int MATCH_READ_ACTIONS = 9;
    private static final int MATCH_SAVE_ACTIONS = 10;
    private static final int MATCH_VOTE_ACTIONS = 11;
    static {
        MATCHER.addURI(AUTHORITY, PATH_THINGS, MATCH_THINGS);
        MATCHER.addURI(AUTHORITY, PATH_COMMENTS, MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGES, MATCH_MESSAGES);
        MATCHER.addURI(AUTHORITY, PATH_SUBREDDITS, MATCH_SUBREDDITS);
        MATCHER.addURI(AUTHORITY, PATH_SESSIONS, MATCH_SESSIONS);
        MATCHER.addURI(AUTHORITY, PATH_COMMENT_ACTIONS, MATCH_COMMENT_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_HIDE_ACTIONS, MATCH_HIDE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGE_ACTIONS, MATCH_MESSAGE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_READ_ACTIONS, MATCH_READ_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_SAVE_ACTIONS, MATCH_SAVE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_VOTE_ACTIONS, MATCH_VOTE_ACTIONS);
    }

    private static final String METHOD_GET_SESSION = "getSession";
    private static final String METHOD_CLEAN_SESSIONS = "cleanSessions";
    private static final String METHOD_EXPAND_COMMENT = "expandComment";
    private static final String METHOD_COLLAPSE_COMMENT = "collapseComment";
    private static final String METHOD_INSERT_COMMENT = "insertComment";
    private static final String METHOD_EDIT_COMMENT = "editComment";
    private static final String METHOD_DELETE_COMMENT = "deleteComment";
    private static final String METHOD_INSERT_MESSAGE = "insertMessage";
    private static final String METHOD_READ_MESSAGE = "readMessage";
    private static final String METHOD_HIDE = "hide";
    private static final String METHOD_SAVE = "save";
    private static final String METHOD_VOTE = "vote";

    // List of extras used throughout the provider code.

    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_BODY = "body";
    private static final String EXTRA_COUNT = "count";
    private static final String EXTRA_FILTER = "filter";
    private static final String EXTRA_HAS_CHILDREN = "hasChildren";
    private static final String EXTRA_ID = "id";
    private static final String EXTRA_ID_ARRAY = "idArray";
    private static final String EXTRA_LINK_ID = "linkId";
    private static final String EXTRA_MARK = "mark";
    private static final String EXTRA_MORE = "more";
    private static final String EXTRA_PARENT_THING_ID = "parentThingId";
    private static final String EXTRA_QUERY = "query";
    private static final String EXTRA_SESSION_DATA = "sessionData";
    private static final String EXTRA_SESSION_ID = "sessionId";
    private static final String EXTRA_SESSION_TYPE = "sessionType";
    private static final String EXTRA_SUBREDDIT = "subreddit";
    private static final String EXTRA_THING_BUNDLE = "thingBundle";
    private static final String EXTRA_THING_ID = "thingId";
    private static final String EXTRA_THING_ID_ARRAY = "thingIdArray";
    private static final String EXTRA_USER = "user";

    private static final String[] CLEAN_PROJECTION = {
            Sessions._ID,
            Sessions.COLUMN_TYPE,
    };
    private static final int CLEAN_INDEX_ID = 0;
    private static final int CLEAN_INDEX_TYPE = 1;

    private static final String CLEAN_SORT = Sessions._ID + " DESC";
    private static final String CLEAN_OFFSET_LIMIT = "10, 1000"; // offset, limit

    private static final String[] EXPAND_PROJECTION = {
            Comments._ID,
            Comments.COLUMN_EXPANDED,
            Comments.COLUMN_NESTING,
    };

    private static final String[] SESSION_ID_PROJECTION = {
            Sessions._ID,
    };
    private static final int SESSION_INDEX_ID = 0;

    private static final String[] INSERT_COMMENT_PROJECTION = {
            Comments._ID,
            Comments.COLUMN_NESTING,
            Comments.COLUMN_SEQUENCE,
            Comments.COLUMN_THING_ID,
    };
    private static final int INSERT_COMMENT_INDEX_ID = 0;
    private static final int INSERT_COMMENT_INDEX_NESTING = 1;
    private static final int INSERT_COMMENT_INDEX_SEQUENCE = 2;
    private static final int INSERT_COMMENT_INDEX_THING = 3;

    private static final String UPDATE_SEQUENCE_STATEMENT = "UPDATE " + Comments.TABLE_NAME
            + " SET " + Comments.COLUMN_SEQUENCE + "=" + Comments.COLUMN_SEQUENCE + "+1"
            + " WHERE " + Comments.COLUMN_SESSION_ID + "=? AND " + Comments.COLUMN_SEQUENCE + ">=?";

    private static final String SELECT_MORE_WITH_SESSION_ID = Kinds.COLUMN_KIND + "="
            + Kinds.KIND_MORE + " AND " + SharedColumns.COLUMN_SESSION_ID + "=?";

    private final SessionManager sessionManager = new SessionManager();

    public ThingProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        switch (MATCHER.match(uri)) {
            case MATCH_THINGS:
                return Things.TABLE_NAME;

            case MATCH_COMMENTS:
                return Comments.TABLE_NAME;

            case MATCH_MESSAGES:
                return Messages.TABLE_NAME;

            case MATCH_SUBREDDITS:
                return SubredditResults.TABLE_NAME;

            case MATCH_SESSIONS:
                return Sessions.TABLE_NAME;

            case MATCH_COMMENT_ACTIONS:
                return CommentActions.TABLE_NAME;

            case MATCH_HIDE_ACTIONS:
                return HideActions.TABLE_NAME;

            case MATCH_MESSAGE_ACTIONS:
                return MessageActions.TABLE_NAME;

            case MATCH_READ_ACTIONS:
                return ReadActions.TABLE_NAME;

            case MATCH_SAVE_ACTIONS:
                return SaveActions.TABLE_NAME;

            case MATCH_VOTE_ACTIONS:
                return VoteActions.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    public static Bundle getSubredditSession(Context context,
            String accountName,
            String subreddit,
            int filter,
            Bundle sessionData,
            String more) {
        Bundle extras = new Bundle(5);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_SUBREDDIT);
        extras.putString(EXTRA_SUBREDDIT, subreddit);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        extras.putString(EXTRA_MORE, more);
        return Provider.call(context, SUBREDDITS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static Bundle getProfileSession(Context context,
            String accountName,
            String profileUser,
            int filter,
            Bundle sessionData,
            String more) {
        Bundle extras = new Bundle(4);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_USER);
        extras.putString(EXTRA_USER, profileUser);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        extras.putString(EXTRA_MORE, more);
        return Provider.call(context, THINGS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static Bundle getCommentsSession(Context context,
            String accountName,
            String thingId,
            String linkId,
            int filter,
            Bundle sessionData,
            int numComments) {
        Bundle extras = new Bundle(6);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_COMMENTS);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putString(EXTRA_LINK_ID, linkId);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putInt(EXTRA_COUNT, numComments);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        return Provider.call(context, COMMENTS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static Bundle getThingSearchSession(Context context,
            String accountName,
            String subreddit,
            String query,
            int filter,
            Bundle sessionData,
            String more) {
        Bundle extras = new Bundle(5);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_THING_SEARCH);
        extras.putString(EXTRA_SUBREDDIT, subreddit);
        extras.putString(EXTRA_QUERY, query);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putString(EXTRA_MORE, more);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        return Provider.call(context, THINGS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static Bundle getSubredditSearchSession(Context context,
            String accountName,
            String query,
            Bundle sessionData) {
        Bundle extras = new Bundle(3);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_SUBREDDIT_SEARCH);
        extras.putString(EXTRA_QUERY, query);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        return Provider.call(context, SUBREDDITS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static Bundle getMessageSession(Context context,
            String accountName,
            int filter,
            Bundle sessionData,
            String more) {
        Bundle extras = new Bundle(4);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_MESSAGES);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        if (!TextUtils.isEmpty(more)) {
            extras.putString(EXTRA_MORE, more);
        } else if (filter == Filter.MESSAGE_INBOX || filter == Filter.MESSAGE_UNREAD) {
            extras.putBoolean(EXTRA_MARK, true);
        }
        return Provider.call(context, MESSAGES_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static Bundle getMessageThreadSession(Context context,
            String accountName,
            String thingId,
            Bundle sessionData) {
        Bundle extras = new Bundle(3);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_MESSAGE_THREAD);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putBundle(EXTRA_SESSION_DATA, sessionData);
        return Provider.call(context, MESSAGES_URI, METHOD_GET_SESSION, accountName, extras);
    }

    static Bundle cleanSessions(Context context, int sessionType) {
        Bundle extras = new Bundle(1);
        extras.putInt(EXTRA_SESSION_TYPE, sessionType);
        return Provider.call(context, THINGS_URI, METHOD_CLEAN_SESSIONS, null, extras);
    }

    static Bundle expandComment(Context context, long id, long sessionId) {
        Bundle extras = new Bundle(2);
        extras.putLong(EXTRA_ID, id);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        return Provider.call(context, COMMENTS_URI, METHOD_EXPAND_COMMENT, null, extras);
    }

    static Bundle collapseComment(Context context, long id, long[] childIds) {
        Bundle extras = new Bundle(2);
        extras.putLong(EXTRA_ID, id);
        extras.putLongArray(EXTRA_ID_ARRAY, childIds);
        return Provider.call(context, COMMENTS_URI, METHOD_COLLAPSE_COMMENT, null, extras);
    }

    static Bundle insertComment(Context context,
            String accountName,
            String body,
            String parentThingId,
            String thingId) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_BODY, body);
        extras.putString(EXTRA_PARENT_THING_ID, parentThingId);
        extras.putString(EXTRA_THING_ID, thingId);
        return Provider.call(context,
                COMMENT_ACTIONS_URI,
                METHOD_INSERT_COMMENT,
                accountName,
                extras);
    }

    static Bundle editComment(Context context,
            String accountName,
            String body,
            String parentThingId,
            String thingId) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_BODY, body);
        extras.putString(EXTRA_PARENT_THING_ID, parentThingId);
        extras.putString(EXTRA_THING_ID, thingId);
        return Provider.call(context,
                COMMENT_ACTIONS_URI,
                METHOD_EDIT_COMMENT,
                accountName,
                extras);
    }

    static Bundle deleteComment(Context context,
            String accountName,
            boolean[] hasChildren,
            long[] ids,
            String parentThingId,
            String[] thingIds) {
        Bundle extras = new Bundle(4);
        extras.putBooleanArray(EXTRA_HAS_CHILDREN, hasChildren);
        extras.putLongArray(EXTRA_ID_ARRAY, ids);
        extras.putString(EXTRA_PARENT_THING_ID, parentThingId);
        extras.putStringArray(EXTRA_THING_ID_ARRAY, thingIds);
        return Provider.call(context,
                COMMENT_ACTIONS_URI,
                METHOD_DELETE_COMMENT,
                accountName,
                extras);
    }

    static Bundle insertMessage(Context context,
            String accountName,
            String body,
            String parentThingId,
            String thingId) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_BODY, body);
        extras.putString(EXTRA_PARENT_THING_ID, parentThingId);
        extras.putString(EXTRA_THING_ID, thingId);
        return Provider.call(context,
                MESSAGE_ACTIONS_URI,
                METHOD_INSERT_MESSAGE,
                accountName,
                extras);
    }

    static Bundle readMessage(Context context,
            String accountName,
            int action,
            String thingId) {
        Bundle extras = new Bundle(2);
        extras.putInt(EXTRA_ACTION, action);
        extras.putString(EXTRA_THING_ID, thingId);
        return Provider.call(context, READ_ACTIONS_URI, METHOD_READ_MESSAGE, accountName, extras);
    }

    static Bundle hide(Context context,
            String accountName,
            int action,
            String thingId,
            ThingBundle thingBundle) {
        Bundle extras = new Bundle(3);
        extras.putInt(EXTRA_ACTION, action);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putParcelable(EXTRA_THING_BUNDLE, thingBundle);
        return Provider.call(context, HIDE_ACTIONS_URI, METHOD_HIDE, accountName, extras);
    }

    static Bundle save(Context context,
            String accountName,
            int action,
            String thingId,
            ThingBundle thingBundle) {
        Bundle extras = new Bundle(3);
        extras.putInt(EXTRA_ACTION, action);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putParcelable(EXTRA_THING_BUNDLE, thingBundle);
        return Provider.call(context, SAVE_ACTIONS_URI, METHOD_SAVE, accountName, extras);
    }

    static Bundle vote(Context context,
            String accountName,
            int action,
            String thingId,
            ThingBundle thingBundle) {
        Bundle extras = new Bundle(3);
        extras.putInt(EXTRA_ACTION, action);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putParcelable(EXTRA_THING_BUNDLE, thingBundle);
        return Provider.call(context, VOTE_ACTIONS_URI, METHOD_VOTE, accountName, extras);
    }

    @Override
    public Bundle call(String method, String accountName, Bundle extras) {
        try {
            if (METHOD_GET_SESSION.equals(method)) {
                return getSession(accountName, extras);
            } else if (METHOD_CLEAN_SESSIONS.equals(method)) {
                return cleanSessions(extras);
            } else if (METHOD_EXPAND_COMMENT.equals(method)) {
                return expandComment(extras);
            } else if (METHOD_COLLAPSE_COMMENT.equals(method)) {
                return collapseComment(extras);
            } else if (METHOD_INSERT_COMMENT.equals(method)) {
                return insertComment(accountName, extras);
            } else if (METHOD_EDIT_COMMENT.equals(method)) {
                return editComment(accountName, extras);
            } else if (METHOD_DELETE_COMMENT.equals(method)) {
                return deleteComment(accountName, extras);
            } else if (METHOD_INSERT_MESSAGE.equals(method)) {
                return insertMessage(accountName, extras);
            } else if (METHOD_READ_MESSAGE.equals(method)) {
                return readMessage(accountName, extras);
            } else if (METHOD_HIDE.equals(method)) {
                return hide(accountName, extras);
            } else if (METHOD_SAVE.equals(method)) {
                return save(accountName, extras);
            } else if (METHOD_VOTE.equals(method)) {
                return vote(accountName, extras);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    private Bundle getSession(String accountName, Bundle extras) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getSession accountName: " + accountName + " extras: " + extras);
        }
        Bundle sessionData = extras.getBundle(EXTRA_SESSION_DATA);
        String more = extras.getString(EXTRA_MORE);
        if (isExistingSession(sessionData, more)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getSession --> EXISTING " + sessionData);
            }
            return sessionData;
        }

        try {
            Listing listing = createListing(accountName, extras);
            Bundle newSessionData = getListingSession(accountName, listing, sessionData);
            listing.performExtraWork(getContext());
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getSession --> NEW " + newSessionData);
            }
            return newSessionData;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private boolean isExistingSession(Bundle sessionData, String more) {
        long sessionId = sessionData != null ? sessionData.getLong(EXTRA_SESSION_ID) : 0;
        if (sessionId != 0 && more == null) {
            SQLiteDatabase db = helper.getReadableDatabase();
            long count = DatabaseUtils.queryNumEntries(db,
                    Sessions.TABLE_NAME,
                    ID_SELECTION,
                    Array.of(sessionId));
            return count > 0;
        }
        return false;
    }

    private Listing createListing(String accountName, Bundle extras)
            throws OperationCanceledException, AuthenticatorException, IOException {
        Context context = getContext();
        String cookie = AccountUtils.getCookie(context, accountName);
        if (cookie == null && AccountUtils.isAccount(accountName)) {
            return null;
        }

        int count = extras.getInt(EXTRA_COUNT);
        int filter = extras.getInt(EXTRA_FILTER, -1);
        String linkId = extras.getString(EXTRA_LINK_ID);
        boolean mark = extras.getBoolean(EXTRA_MARK, false);
        String more = extras.getString(EXTRA_MORE);
        String query = extras.getString(EXTRA_QUERY);
        String subreddit = extras.getString(EXTRA_SUBREDDIT);
        String thingId = extras.getString(EXTRA_THING_ID);
        String user = extras.getString(EXTRA_USER);

        int listingType = extras.getInt(EXTRA_SESSION_TYPE);
        switch (listingType) {
            case Sessions.TYPE_MESSAGE_THREAD:
                return MessageListing.newThreadInstance(helper, accountName, thingId, cookie);

            case Sessions.TYPE_MESSAGES:
                return MessageListing.newInstance(helper,
                        accountName,
                        filter,
                        more,
                        mark,
                        cookie);

            case Sessions.TYPE_SUBREDDIT:
                return ThingListing.newSubredditInstance(context,
                        helper,
                        accountName,
                        subreddit,
                        filter,
                        more,
                        cookie);

            case Sessions.TYPE_USER:
                return ThingListing.newUserInstance(context,
                        helper,
                        accountName,
                        user,
                        filter,
                        more,
                        cookie);

            case Sessions.TYPE_COMMENTS:
                return CommentListing.newInstance(context,
                        helper,
                        accountName,
                        thingId,
                        linkId,
                        filter,
                        count,
                        cookie);

            case Sessions.TYPE_THING_SEARCH:
                return ThingListing.newSearchInstance(context,
                        helper,
                        accountName,
                        subreddit,
                        query,
                        filter,
                        more,
                        cookie);

            case Sessions.TYPE_SUBREDDIT_SEARCH:
                return SubredditResultListing.newInstance(accountName, query, cookie);

            default:
                throw new IllegalArgumentException();
        }
    }

    private Bundle getListingSession(String accountName, Listing listing, Bundle sessionData)
            throws IOException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getListingSession accountName: " + accountName
                    + " sessionData: " + sessionData);
        }
        long sessionId = sessionData != null ? sessionData.getLong(EXTRA_SESSION_ID) : 0;

        // Get new values over the network.
        List<ContentValues> values = listing.getValues();

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete any existing "Loading..." signs if appending.
            if (listing.isAppend()) {
                // Appending requires an existing session to append the data.
                if (sessionId == 0) {
                    throw new IllegalStateException();
                }

                // Delete the row for this append. If there is no such row, then
                // this might be a duplicate append that got triggered, so just
                // return the existing session id and hope for the best.
                int count = db.delete(listing.getTargetTable(),
                        SELECT_MORE_WITH_SESSION_ID, Array.of(sessionId));
                if (count == 0) {
                    return sessionData;
                }
            }

            // Create a new session if there is no id.
            if (sessionId == 0) {
                ContentValues v = new ContentValues(4);
                v.put(Sessions.COLUMN_ACCOUNT, accountName);
                v.put(Sessions.COLUMN_TYPE, listing.getSessionType());
                v.put(Sessions.COLUMN_THING_ID, listing.getSessionThingId());
                v.put(Sessions.COLUMN_TIMESTAMP, System.currentTimeMillis());
                sessionId = db.insert(Sessions.TABLE_NAME, null, v);
            }

            // Add the session id to the data rows.
            int count = values.size();
            for (int i = 0; i < count; i++) {
                values.get(i).put(SharedColumns.COLUMN_SESSION_ID, sessionId);
            }

            // Insert the rows into the database.
            InsertHelper helper = new InsertHelper(db, listing.getTargetTable());
            for (int i = 0; i < count; i++) {
                helper.insert(values.get(i));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // Start cleaning service on separate thread after the latest session was made.
        sessionManager.cleanIfNecessary(getContext(), listing.getSessionType());

        Bundle newSessionData = new Bundle(1);
        newSessionData.putLong(EXTRA_SESSION_ID, sessionId);
        return newSessionData;
    }

    private Bundle cleanSessions(Bundle extras) {
        int sessionType = extras.getInt(EXTRA_SESSION_TYPE);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "cleanSessions sessionType: " + sessionType);
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.query(Sessions.TABLE_NAME,
                    CLEAN_PROJECTION,
                    Sessions.SELECT_BY_TYPE,
                    Array.of(sessionType),
                    null,
                    null,
                    CLEAN_SORT,
                    CLEAN_OFFSET_LIMIT);
            while (cursor.moveToNext()) {
                long sessionId = cursor.getLong(CLEAN_INDEX_ID);
                int type = cursor.getInt(CLEAN_INDEX_TYPE);

                // TODO(btmura): Use listing objects to get target table name.
                String tableName;
                switch (type) {
                    case Sessions.TYPE_SUBREDDIT:
                    case Sessions.TYPE_THING_SEARCH:
                    case Sessions.TYPE_USER:
                        tableName = Things.TABLE_NAME;
                        break;

                    case Sessions.TYPE_COMMENTS:
                        tableName = Comments.TABLE_NAME;
                        break;

                    case Sessions.TYPE_SUBREDDIT_SEARCH:
                        tableName = SubredditResults.TABLE_NAME;
                        break;

                    case Sessions.TYPE_MESSAGES:
                    case Sessions.TYPE_MESSAGE_THREAD:
                        tableName = Messages.TABLE_NAME;
                        break;

                    default:
                        throw new IllegalArgumentException();
                }

                String[] selectionArgs = Array.of(sessionId);
                int count2 = db.delete(Sessions.TABLE_NAME, Sessions.SELECT_BY_ID, selectionArgs);
                int count = db.delete(tableName, SharedColumns.SELECT_BY_SESSION_ID, selectionArgs);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "cleanSessions sessionId: " + sessionId
                            + " type: " + type
                            + " table: " + tableName
                            + " deleted: " + count2 + "," + count);
                }
            }
            cursor.close();

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return null;
    }

    private Bundle expandComment(Bundle extras) {
        Cursor c = null;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long sessionId = extras.getLong(EXTRA_SESSION_ID);
            c = db.query(Comments.TABLE_NAME,
                    EXPAND_PROJECTION,
                    Comments.SELECT_BY_SESSION_ID,
                    Array.of(sessionId),
                    null,
                    null,
                    Comments.SORT_BY_SEQUENCE_AND_ID);

            long id = extras.getLong(EXTRA_ID);
            long[] childIds = null;
            CursorCommentList cl = new CursorCommentList(c, 0, 2, -1);
            int count = cl.getCommentCount();
            for (int i = 0; i < count; i++) {
                if (cl.getCommentId(i) == id) {
                    childIds = CommentLogic.getChildren(cl, i);
                    break;
                }
            }

            ContentValues values = new ContentValues(2);
            values.put(Comments.COLUMN_EXPANDED, true);
            db.update(Comments.TABLE_NAME, values, ID_SELECTION, Array.of(id));

            values.clear();
            values.put(Comments.COLUMN_EXPANDED, true);
            values.put(Comments.COLUMN_VISIBLE, true);
            int childCount = childIds != null ? childIds.length : 0;
            for (int i = 0; i < childCount; i++) {
                db.update(Comments.TABLE_NAME, values, ID_SELECTION, Array.of(childIds[i]));
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        return null;
    }

    private Bundle collapseComment(Bundle extras) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(2);
            values.put(Comments.COLUMN_EXPANDED, false);
            long id = extras.getLong(EXTRA_ID);
            db.update(Comments.TABLE_NAME, values, ID_SELECTION, Array.of(id));

            values.clear();
            values.put(Comments.COLUMN_EXPANDED, true);
            values.put(Comments.COLUMN_VISIBLE, false);
            long[] childIds = extras.getLongArray(EXTRA_ID_ARRAY);
            int childCount = childIds != null ? childIds.length : 0;
            for (int i = 0; i < childCount; i++) {
                db.update(Comments.TABLE_NAME, values, ID_SELECTION, Array.of(childIds[i]));
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        return null;
    }

    private Bundle insertComment(String accountName, Bundle extras) {
        String body = getBodyExtra(extras);
        String parentThingId = getParentThingIdExtra(extras);
        String thingId = getThingIdExtra(extras);

        SQLiteDatabase db = helper.getWritableDatabase();
        SQLiteStatement updateSequence = db.compileStatement(UPDATE_SEQUENCE_STATEMENT);
        db.beginTransaction();
        try {
            // Queue an action to sync back the comment to the server.
            ContentValues values = new ContentValues(7);
            values.put(CommentActions.COLUMN_ACCOUNT, accountName);
            values.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_INSERT);
            values.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
            values.put(CommentActions.COLUMN_TEXT, body);
            values.put(CommentActions.COLUMN_THING_ID, thingId);

            long actionId = db.insert(CommentActions.TABLE_NAME, null, values);
            if (actionId == -1) {
                return null;
            }

            Cursor cursor = db.query(Sessions.TABLE_NAME,
                    SESSION_ID_PROJECTION,
                    Sessions.SELECT_BY_TYPE_AND_THING_ID,
                    Array.of(Sessions.TYPE_COMMENTS, parentThingId),
                    null,
                    null,
                    null);
            try {
                while (cursor.moveToNext()) {
                    long sessionId = cursor.getLong(SESSION_INDEX_ID);

                    // Get information from the session to figure out here to insert the comment.
                    long headerDbId = -1;
                    int position = -1;
                    int nesting = -1;
                    int sequence = -1;
                    Cursor c = db.query(Comments.TABLE_NAME,
                            INSERT_COMMENT_PROJECTION,
                            Comments.SELECT_BY_SESSION_ID,
                            Array.of(sessionId),
                            null,
                            null,
                            Comments.SORT_BY_SEQUENCE_AND_ID);
                    try {
                        while (c.moveToNext()) {
                            if (c.getPosition() == 0) {
                                headerDbId = c.getLong(INSERT_COMMENT_INDEX_ID);
                            }
                            String rowThingId = c.getString(INSERT_COMMENT_INDEX_THING);
                            if (thingId.equals(rowThingId)) {
                                position = c.getPosition();
                                break;
                            }
                        }
                        if (headerDbId == -1 || position == -1) {
                            continue;
                        }

                        CursorCommentList cl = new CursorCommentList(c,
                                INSERT_COMMENT_INDEX_ID,
                                INSERT_COMMENT_INDEX_NESTING,
                                INSERT_COMMENT_INDEX_SEQUENCE);
                        nesting = CommentLogic.getInsertNesting(cl, position);
                        sequence = CommentLogic.getInsertSequence(cl, position);
                    } finally {
                        c.close();
                    }

                    // Increment the sequence numbers to make room for our comment
                    updateSequence.bindLong(1, sessionId);
                    updateSequence.bindLong(2, sequence);
                    updateSequence.executeUpdateDelete();

                    // Insert the placeholder comment with the proper sequence number.
                    values.clear();
                    values.put(Comments.COLUMN_ACCOUNT, accountName);
                    values.put(Comments.COLUMN_AUTHOR, accountName);
                    values.put(Comments.COLUMN_BODY, body);
                    values.put(Comments.COLUMN_COMMENT_ACTION_ID, actionId);
                    values.put(Comments.COLUMN_KIND, Kinds.KIND_COMMENT);
                    values.put(Comments.COLUMN_NESTING, nesting);
                    values.put(Comments.COLUMN_SEQUENCE, sequence);
                    values.put(Comments.COLUMN_SESSION_ID, sessionId);
                    long commentId = db.insert(Comments.TABLE_NAME, null, values);
                    if (commentId == -1) {
                        return null;
                    }
                }
            } finally {
                cursor.close();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(COMMENT_ACTIONS_URI, null, SYNC);
        cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        return Bundle.EMPTY;
    }

    private Bundle editComment(String accountName, Bundle extras) {
        String body = getBodyExtra(extras);
        String parentThingId = getParentThingIdExtra(extras);
        String thingId = getThingIdExtra(extras);

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(5);
            values.put(CommentActions.COLUMN_ACCOUNT, accountName);
            values.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_EDIT);
            values.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
            values.put(CommentActions.COLUMN_TEXT, body);
            values.put(CommentActions.COLUMN_THING_ID, thingId);

            long actionId = db.insert(CommentActions.TABLE_NAME, null, values);
            if (actionId == -1) {
                return null;
            }

            values.clear();
            values.put(Comments.COLUMN_BODY, body);
            values.put(Comments.COLUMN_COMMENT_ACTION_ID, actionId);

            db.update(Comments.TABLE_NAME,
                    values,
                    SharedColumns.SELECT_BY_THING_ID,
                    Array.of(thingId));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(COMMENT_ACTIONS_URI, null, SYNC);
        cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        return Bundle.EMPTY;
    }

    private Bundle deleteComment(String accountName, Bundle extras) {
        boolean[] hasChildren = getHasChildren(extras);
        long[] ids = getIdArrayExtra(extras);
        String parentThingId = getParentThingIdExtra(extras);
        String[] thingIds = getThingIdArrayExtra(extras);

        ContentValues values = new ContentValues(5);

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int count = thingIds.length;
            for (int i = 0; i < count; i++) {
                values.clear();
                values.put(CommentActions.COLUMN_ACCOUNT, accountName);
                values.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_DELETE);
                values.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
                values.put(CommentActions.COLUMN_THING_ID, thingIds[i]);

                long actionId = db.insert(CommentActions.TABLE_NAME, null, values);
                if (actionId == -1) {
                    return null;
                }

                // Make sure to sync this logic is duplicated in CommentListing#deleteThing.
                if (Objects.equals(parentThingId, thingIds[i])) {
                    values.clear();
                    values.put(Comments.COLUMN_AUTHOR, Comments.DELETED_AUTHOR);
                    db.update(Comments.TABLE_NAME,
                            values,
                            Comments.SELECT_BY_ACCOUNT_AND_THING_ID,
                            Array.of(accountName, parentThingId));
                } else if (hasChildren[i]) {
                    values.clear();
                    values.put(Comments.COLUMN_AUTHOR, Comments.DELETED_AUTHOR);
                    values.put(Comments.COLUMN_BODY, Comments.DELETED_BODY);
                    db.update(Comments.TABLE_NAME, values, ID_SELECTION, Array.of(ids[i]));
                } else {
                    db.delete(Comments.TABLE_NAME, ID_SELECTION, Array.of(ids[i]));
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(COMMENT_ACTIONS_URI, null, SYNC);
        cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        return Bundle.EMPTY;
    }

    private Bundle insertMessage(String accountName, Bundle extras) {
        String body = getBodyExtra(extras);
        String parentThingId = getParentThingIdExtra(extras);
        String thingId = getThingIdExtra(extras);

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(7);
            values.put(MessageActions.COLUMN_ACCOUNT, accountName);
            values.put(MessageActions.COLUMN_ACTION, MessageActions.ACTION_INSERT);
            values.put(MessageActions.COLUMN_PARENT_THING_ID, parentThingId);
            values.put(MessageActions.COLUMN_TEXT, body);
            values.put(MessageActions.COLUMN_THING_ID, thingId);

            long actionId = db.insert(MessageActions.TABLE_NAME, null, values);
            if (actionId == -1) {
                return null;
            }

            Cursor cursor = db.query(Sessions.TABLE_NAME,
                    SESSION_ID_PROJECTION,
                    Sessions.SELECT_BY_TYPE_AND_THING_ID,
                    Array.of(Sessions.TYPE_MESSAGE_THREAD, thingId),
                    null,
                    null,
                    null);
            try {
                while (cursor.moveToNext()) {
                    long sessionId = cursor.getLong(SESSION_INDEX_ID);

                    values.clear();
                    values.put(Messages.COLUMN_ACCOUNT, accountName);
                    values.put(Messages.COLUMN_AUTHOR, accountName);
                    values.put(Messages.COLUMN_BODY, body);
                    values.put(Messages.COLUMN_KIND, Kinds.KIND_MESSAGE);
                    values.put(Messages.COLUMN_MESSAGE_ACTION_ID, actionId);
                    values.put(Messages.COLUMN_SESSION_ID, sessionId);
                    values.put(Messages.COLUMN_WAS_COMMENT, false);

                    long messageId = db.insert(Messages.TABLE_NAME, null, values);
                    if (messageId == -1) {
                        return null;
                    }
                }
            } finally {
                cursor.close();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(MESSAGE_ACTIONS_URI, null, SYNC);
        cr.notifyChange(MESSAGES_URI, null, NO_SYNC);
        return Bundle.EMPTY;
    }

    private Bundle readMessage(String accountName, Bundle extras) {
        int action = getActionExtra(extras);
        String thingId = getThingIdExtra(extras);

        int changed = 0;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues(3);
            v.put(ReadActions.COLUMN_ACCOUNT, accountName);
            v.put(ReadActions.COLUMN_ACTION, action);
            v.put(ReadActions.COLUMN_THING_ID, thingId);

            long actionId = db.replace(ReadActions.TABLE_NAME, null, v);
            if (actionId == -1) {
                return null;
            }

            changed += ReadMerger.updateDatabase(db, accountName, action, thingId, v);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(READ_ACTIONS_URI, null, SYNC);
        if (changed > 0) {
            cr.notifyChange(MESSAGES_URI, null, NO_SYNC);
        }
        return Bundle.EMPTY;
    }

    private Bundle hide(String accountName, Bundle extras) {
        int action = getActionExtra(extras);
        String thingId = getThingIdExtra(extras);
        ThingBundle thingBundle = getThingBundleExtra(extras);

        int changed = 0;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues(thingBundle == null ? 3 : 19);
            v.put(HideActions.COLUMN_ACCOUNT, accountName);
            v.put(HideActions.COLUMN_ACTION, action);
            v.put(HideActions.COLUMN_THING_ID, thingId);

            if (thingBundle != null) {
                v.put(HideActions.COLUMN_AUTHOR, thingBundle.getAuthor());
                v.put(HideActions.COLUMN_CREATED_UTC, thingBundle.getCreatedUtc());
                v.put(HideActions.COLUMN_DOMAIN, thingBundle.getDomain());
                v.put(HideActions.COLUMN_DOWNS, thingBundle.getDowns());
                v.put(HideActions.COLUMN_LIKES, thingBundle.getLikes());
                v.put(HideActions.COLUMN_NUM_COMMENTS, thingBundle.getNumComments());
                v.put(HideActions.COLUMN_OVER_18, thingBundle.isOver18());
                v.put(HideActions.COLUMN_PERMA_LINK, thingBundle.getPermaLink());
                v.put(HideActions.COLUMN_SCORE, thingBundle.getScore());
                v.put(HideActions.COLUMN_SELF, thingBundle.isSelf());
                v.put(HideActions.COLUMN_SUBREDDIT, thingBundle.getSubreddit());
                v.put(HideActions.COLUMN_TITLE, thingBundle.getTitle());
                v.put(HideActions.COLUMN_THUMBNAIL_URL, thingBundle.getThumbnailUrl());
                v.put(HideActions.COLUMN_UPS, thingBundle.getUps());
                v.put(HideActions.COLUMN_URL, thingBundle.getUrl());
            }

            long actionId = db.replace(HideActions.TABLE_NAME, null, v);
            if (actionId == -1) {
                return null;
            }

            changed += HideMerger.updateDatabase(db, accountName, action, thingId, v);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(HIDE_ACTIONS_URI, null, SYNC);
        if (changed > 0) {
            cr.notifyChange(THINGS_URI, null, NO_SYNC);
        }
        return Bundle.EMPTY;
    }

    private Bundle save(String accountName, Bundle extras) {
        int action = getActionExtra(extras);
        String thingId = getThingIdExtra(extras);
        ThingBundle thingBundle = getThingBundleExtra(extras);

        int changed = 0;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues(thingBundle == null ? 4 : 19);
            v.put(SaveActions.COLUMN_ACCOUNT, accountName);
            v.put(SaveActions.COLUMN_ACTION, action);
            v.put(SaveActions.COLUMN_THING_ID, thingId);

            if (thingBundle != null) {
                v.put(SaveActions.COLUMN_AUTHOR, thingBundle.getAuthor());
                v.put(SaveActions.COLUMN_CREATED_UTC, thingBundle.getCreatedUtc());
                v.put(SaveActions.COLUMN_DOMAIN, thingBundle.getDomain());
                v.put(SaveActions.COLUMN_DOWNS, thingBundle.getDowns());
                v.put(SaveActions.COLUMN_LIKES, thingBundle.getLikes());
                v.put(SaveActions.COLUMN_NUM_COMMENTS, thingBundle.getNumComments());
                v.put(SaveActions.COLUMN_OVER_18, thingBundle.isOver18());
                v.put(SaveActions.COLUMN_PERMA_LINK, thingBundle.getPermaLink());
                v.put(SaveActions.COLUMN_SCORE, thingBundle.getScore());
                v.put(SaveActions.COLUMN_SELF, thingBundle.isSelf());
                v.put(SaveActions.COLUMN_SUBREDDIT, thingBundle.getSubreddit());
                v.put(SaveActions.COLUMN_TITLE, thingBundle.getTitle());
                v.put(SaveActions.COLUMN_THUMBNAIL_URL, thingBundle.getThumbnailUrl());
                v.put(SaveActions.COLUMN_UPS, thingBundle.getUps());
                v.put(SaveActions.COLUMN_URL, thingBundle.getUrl());
            }

            long actionId = db.replace(SaveActions.TABLE_NAME, null, v);
            if (actionId == -1) {
                return null;
            }

            changed += SaveMerger.updateDatabase(db, accountName, action, thingId, v);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(SAVE_ACTIONS_URI, null, SYNC);
        if (changed > 0) {
            cr.notifyChange(THINGS_URI, null, NO_SYNC);
            cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        }
        return Bundle.EMPTY;
    }

    private Bundle vote(String accountName, Bundle extras) {
        int action = getActionExtra(extras);
        String thingId = getThingIdExtra(extras);
        ThingBundle thingBundle = getThingBundleExtra(extras);

        int changed = 0;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues(thingBundle == null ? 4 : 19);
            v.put(VoteActions.COLUMN_ACCOUNT, accountName);
            v.put(VoteActions.COLUMN_ACTION, action);
            v.put(VoteActions.COLUMN_THING_ID, thingId);
            v.put(VoteActions.COLUMN_SHOW_IN_LISTING, thingBundle != null);

            if (thingBundle != null) {
                v.put(VoteActions.COLUMN_AUTHOR, thingBundle.getAuthor());
                v.put(VoteActions.COLUMN_CREATED_UTC, thingBundle.getCreatedUtc());
                v.put(VoteActions.COLUMN_DOMAIN, thingBundle.getDomain());
                v.put(VoteActions.COLUMN_DOWNS, thingBundle.getDowns());
                v.put(VoteActions.COLUMN_LIKES, thingBundle.getLikes());
                v.put(VoteActions.COLUMN_NUM_COMMENTS, thingBundle.getNumComments());
                v.put(VoteActions.COLUMN_OVER_18, thingBundle.isOver18());
                v.put(VoteActions.COLUMN_PERMA_LINK, thingBundle.getPermaLink());
                v.put(VoteActions.COLUMN_SCORE, thingBundle.getScore());
                v.put(VoteActions.COLUMN_SELF, thingBundle.isSelf());
                v.put(VoteActions.COLUMN_SUBREDDIT, thingBundle.getSubreddit());
                v.put(VoteActions.COLUMN_TITLE, thingBundle.getTitle());
                v.put(VoteActions.COLUMN_THUMBNAIL_URL, thingBundle.getThumbnailUrl());
                v.put(VoteActions.COLUMN_UPS, thingBundle.getUps());
                v.put(VoteActions.COLUMN_URL, thingBundle.getUrl());
            }

            long actionId = db.replace(VoteActions.TABLE_NAME, null, v);
            if (actionId == -1) {
                return null;
            }

            changed += VoteMerger.updateDatabase(db, accountName, action, thingId);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(VOTE_ACTIONS_URI, null, SYNC);
        if (changed > 0) {
            cr.notifyChange(THINGS_URI, null, NO_SYNC);
            cr.notifyChange(COMMENTS_URI, null, NO_SYNC);
        }
        return Bundle.EMPTY;
    }

    private int getActionExtra(Bundle extras) {
        return extras.getInt(EXTRA_ACTION);
    }

    private static String getBodyExtra(Bundle extras) {
        return extras.getString(EXTRA_BODY);
    }

    private static boolean[] getHasChildren(Bundle extras) {
        return extras.getBooleanArray(EXTRA_HAS_CHILDREN);
    }

    private static long[] getIdArrayExtra(Bundle extras) {
        return extras.getLongArray(EXTRA_ID_ARRAY);
    }

    private static String getParentThingIdExtra(Bundle extras) {
        return extras.getString(EXTRA_PARENT_THING_ID);
    }

    private static ThingBundle getThingBundleExtra(Bundle extras) {
        return extras.getParcelable(EXTRA_THING_BUNDLE);
    }

    private static String getThingIdExtra(Bundle extras) {
        return extras.getString(EXTRA_THING_ID);
    }

    private static String[] getThingIdArrayExtra(Bundle extras) {
        return extras.getStringArray(EXTRA_THING_ID_ARRAY);
    }
}
