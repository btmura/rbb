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
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
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

    public static final String EXTRA_RESOLVED_SUBREDDIT = "resolvedSubreddit";

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

    static final String PARAM_LISTING_GET = "getListing";
    static final String PARAM_LISTING_TYPE = "listingType";

    static final String PARAM_ACCOUNT = "account";
    static final String PARAM_SUBREDDIT = "subreddit";
    static final String PARAM_QUERY = "query";
    static final String PARAM_PROFILE_USER = "profileUser";
    static final String PARAM_FILTER = "filter";
    static final String PARAM_MORE = "more";
    static final String PARAM_MARK = "mark";
    static final String PARAM_THING_ID = "thingId";
    static final String PARAM_LINK_ID = "linkId";
    static final String PARAM_COUNT = "count";
    static final String PARAM_JOIN = "join";
    static final String PARAM_NOTIFY_ACCOUNTS = "notifyAccounts";
    static final String PARAM_NOTIFY_COMMENTS = "notifyComments";
    static final String PARAM_NOTIFY_THINGS = "notifyThings";
    static final String PARAM_NOTIFY_MESSAGES = "notifyMessages";

    private static final String JOINED_TABLE = ""
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
            + SharedColumns.COLUMN_THING_ID + ")"

            // Join with pending hides to fake that the things were hidden.
            + " LEFT OUTER JOIN (SELECT "
            + HideActions.COLUMN_ACCOUNT + ","
            + HideActions.COLUMN_THING_ID + ","
            + HideActions.COLUMN_ACTION + " AS " + SharedColumns.COLUMN_LOCAL_HIDDEN
            + " FROM " + HideActions.TABLE_NAME + ") USING ("
            + HideActions.COLUMN_ACCOUNT + ","
            + SharedColumns.COLUMN_THING_ID + ")";

    private static final String JOINED_THINGS_TABLE = Things.TABLE_NAME + JOINED_TABLE;

    private static final String JOINED_COMMENTS_TABLE = Comments.TABLE_NAME + JOINED_TABLE;

    // TODO: Make a separate table for just read actions?
    private static final String JOINED_MESSAGES_TABLE = Messages.TABLE_NAME
            // Join with pending actions to decide if need to mark as read.
            + " LEFT OUTER JOIN (SELECT "
            + ReadActions.COLUMN_ACCOUNT + ", "
            + ReadActions.COLUMN_THING_ID + ", "
            + ReadActions.COLUMN_ACTION
            + " FROM " + ReadActions.TABLE_NAME + ") USING ("
            + ReadActions.COLUMN_ACCOUNT + ", "
            + SharedColumns.COLUMN_THING_ID + ")";

    public static final String METHOD_GET_SESSION = "getSession";

    /** Method to insert a pending comment in a listing. */
    static final String METHOD_INSERT_COMMENT = "insertComment";

    public static final String EXTRA_COUNT = "count";
    public static final String EXTRA_FILTER = "filter";
    public static final String EXTRA_LINK_ID = "linkId";
    public static final String EXTRA_MARK = "mark";
    public static final String EXTRA_MORE = "more";
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_SESSION_TYPE = "sessionType";
    public static final String EXTRA_SUBREDDIT = "subreddit";
    public static final String EXTRA_THING_ID = "thingId";
    public static final String EXTRA_USER = "user";

    static final String CALL_EXTRA_ACCOUNT = "account";
    static final String CALL_EXTRA_BODY = "body";
    static final String CALL_EXTRA_NESTING = "nesting";
    static final String CALL_EXTRA_PARENT_ID = "parentId";
    static final String CALL_EXTRA_PARENT_NUM_COMMENTS = "parentNumComments";
    static final String CALL_EXTRA_PARENT_THING_ID = "parentThingId";
    static final String CALL_EXTRA_SEQUENCE = "sequence";
    static final String CALL_EXTRA_SESSION_ID = "sessionId";
    static final String CALL_EXTRA_THING_ID = "thingId";

    private static final String UPDATE_SEQUENCE_STATEMENT = "UPDATE " + Comments.TABLE_NAME
            + " SET " + Comments.COLUMN_SEQUENCE + "=" + Comments.COLUMN_SEQUENCE + "+1"
            + " WHERE " + Comments.COLUMN_SESSION_ID + "=? AND " + Comments.COLUMN_SEQUENCE + ">=?";

    private static final String SELECT_MORE_WITH_SESSION_ID = Kinds.COLUMN_KIND + "="
            + Kinds.KIND_MORE + " AND " + SharedColumns.COLUMN_SESSION_ID + "=?";

    public static final Uri subredditUri(String accountName, String subreddit, int filter,
            String more) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_SUBREDDIT));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_SUBREDDIT, subreddit);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        }
        return b.build();
    }

    public static final Bundle getComments(Context context, String accountName,
            String thingId, String linkId, int numComments) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_THING_ID, thingId);
        extras.putString(EXTRA_LINK_ID, linkId);
        extras.putInt(EXTRA_COUNT, numComments);
        ContentResolver cr = context.getApplicationContext().getContentResolver();
        return cr.call(ThingProvider.COMMENTS_URI, METHOD_GET_SESSION, accountName, extras);
    }

    public static final Uri profileUri(String accountName, String profileUser, int filter,
            String more) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_USER));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_PROFILE_USER, profileUser);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        }
        return b.build();
    }

    public static final Uri searchUri(String accountName, String subreddit, String query) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_THING_SEARCH));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_QUERY, query);
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(subreddit)) {
            b.appendQueryParameter(PARAM_SUBREDDIT, subreddit);
        }
        return b.build();
    }

    public static final Uri subredditSearchUri(String accountName, String query) {
        Uri.Builder b = SUBREDDITS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_SUBREDDIT_SEARCH));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_QUERY, query);
        return b.build();
    }

    public static final Uri messageUri(String accountName, int filter, String more) {
        Uri.Builder b = MESSAGES_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_MESSAGES));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        } else if (filter == FilterAdapter.MESSAGE_INBOX
                || filter == FilterAdapter.MESSAGE_UNREAD) {
            b.appendQueryParameter(PARAM_MARK, TRUE);
            b.appendQueryParameter(PARAM_NOTIFY_ACCOUNTS, TRUE);
        }
        return b.build();
    }

    public static final Uri messageThreadUri(String accountName, String thingId) {
        Uri.Builder b = MESSAGES_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_MESSAGE_THREAD));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_THING_ID, thingId);
        return b.build();
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
        if (METHOD_GET_SESSION.equals(method)) {
            return getSession(arg, extras);
        } else if (METHOD_INSERT_COMMENT.equals(method)) {
            return insertComment(extras);
        }
        return null;
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

            int listingType = extras.getInt(EXTRA_SESSION_TYPE, -1);
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

    long getListingSession(Listing listing, long sessionId) throws IOException {
        SQLiteDatabase db = helper.getWritableDatabase();

        // Get new values over the network.
        ArrayList<ContentValues> values = listing.getValues();

        // Delete any existing "Loading..." signs if appending.
        if (listing.isAppend()) {
            // Appending requires an existing session to append the data.
            if (sessionId == -1) {
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
        if (sessionId == -1) {
            ContentValues v = new ContentValues(3);
            v.put(Sessions.COLUMN_TYPE, listing.getSessionType());
            v.put(Sessions.COLUMN_TAG, listing.getSessionTag());
            v.put(Sessions.COLUMN_TIMESTAMP, System.currentTimeMillis());
            sessionId = db.insert(Sessions.TABLE_NAME, null, v);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "created session: " + sessionId);
            }
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

        return sessionId;
    }

    private Bundle insertComment(Bundle extras) {
        String accountName = extras.getString(CALL_EXTRA_ACCOUNT);
        String body = extras.getString(CALL_EXTRA_BODY);
        int nesting = extras.getInt(CALL_EXTRA_NESTING);
        long parentId = extras.getLong(CALL_EXTRA_PARENT_ID);
        int parentNumComments = extras.getInt(CALL_EXTRA_PARENT_NUM_COMMENTS);
        String parentThingId = extras.getString(CALL_EXTRA_PARENT_THING_ID);
        int sequence = extras.getInt(CALL_EXTRA_SEQUENCE);
        long sessionId = extras.getLong(CALL_EXTRA_SESSION_ID);
        String thingId = extras.getString(CALL_EXTRA_THING_ID);

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Update the number of comments in the header comment.
            ContentValues values = new ContentValues(8);
            values.put(Comments.COLUMN_NUM_COMMENTS, parentNumComments + 1);
            int count = db.update(Comments.TABLE_NAME, values, ID_SELECTION, Array.of(parentId));
            if (count != 1) {
                return null;
            }

            // Update the number of comments in any thing listings.
            values.clear();
            values.put(Things.COLUMN_NUM_COMMENTS, parentNumComments + 1);
            db.update(Things.TABLE_NAME, values, Things.SELECT_BY_ACCOUNT_AND_THING_ID,
                    Array.of(accountName, parentThingId));

            // Queue an action to sync back the comment to the server.
            values.clear();
            values.put(CommentActions.COLUMN_ACCOUNT, accountName);
            values.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_INSERT);
            values.put(CommentActions.COLUMN_TEXT, body);
            values.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
            values.put(CommentActions.COLUMN_THING_ID, thingId);
            long commentActionId = db.insert(CommentActions.TABLE_NAME, null, values);
            if (commentActionId == -1) {
                return null;
            }

            // Increment the sequence numbers to make room for our comment.
            SQLiteStatement sql = db.compileStatement(UPDATE_SEQUENCE_STATEMENT);
            sql.bindLong(1, sessionId);
            sql.bindLong(2, sequence);
            sql.executeUpdateDelete();

            // Insert the placeholder comment with the proper sequence number.
            values.clear();
            values.put(Comments.COLUMN_ACCOUNT, accountName);
            values.put(Comments.COLUMN_AUTHOR, accountName);
            values.put(Comments.COLUMN_BODY, body);
            values.put(Comments.COLUMN_COMMENT_ACTION_ID, commentActionId);
            values.put(Comments.COLUMN_KIND, Kinds.KIND_COMMENT);
            values.put(Comments.COLUMN_NESTING, nesting);
            values.put(Comments.COLUMN_SEQUENCE, sequence);
            values.put(Comments.COLUMN_SESSION_ID, sessionId);
            long commentId = db.insert(Comments.TABLE_NAME, null, values);
            if (commentId == -1) {
                return null;
            }

            db.setTransactionSuccessful();

            // Update observers and schedule a sync. Both URIs are backed by the same sync adapter.
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(ThingProvider.THINGS_URI, null, false);
            cr.notifyChange(ThingProvider.COMMENTS_URI, null, true);

        } finally {
            db.endTransaction();
        }
        return null;
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
}
