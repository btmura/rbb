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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.widget.FilterAdapter;

/**
 * URI MATCHING PATTERNS:
 *
 * <pre>
 * /things
 * /messages
 * /subreddits
 * /actions/comments
 * /actions/messages
 * /actions/reads
 * /actions/saves
 * /actions/votes
 * </pre>
 */
public class ThingProvider extends SessionProvider {

    public static final String TAG = "ThingProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";
    static final String AUTHORITY_URI = "content://" + AUTHORITY + "/";

    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_RESOLVED_SUBREDDIT = "resolvedSubreddit";

    private static final String PATH_THINGS = "things";
    private static final String PATH_MESSAGES = "messages";
    private static final String PATH_SUBREDDITS = "subreddits";
    private static final String PATH_COMMENT_ACTIONS = "actions/comments";
    private static final String PATH_MESSAGE_ACTIONS = "actions/messages";
    private static final String PATH_READ_ACTIONS = "actions/read";
    private static final String PATH_SAVE_ACTIONS = "actions/saves";
    private static final String PATH_VOTE_ACTIONS = "actions/votes";

    public static final Uri THINGS_URI = Uri.parse(AUTHORITY_URI + PATH_THINGS);
    public static final Uri MESSAGES_URI = Uri.parse(AUTHORITY_URI + PATH_MESSAGES);
    public static final Uri SUBREDDITS_URI = Uri.parse(AUTHORITY_URI + PATH_SUBREDDITS);
    public static final Uri COMMENT_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_COMMENT_ACTIONS);
    public static final Uri MESSAGE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_MESSAGE_ACTIONS);
    public static final Uri READ_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_READ_ACTIONS);
    public static final Uri SAVE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_SAVE_ACTIONS);
    public static final Uri VOTE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_VOTE_ACTIONS);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_THINGS = 1;
    private static final int MATCH_MESSAGES = 2;
    private static final int MATCH_SUBREDDITS = 3;
    private static final int MATCH_COMMENT_ACTIONS = 4;
    private static final int MATCH_MESSAGE_ACTIONS = 5;
    private static final int MATCH_READ_ACTIONS = 6;
    private static final int MATCH_SAVE_ACTIONS = 7;
    private static final int MATCH_VOTE_ACTIONS = 8;
    static {
        MATCHER.addURI(AUTHORITY, PATH_THINGS, MATCH_THINGS);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGES, MATCH_MESSAGES);
        MATCHER.addURI(AUTHORITY, PATH_SUBREDDITS, MATCH_SUBREDDITS);
        MATCHER.addURI(AUTHORITY, PATH_COMMENT_ACTIONS, MATCH_COMMENT_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGE_ACTIONS, MATCH_MESSAGE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_READ_ACTIONS, MATCH_READ_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_SAVE_ACTIONS, MATCH_SAVE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_VOTE_ACTIONS, MATCH_VOTE_ACTIONS);
    }

    static final String PARAM_LISTING_GET = "getListing";
    static final String PARAM_LISTING_TYPE = "listingType";

    static final String PARAM_SESSION_ID = "sessionId";
    static final String PARAM_ACCOUNT = "account";
    static final String PARAM_SUBREDDIT = "subreddit";
    static final String PARAM_QUERY = "query";
    static final String PARAM_PROFILE_USER = "profileUser";
    static final String PARAM_FILTER = "filter";
    static final String PARAM_MORE = "more";
    static final String PARAM_MARK = "mark";
    static final String PARAM_THING_ID = "thingId";
    static final String PARAM_LINK_ID = "linkId";
    static final String PARAM_JOIN = "join";
    static final String PARAM_NOTIFY_ACCOUNTS = "notifyAccounts";
    static final String PARAM_NOTIFY_THINGS = "notifyThings";
    static final String PARAM_NOTIFY_MESSAGES = "notifyMessages";

    private static final String JOINED_THING_TABLE = Things.TABLE_NAME
            // Join with pending saves to fake that the save happened.
            + " LEFT OUTER JOIN (SELECT "
            + SaveActions.COLUMN_ACCOUNT + ","
            + SaveActions.COLUMN_THING_ID + ","
            + SaveActions.COLUMN_ACTION + " AS " + SharedColumns.COLUMN_SAVE
            + " FROM " + SaveActions.TABLE_NAME + ") USING ("
            + SaveActions.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")"

            // Join with pending votes to fake that the vote happened.
            + " LEFT OUTER JOIN (SELECT "
            + VoteActions.COLUMN_ACCOUNT + ", "
            + VoteActions.COLUMN_THING_ID + ", "
            + VoteActions.COLUMN_ACTION + " AS " + SharedColumns.COLUMN_VOTE
            + " FROM " + VoteActions.TABLE_NAME + ") USING ("
            + VoteActions.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")";

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

    public static final Uri subredditUri(long sessionId, String accountName, String subreddit,
            int filter, String more) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_SUBREDDIT_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_SUBREDDIT, subreddit);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        }
        return b.build();
    }

    public static final Uri commentsUri(long sessionId, String accountName, String thingId,
            String linkId) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_COMMENT_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_THING_ID, thingId);
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        if (!TextUtils.isEmpty(linkId)) {
            b.appendQueryParameter(PARAM_LINK_ID, linkId);
        }
        return b.build();
    }

    public static final Uri profileUri(long sessionId, String accountName, String profileUser,
            int filter, String more) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_USER_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_PROFILE_USER, profileUser);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        }
        return b.build();
    }

    public static final Uri searchUri(long sessionId, String accountName, String subreddit,
            String query) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_SEARCH_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_QUERY, query);
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(subreddit)) {
            b.appendQueryParameter(PARAM_SUBREDDIT, subreddit);
        }
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        return b.build();
    }

    public static final Uri subredditSearchUri(long sessionId, String accountName, String query) {
        Uri.Builder b = SUBREDDITS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_REDDIT_SEARCH_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_QUERY, query);
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        return b.build();
    }

    public static final Uri messageUri(long sessionId, String accountName, int filter, String more) {
        Uri.Builder b = MESSAGES_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_MESSAGE_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));

        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        } else if (filter == FilterAdapter.MESSAGE_INBOX
                || filter == FilterAdapter.MESSAGE_UNREAD) {
            b.appendQueryParameter(PARAM_MARK, TRUE);
            b.appendQueryParameter(PARAM_NOTIFY_ACCOUNTS, TRUE);
        }
        return b.build();
    }

    public static final Uri messageThreadUri(long sessionId, String accountName, String thingId) {
        Uri.Builder b = MESSAGES_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Listing.TYPE_MESSAGE_THREAD_LISTING));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_THING_ID, thingId);
        if (sessionId != -1) {
            b.appendQueryParameter(PARAM_SESSION_ID, toString(sessionId));
        }
        return b.build();
    }

    public ThingProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        switch (MATCHER.match(uri)) {
            case MATCH_THINGS:
                if (uri.getBooleanQueryParameter(PARAM_JOIN, false)) {
                    return JOINED_THING_TABLE;
                } else {
                    return Things.TABLE_NAME;
                }

            case MATCH_MESSAGES:
                if (uri.getBooleanQueryParameter(PARAM_JOIN, false)) {
                    return JOINED_MESSAGES_TABLE;
                } else {
                    return Messages.TABLE_NAME;
                }

            case MATCH_SUBREDDITS:
                return SubredditResults.TABLE_NAME;

            case MATCH_COMMENT_ACTIONS:
                return CommentActions.TABLE_NAME;

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
    protected Cursor innerQuery(Uri uri, SQLiteDatabase db, String table, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        if (uri.getBooleanQueryParameter(PARAM_LISTING_GET, false)) {
            return handleListingGet(uri, db, table, projection, selection, selectionArgs, sortOrder);
        }
        return super.innerQuery(uri, db, table, projection, selection, selectionArgs, sortOrder);
    }

    private Cursor handleListingGet(Uri uri, SQLiteDatabase db, String table, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        try {
            Context context = getContext();
            long sessionId = getLongParameter(uri, PARAM_SESSION_ID, -1);
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String subreddit = uri.getQueryParameter(PARAM_SUBREDDIT);
            String query = uri.getQueryParameter(PARAM_QUERY);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);
            int filter = getIntParameter(uri, PARAM_FILTER, 0);
            String more = uri.getQueryParameter(PARAM_MORE);

            String cookie = AccountUtils.getCookie(context, accountName);
            if (cookie == null && AccountUtils.isAccount(accountName)) {
                return null;
            }

            int listingType = Integer.parseInt(uri.getQueryParameter(PARAM_LISTING_TYPE));
            Listing listing = null;
            switch (listingType) {
                case Listing.TYPE_MESSAGE_THREAD_LISTING:
                    listing = MessageListing.newThreadInstance(helper,
                            accountName, thingId, cookie);
                    break;

                case Listing.TYPE_MESSAGE_LISTING:
                    boolean mark = uri.getBooleanQueryParameter(PARAM_MARK, false);
                    listing = MessageListing.newInstance(helper,
                            accountName, filter, more, mark, cookie);
                    break;

                case Listing.TYPE_SUBREDDIT_LISTING:
                    listing = ThingListing.newSubredditInstance(context, helper,
                            accountName, subreddit, filter, more, cookie);
                    break;

                case Listing.TYPE_USER_LISTING:
                    String profileUser = uri.getQueryParameter(PARAM_PROFILE_USER);
                    listing = ThingListing.newUserInstance(context, helper,
                            accountName, profileUser, filter, more, cookie);
                    break;

                case Listing.TYPE_COMMENT_LISTING:
                    String linkId = uri.getQueryParameter(PARAM_LINK_ID);
                    listing = CommentListing.newInstance(context, helper,
                            accountName, thingId, linkId, cookie);
                    break;

                case Listing.TYPE_SEARCH_LISTING:
                    listing = ThingListing.newSearchInstance(context, helper,
                            accountName, subreddit, query, cookie);
                    break;

                case Listing.TYPE_REDDIT_SEARCH_LISTING:
                    listing = SubredditResultListing.newInstance(
                            accountName, query, cookie);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            sessionId = getListingSession(listing, db, sessionId);
            selection = appendSelection(selection, SharedColumns.SELECT_BY_SESSION_ID);
            selectionArgs = appendSelectionArg(selectionArgs, Long.toString(sessionId));
            Cursor c = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);

            Bundle extras = new Bundle(2);
            extras.putLong(EXTRA_SESSION_ID, sessionId);
            listing.addCursorExtras(extras);
            listing.performExtraWork(getContext());
            return new CursorExtrasWrapper(c, extras);

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void notifyChange(Uri uri) {
        super.notifyChange(uri);
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_ACCOUNTS, false)) {
            getContext().getContentResolver().notifyChange(AccountProvider.ACCOUNTS_URI, null);
        }
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_THINGS, false)) {
            getContext().getContentResolver().notifyChange(THINGS_URI, null);
        }
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_MESSAGES, false)) {
            getContext().getContentResolver().notifyChange(MESSAGES_URI, null);
        }
    }
}
