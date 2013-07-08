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
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
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
import com.btmura.android.reddit.widget.FilterAdapter;

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

    public static final Uri THINGS_SYNC_URI = makeSyncUri(THINGS_URI);
    public static final Uri MESSAGES_SYNC_URI = makeSyncUri(MESSAGES_URI);
    public static final Uri COMMENT_ACTIONS_SYNC_URI = makeSyncUri(COMMENT_ACTIONS_URI);

    public static final Uri THINGS_WITH_ACTIONS_URI = makeJoinUri(THINGS_URI);
    public static final Uri COMMENTS_WITH_ACTIONS_URI = makeJoinUri(COMMENTS_URI);
    public static final Uri MESSAGES_WITH_ACTIONS_URI = makeJoinUri(MESSAGES_URI);

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

    static final String PARAM_NOTIFY_ACCOUNTS = "notifyAccounts";
    static final String PARAM_NOTIFY_COMMENTS = "notifyComments";
    static final String PARAM_NOTIFY_THINGS = "notifyThings";
    static final String PARAM_NOTIFY_MESSAGES = "notifyMessages";

    private static final String PARAM_JOIN = "join";

    private static final Uri makeJoinUri(Uri uri) {
        return uri.buildUpon().appendQueryParameter(PARAM_JOIN, TRUE).build();
    }

    private static final String JOINED_TABLE = ""
            // Join with pending hides to fake that the things were hidden.
            + " LEFT OUTER JOIN (SELECT "
            + HideActions.COLUMN_ACCOUNT + ","
            + HideActions.COLUMN_THING_ID + ","
            + HideActions.COLUMN_ACTION + " AS " + SharedColumns.COLUMN_HIDE_ACTION
            + " FROM " + HideActions.TABLE_NAME + ") USING ("
            + HideActions.COLUMN_ACCOUNT + ","
            + SharedColumns.COLUMN_THING_ID + ")"

            // Join with pending saves to fake that the save happened.
            + " LEFT OUTER JOIN (SELECT "
            + SaveActions.COLUMN_ACCOUNT + ","
            + SaveActions.COLUMN_THING_ID + ","
            + SaveActions.COLUMN_ACTION + " AS " + SharedColumns.COLUMN_SAVE_ACTION
            + " FROM " + SaveActions.TABLE_NAME + ") USING ("
            + SaveActions.COLUMN_ACCOUNT + ", "
            + SharedColumns.COLUMN_THING_ID + ")"

            // Join with pending votes to fake that the vote happened.
            + " LEFT OUTER JOIN (SELECT "
            + VoteActions.COLUMN_ACCOUNT + ","
            + VoteActions.COLUMN_THING_ID + ","
            + VoteActions.COLUMN_ACTION + " AS " + SharedColumns.COLUMN_VOTE_ACTION
            + " FROM " + VoteActions.TABLE_NAME + ") USING ("
            + VoteActions.COLUMN_ACCOUNT + ","
            + SharedColumns.COLUMN_THING_ID + ")";

    private static final String JOINED_THINGS_TABLE = Things.TABLE_NAME + JOINED_TABLE;

    private static final String JOINED_COMMENTS_TABLE = Comments.TABLE_NAME + JOINED_TABLE;

    private static final String JOINED_MESSAGES_TABLE = Messages.TABLE_NAME
            // Join with pending actions to decide if need to mark as read.
            + " LEFT OUTER JOIN (SELECT "
            + ReadActions.COLUMN_ACCOUNT + ", "
            + ReadActions.COLUMN_THING_ID + ", "
            + ReadActions.COLUMN_ACTION
            + " FROM " + ReadActions.TABLE_NAME + ") USING ("
            + ReadActions.COLUMN_ACCOUNT + ", "
            + SharedColumns.COLUMN_THING_ID + ")";

    /** Method to collapse a comment in a listing session. */
    private static final String METHOD_COLLAPSE_COMMENT = "collapseComment";

    /** Method to expand a comment in a listing session. */
    private static final String METHOD_EXPAND_COMMENT = "expandComment";

    /** Method to create a listing session of some kind. */
    private static final String METHOD_GET_SESSION = "getSession";

    /** Method to insert a pending comment in a listing. */
    private static final String METHOD_INSERT_COMMENT = "insertComment";

    /** Method to insert a pending message in a listing. */
    private static final String METHOD_INSERT_MESSAGE = "insertMessage";

    // List of extras used throughout the provider code.

    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_COUNT = "count";
    public static final String EXTRA_FILTER = "filter";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_IDS = "ids";
    public static final String EXTRA_LINK_ID = "linkId";
    public static final String EXTRA_MARK = "mark";
    public static final String EXTRA_MORE = "more";
    public static final String EXTRA_NESTING = "nesting";
    public static final String EXTRA_PARENT_ID = "parentId";
    public static final String EXTRA_PARENT_NUM_COMMENTS = "parentNumComments";
    public static final String EXTRA_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_RESOLVED_SUBREDDIT = "resolvedSubreddit";
    public static final String EXTRA_SEQUENCE = "sequence";
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_SESSION_TYPE = "sessionType";
    public static final String EXTRA_SUBREDDIT = "subreddit";
    public static final String EXTRA_THING_ID = "thingId";
    public static final String EXTRA_USER = "user";

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

    private static final String UPDATE_NUM_COMMENTS_STATEMENT = "UPDATE " + Comments.TABLE_NAME
            + " SET " + Comments.COLUMN_NUM_COMMENTS + "=" + Comments.COLUMN_NUM_COMMENTS + "+1"
            + " WHERE " + Comments._ID + "=?";

    private static final String UPDATE_NUM_COMMENTS_STATEMENT_2 = "UPDATE " + Things.TABLE_NAME
            + " SET " + Things.COLUMN_NUM_COMMENTS + "=" + Things.COLUMN_NUM_COMMENTS + "+1"
            + " WHERE " + Things.SELECT_BY_ACCOUNT_AND_THING_ID;

    private static final String SELECT_MORE_WITH_SESSION_ID = Kinds.COLUMN_KIND + "="
            + Kinds.KIND_MORE + " AND " + SharedColumns.COLUMN_SESSION_ID + "=?";

    private static final boolean SYNC = true;

    private static final boolean NO_SYNC = false;

    public static final Bundle getSubredditSession(Context context, String accountName,
            String subreddit, int filter, String more, long sessionId) {
        Bundle extras = new Bundle(5);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_SUBREDDIT);
        extras.putString(EXTRA_SUBREDDIT, subreddit);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putString(EXTRA_MORE, more);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        return call(context, SUBREDDITS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Bundle getProfileSession(Context context, String accountName,
            String profileUser, int filter, String more, long sessionId) {
        Bundle extras = new Bundle(4);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_USER);
        extras.putString(EXTRA_USER, profileUser);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putString(EXTRA_MORE, more);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        return call(context, THINGS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Bundle getCommentsSession(Context context, String accountName,
            String thingId, String linkId, long sessionId, int numComments) {
        Bundle extras = new Bundle(5);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_COMMENTS);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putString(EXTRA_LINK_ID, linkId);
        extras.putInt(EXTRA_COUNT, numComments);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        return call(context, COMMENTS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Bundle getThingSearchSession(Context context, String accountName,
            String subreddit, String query, long sessionId) {
        Bundle extras = new Bundle(4);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_THING_SEARCH);
        extras.putString(EXTRA_SUBREDDIT, subreddit);
        extras.putString(EXTRA_QUERY, query);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        return call(context, THINGS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Bundle getSubredditSearchSession(Context context, String accountName,
            String query) {
        Bundle extras = new Bundle(2);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_SUBREDDIT_SEARCH);
        extras.putString(EXTRA_QUERY, query);
        return call(context, SUBREDDITS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Bundle getMessageSession(Context context, String accountName,
            int filter, String more, long sessionId) {
        Bundle extras = new Bundle(4);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_MESSAGES);
        extras.putInt(EXTRA_FILTER, filter);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        if (!TextUtils.isEmpty(more)) {
            extras.putString(EXTRA_MORE, more);
        } else if (filter == FilterAdapter.MESSAGE_INBOX
                || filter == FilterAdapter.MESSAGE_UNREAD) {
            extras.putBoolean(EXTRA_MARK, true);
        }
        return call(context, MESSAGES_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Bundle getMessageThreadSession(Context context, String accountName,
            String thingId, long sessionId) {
        Bundle extras = new Bundle(3);
        extras.putInt(EXTRA_SESSION_TYPE, Sessions.TYPE_MESSAGE_THREAD);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        return call(context, MESSAGES_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final void expandComment(Context context, long id, long sessionId) {
        Bundle extras = new Bundle(2);
        extras.putLong(EXTRA_ID, id);
        extras.putLong(EXTRA_SESSION_ID, sessionId);
        call(context, COMMENTS_URI, METHOD_EXPAND_COMMENT, null, extras);
    }

    public static final void collapseComment(Context context, long id, long[] childIds) {
        Bundle extras = new Bundle(2);
        extras.putLong(EXTRA_ID, id);
        extras.putLongArray(EXTRA_IDS, childIds);
        call(context, COMMENTS_URI, METHOD_COLLAPSE_COMMENT, null, extras);
    }

    public static final Bundle insertComment(Context context, String accountName,
            String body, String parentThingId, String thingId) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_BODY, body);
        extras.putString(EXTRA_PARENT_THING_ID, parentThingId);
        extras.putString(EXTRA_THING_ID, thingId);
        return call(context, COMMENT_ACTIONS_URI, METHOD_INSERT_COMMENT, accountName, extras);
    }

    public static final Bundle insertMessage(Context context, String accountName,
            String body, String parentThingId, String thingId) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_BODY, body);
        extras.putString(EXTRA_PARENT_THING_ID, parentThingId);
        extras.putString(EXTRA_THING_ID, thingId);
        return call(context, MESSAGE_ACTIONS_URI, METHOD_INSERT_MESSAGE, accountName, extras);
    }

    private static Bundle call(Context context, Uri uri, String method,
            String arg, Bundle extras) {
        return context.getApplicationContext().getContentResolver().call(uri, method, arg, extras);
    }

    public ThingProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        switch (MATCHER.match(uri)) {
            case MATCH_THINGS:
                return uri.getBooleanQueryParameter(PARAM_JOIN, false)
                        ? JOINED_THINGS_TABLE
                        : Things.TABLE_NAME;

            case MATCH_COMMENTS:
                return uri.getBooleanQueryParameter(PARAM_JOIN, false)
                        ? JOINED_COMMENTS_TABLE
                        : Comments.TABLE_NAME;

            case MATCH_MESSAGES:
                return uri.getBooleanQueryParameter(PARAM_JOIN, false)
                        ? JOINED_MESSAGES_TABLE
                        : Messages.TABLE_NAME;

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

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        try {
            if (METHOD_GET_SESSION.equals(method)) {
                return getSession(arg, extras);
            } else if (METHOD_EXPAND_COMMENT.equals(method)) {
                return expandComment(extras);
            } else if (METHOD_COLLAPSE_COMMENT.equals(method)) {
                return collapseComment(extras);
            } else if (METHOD_INSERT_COMMENT.equals(method)) {
                return insertComment(arg, extras);
            } else if (METHOD_INSERT_MESSAGE.equals(method)) {
                return insertMessage(arg, extras);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    private Bundle getSession(String accountName, Bundle extras) {
        try {
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
            long sessionId = extras.getLong(EXTRA_SESSION_ID);
            String subreddit = extras.getString(EXTRA_SUBREDDIT);
            String thingId = extras.getString(EXTRA_THING_ID);
            String user = extras.getString(EXTRA_USER);

            int listingType = extras.getInt(EXTRA_SESSION_TYPE);
            Listing listing = null;
            switch (listingType) {
                case Sessions.TYPE_MESSAGE_THREAD:
                    listing = MessageListing.newThreadInstance(helper, accountName,
                            thingId, cookie);
                    break;

                case Sessions.TYPE_MESSAGES:
                    listing = MessageListing.newInstance(helper, accountName,
                            filter, more, mark, cookie);
                    break;

                case Sessions.TYPE_SUBREDDIT:
                    listing = ThingListing.newSubredditInstance(context, helper, accountName,
                            subreddit, filter, more, cookie);
                    break;

                case Sessions.TYPE_USER:
                    listing = ThingListing.newUserInstance(context, helper, accountName,
                            user, filter, more, cookie);
                    break;

                case Sessions.TYPE_COMMENTS:
                    listing = CommentListing.newInstance(context, helper, accountName,
                            thingId, linkId, count, cookie);
                    break;

                case Sessions.TYPE_THING_SEARCH:
                    listing = ThingListing.newSearchInstance(context, helper, accountName,
                            subreddit, query, cookie);
                    break;

                case Sessions.TYPE_SUBREDDIT_SEARCH:
                    listing = SubredditResultListing.newInstance(accountName,
                            query, cookie);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            sessionId = getListingSession(listing, sessionId);
            Bundle result = new Bundle(2);
            result.putLong(EXTRA_SESSION_ID, sessionId);
            listing.addCursorExtras(result);
            listing.performExtraWork(getContext());
            return result;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private long getListingSession(Listing listing, long sessionId) throws IOException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getListingSession sessionId: " + sessionId);
        }

        // Don't get new values if the session appears valid.
        if (sessionId != 0 && !listing.isAppend()) {
            return sessionId;
        }

        // Get new values over the network.
        ArrayList<ContentValues> values = listing.getValues();

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
                    return sessionId;
                }
            }

            // Create a new session if there is no id.
            if (sessionId == 0) {
                ContentValues v = new ContentValues(3);
                v.put(Sessions.COLUMN_TYPE, listing.getSessionType());
                v.put(Sessions.COLUMN_TAG, listing.getSessionTag());
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
            return sessionId;
        } finally {
            db.endTransaction();
        }
    }

    private Bundle expandComment(Bundle extras) {
        Cursor c = null;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long sessionId = extras.getLong(EXTRA_SESSION_ID);
            c = db.query(Comments.TABLE_NAME, EXPAND_PROJECTION,
                    Comments.SELECT_BY_SESSION_ID, Array.of(sessionId),
                    null, null, Comments.SORT_BY_SEQUENCE_AND_ID);

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
            long[] childIds = extras.getLongArray(EXTRA_IDS);
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
        SQLiteStatement updateNumComments = db.compileStatement(UPDATE_NUM_COMMENTS_STATEMENT);
        SQLiteStatement updateNumComments2 = db.compileStatement(UPDATE_NUM_COMMENTS_STATEMENT_2);
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

            // TODO: Add account scoping to sessions table.

            Cursor cursor = db.query(Sessions.TABLE_NAME, SESSION_ID_PROJECTION,
                    Sessions.SELECT_BY_TYPE_AND_TAG,
                    Array.of(Sessions.TYPE_COMMENTS, parentThingId),
                    null, null, null);
            try {
                while (cursor.moveToNext()) {
                    long sessionId = cursor.getLong(SESSION_INDEX_ID);

                    // Get information from the session to figure out here to insert the comment.
                    long headerDbId = -1;
                    int position = -1;
                    int nesting = -1;
                    int sequence = -1;
                    Cursor c = db.query(Comments.TABLE_NAME, INSERT_COMMENT_PROJECTION,
                            Comments.SELECT_BY_SESSION_ID, Array.of(sessionId), null, null,
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

                    // Update the number of comments in the header comment.
                    updateNumComments.bindLong(1, headerDbId);
                    updateNumComments.executeUpdateDelete();

                    // Update the number of comments in any thing listings.
                    updateNumComments2.bindString(1, accountName);
                    updateNumComments2.bindString(2, parentThingId);
                    updateNumComments2.executeUpdateDelete();

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
        cr.notifyChange(THINGS_URI, null, NO_SYNC);
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

            // TODO: Add account scoping to sessions table.

            Cursor cursor = db.query(Sessions.TABLE_NAME, SESSION_ID_PROJECTION,
                    Sessions.SELECT_BY_TYPE_AND_TAG,
                    Array.of(Sessions.TYPE_MESSAGE_THREAD, thingId),
                    null, null, null);
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

    @Override
    protected void notifyChange(Uri uri) {
        super.notifyChange(uri);
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_ACCOUNTS, false)) {
            getContext().getContentResolver().notifyChange(AccountProvider.ACCOUNTS_URI, null);
        }
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_COMMENTS, false)) {
            getContext().getContentResolver().notifyChange(COMMENTS_URI, null);
        }
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_THINGS, false)) {
            getContext().getContentResolver().notifyChange(THINGS_URI, null);
        }
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_MESSAGES, false)) {
            getContext().getContentResolver().notifyChange(MESSAGES_URI, null);
        }
    }

    private static String getBodyExtra(Bundle extras) {
        return extras.getString(EXTRA_BODY);
    }

    private static String getParentThingIdExtra(Bundle extras) {
        return extras.getString(EXTRA_PARENT_THING_ID);
    }

    private static String getThingIdExtra(Bundle extras) {
        return extras.getString(EXTRA_THING_ID);
    }
}
