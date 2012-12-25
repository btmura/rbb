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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Saves;
import com.btmura.android.reddit.database.SessionIds;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;

/**
 * URI MATCHING PATTERNS:
 *
 * <pre>
 * /things/
 *
 * /actions/comments - DONE
 * /actions/messages - DONE
 * /actions/saves - DONE
 * /actions/votes - DONE
 * </pre>
 */
public class ThingProvider extends SessionProvider {

    public static final String TAG = "ThingProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";
    static final String AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final String PATH_THINGS = "things";
    private static final String PATH_COMMENT_ACTIONS = "actions/comments";
    private static final String PATH_MESSAGE_ACTIONS = "actions/messages";
    private static final String PATH_SAVE_ACTIONS = "actions/saves";
    private static final String PATH_VOTE_ACTIONS = "actions/votes";

    public static final Uri THINGS_URI = Uri.parse(AUTHORITY_URI + PATH_THINGS);
    public static final Uri COMMENT_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_COMMENT_ACTIONS);
    public static final Uri MESSAGE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_MESSAGE_ACTIONS);
    public static final Uri SAVE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_SAVE_ACTIONS);
    public static final Uri VOTE_ACTIONS_URI = Uri.parse(AUTHORITY_URI + PATH_VOTE_ACTIONS);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_THINGS = 1;
    private static final int MATCH_COMMENT_ACTIONS = 2;
    private static final int MATCH_MESSAGE_ACTIONS = 3;
    private static final int MATCH_SAVE_ACTIONS = 4;
    private static final int MATCH_VOTE_ACTIONS = 5;
    static {
        MATCHER.addURI(AUTHORITY, PATH_THINGS, MATCH_THINGS);
        MATCHER.addURI(AUTHORITY, PATH_COMMENT_ACTIONS, MATCH_COMMENT_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGE_ACTIONS, MATCH_MESSAGE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_SAVE_ACTIONS, MATCH_SAVE_ACTIONS);
        MATCHER.addURI(AUTHORITY, PATH_VOTE_ACTIONS, MATCH_VOTE_ACTIONS);
    }

    static final String PARAM_LISTING_GET = "getListing";
    static final String PARAM_LISTING_TYPE = "listingType";
    static final String PARAM_LISTING_REFRESH = "refresh";

    public static final String PARAM_COMMENT_REPLY = "commentReply";
    public static final String PARAM_COMMENT_DELETE = "commentDelete";

    public static final String PARAM_MESSAGE_REPLY = "messageReply";

    static final String PARAM_ACCOUNT = "account";
    static final String PARAM_SUBREDDIT = "subreddit";
    static final String PARAM_QUERY = "query";
    static final String PARAM_PROFILE_USER = "profileUser";
    static final String PARAM_FILTER = "filter";
    static final String PARAM_MORE = "more";
    static final String PARAM_PARENT_THING_ID = "parentThingId";
    static final String PARAM_THING_ID = "thingId";
    static final String PARAM_LINK_ID = "linkId";
    static final String PARAM_JOIN = "join";
    static final String PARAM_NOTIFY_OTHERS = "notifyOthers";

    private static final String JOINED_THING_TABLE = Things.TABLE_NAME
            // Join with pending saves to fake that the save happened.
            + " LEFT OUTER JOIN (SELECT "
            + Saves.COLUMN_ACCOUNT + ", "
            + Saves.COLUMN_THING_ID + ", "
            + Saves.COLUMN_ACTION
            + " FROM " + Saves.TABLE_NAME + ") USING ("
            + Saves.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")"

            // Join with pending votes to fake that the vote happened.
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")";

    public static final Uri subredditUri(String accountName, String subreddit, int filter,
            String more, boolean refresh) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_SUBREDDIT_LISTING));
        b.appendQueryParameter(PARAM_LISTING_REFRESH, toString(refresh));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_SUBREDDIT, subreddit);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        }
        return b.build();
    }

    public static final Uri commentsUri(String accountName, String thingId, String linkId,
            boolean refresh) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_COMMENT_LISTING));
        b.appendQueryParameter(PARAM_LISTING_REFRESH, toString(refresh));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_THING_ID, thingId);
        if (!TextUtils.isEmpty(linkId)) {
            b.appendQueryParameter(PARAM_LINK_ID, linkId);
        }
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        return b.build();
    }

    public static final Uri profileUri(String accountName, String profileUser, int filter,
            String more, boolean refresh) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_USER_LISTING));
        b.appendQueryParameter(PARAM_LISTING_REFRESH, toString(refresh));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_PROFILE_USER, profileUser);
        b.appendQueryParameter(PARAM_FILTER, toString(filter));
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(PARAM_MORE, more);
        }
        return b.build();
    }

    public static final Uri searchUri(String accountName, String query, boolean refresh) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_SEARCH_LISTING));
        b.appendQueryParameter(PARAM_LISTING_REFRESH, toString(refresh));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_QUERY, query);
        b.appendQueryParameter(PARAM_JOIN, TRUE);
        return b.build();
    }

    public static final Uri subredditSearchUri(String accountName, String query, boolean refresh) {
        Uri.Builder b = THINGS_URI.buildUpon();
        b.appendQueryParameter(PARAM_LISTING_GET, TRUE);
        b.appendQueryParameter(PARAM_LISTING_TYPE, toString(Sessions.TYPE_REDDIT_SEARCH_LISTING));
        b.appendQueryParameter(PARAM_LISTING_REFRESH, toString(refresh));
        b.appendQueryParameter(PARAM_ACCOUNT, accountName);
        b.appendQueryParameter(PARAM_QUERY, query);
        return b.build();
    }

    public ThingProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        int match = MATCHER.match(uri);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getTable match: " + match);
        }
        switch (match) {
            case MATCH_THINGS:
                if (uri.getBooleanQueryParameter(PARAM_JOIN, false)) {
                    return JOINED_THING_TABLE;
                } else {
                    return Things.TABLE_NAME;
                }

            case MATCH_COMMENT_ACTIONS:
                return Comments.TABLE_NAME;

            case MATCH_MESSAGE_ACTIONS:
                return MessageActions.TABLE_NAME;

            case MATCH_SAVE_ACTIONS:
                return Saves.TABLE_NAME;

            case MATCH_VOTE_ACTIONS:
                return Votes.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    @Override
    protected Selection processUri(Uri uri, SQLiteDatabase db, ContentValues values,
            String selection, String[] selectionArgs) {
        if (uri.getBooleanQueryParameter(PARAM_LISTING_GET, false)) {
            return handleListingGet(uri, db, selection, selectionArgs);
        } else if (uri.getBooleanQueryParameter(PARAM_COMMENT_REPLY, false)) {
            handleCommentReply(uri, db, values);
        } else if (uri.getBooleanQueryParameter(PARAM_COMMENT_DELETE, false)) {
            handleCommentDelete(uri, db);
        } else if (uri.getBooleanQueryParameter(PARAM_MESSAGE_REPLY, false)) {
            handleMessageReply(uri, db, values);
        }

        return null;
    }

    private Selection handleListingGet(Uri uri, SQLiteDatabase db, String selection,
            String[] selectionArgs) {
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String subreddit = uri.getQueryParameter(PARAM_SUBREDDIT);
            String query = uri.getQueryParameter(PARAM_QUERY);
            String profileUser = uri.getQueryParameter(PARAM_PROFILE_USER);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);
            String linkId = uri.getQueryParameter(PARAM_LINK_ID);
            String filterParameter = uri.getQueryParameter(PARAM_FILTER);
            int filter = filterParameter != null ? Integer.parseInt(filterParameter) : 0;
            String more = uri.getQueryParameter(PARAM_MORE);

            String cookie = AccountUtils.getCookie(context, accountName);
            if (cookie == null && AccountUtils.isAccount(accountName)) {
                return null;
            }

            int listingType = Integer.parseInt(uri.getQueryParameter(PARAM_LISTING_TYPE));
            Listing listing = null;
            switch (listingType) {
                case Sessions.TYPE_SUBREDDIT_LISTING:
                    listing = ThingListing.newSubredditInstance(context, accountName, subreddit,
                            filter, more, cookie);
                    break;

                case Sessions.TYPE_USER_LISTING:
                    listing = ThingListing.newUserInstance(context, accountName, profileUser,
                            filter, more, cookie);
                    break;

                case Sessions.TYPE_COMMENT_LISTING:
                    listing = CommentListing.newInstance(context, helper, accountName, thingId,
                            linkId, cookie);
                    break;

                case Sessions.TYPE_SEARCH_LISTING:
                    listing = ThingListing.newSearchInstance(context, accountName, query, cookie);
                    break;

                case Sessions.TYPE_REDDIT_SEARCH_LISTING:
                    listing = SubredditSearchListing.newInstance(accountName, query, cookie);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            boolean refresh = uri.getBooleanQueryParameter(PARAM_LISTING_REFRESH, false);
            long sessionId = getListingSession(listing, db, refresh);

            Selection newSelection = new Selection();
            newSelection.selection = appendSelection(selection, SessionIds.SELECT_BY_SESSION_ID);
            newSelection.selectionArgs = appendSelectionArg(selectionArgs, Long.toString(sessionId));
            return newSelection;

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private void handleCommentReply(Uri uri, SQLiteDatabase db, ContentValues values) {
        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(5);
        v.put(Comments.COLUMN_ACTION, Comments.ACTION_INSERT);
        v.put(Comments.COLUMN_ACCOUNT, values.getAsString(Things.COLUMN_ACCOUNT));
        v.put(Comments.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(Comments.COLUMN_THING_ID, thingId);
        v.put(Comments.COLUMN_TEXT, values.getAsString(Things.COLUMN_BODY));
        db.insert(Comments.TABLE_NAME, null, v);
    }

    private void handleCommentDelete(Uri uri, SQLiteDatabase db) {
        String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(4);
        v.put(Comments.COLUMN_ACTION, Comments.ACTION_DELETE);
        v.put(Comments.COLUMN_ACCOUNT, accountName);
        v.put(Comments.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(Comments.COLUMN_THING_ID, thingId);
        db.insert(Comments.TABLE_NAME, null, v);
    }

    private void handleMessageReply(Uri uri, SQLiteDatabase db, ContentValues values) {
        // Get the thing ID of the thing we are replying to.
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        // Insert an action to sync the reply back to the network eventually.
        ContentValues v = new ContentValues(5);
        v.put(MessageActions.COLUMN_ACCOUNT, values.getAsString(Messages.COLUMN_ACCOUNT));
        v.put(MessageActions.COLUMN_ACTION, MessageActions.ACTION_INSERT);
        v.put(MessageActions.COLUMN_PARENT_THING_ID, uri.getLastPathSegment());
        v.put(MessageActions.COLUMN_THING_ID, thingId);
        v.put(MessageActions.COLUMN_TEXT, values.getAsString(Messages.COLUMN_BODY));
        db.insert(MessageActions.TABLE_NAME, null, v);
    }

    @Override
    protected void notifyChange(Uri uri) {
        super.notifyChange(uri);
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(ThingProvider.THINGS_URI, null);
        }
    }
}
