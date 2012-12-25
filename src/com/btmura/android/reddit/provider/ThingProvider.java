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
import android.util.Log;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Saves;
import com.btmura.android.reddit.database.SessionIds;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;

public class ThingProvider extends SessionProvider {

    public static final String TAG = "ThingProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";
    static final String AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final String PATH_SUBREDDIT = "r";


    private static final String PATH_THINGS = "things";
    private static final String PATH_COMMENTS = "comments";
    private static final String PATH_VOTES = "votes";
    private static final String PATH_SAVES = "saves";

    public static final Uri THINGS_URI = Uri.parse(AUTHORITY_URI + PATH_THINGS);
    public static final Uri COMMENTS_URI = Uri.parse(AUTHORITY_URI + PATH_COMMENTS);
    public static final Uri VOTES_URI = Uri.parse(AUTHORITY_URI + PATH_VOTES);
    public static final Uri SAVES_URI = Uri.parse(AUTHORITY_URI + PATH_SAVES);

    public static final String PARAM_FETCH = "fetch";
    public static final String PARAM_COMMENT_REPLY = "commentReply";
    public static final String PARAM_COMMENT_DELETE = "commentDelete";

    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_SUBREDDIT = "subreddit";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_PROFILE_USER = "profileUser";
    public static final String PARAM_MESSAGE_USER = "messageUser";
    public static final String PARAM_FILTER = "filter";
    public static final String PARAM_MORE = "more";

    public static final String PARAM_PARENT_THING_ID = "parentThingId";
    public static final String PARAM_THING_ID = "thingId";
    public static final String PARAM_LINK_ID = "linkId";

    public static final String PARAM_JOIN = "joinVotes";
    public static final String PARAM_NOTIFY_OTHERS = "notifyOthers";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDIT = 1;
    private static final int MATCH_COMMENTS = 2;

    private static final int MATCH_THINGS = 3;
    private static final int MATCH_VOTES = 5;
    private static final int MATCH_SAVES = 6;
    static {
        MATCHER.addURI(AUTHORITY, PATH_SUBREDDIT + "/*", MATCH_SUBREDDIT);
        MATCHER.addURI(AUTHORITY, PATH_SUBREDDIT + "/*/" + PATH_COMMENTS + "/*", MATCH_COMMENTS);


        MATCHER.addURI(AUTHORITY, PATH_THINGS, MATCH_THINGS);
        MATCHER.addURI(AUTHORITY, PATH_COMMENTS, MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY, PATH_VOTES, MATCH_VOTES);
        MATCHER.addURI(AUTHORITY, PATH_SAVES, MATCH_SAVES);
    }

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

    public ThingProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_THINGS:
                if (uri.getBooleanQueryParameter(PARAM_JOIN, false)) {
                    return JOINED_THING_TABLE;
                } else {
                    return Things.TABLE_NAME;
                }

            case MATCH_COMMENTS:
                return Comments.TABLE_NAME;

            case MATCH_VOTES:
                return Votes.TABLE_NAME;

            case MATCH_SAVES:
                return Saves.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    @Override
    protected Selection processUri(Uri uri, SQLiteDatabase db, ContentValues values,
            String selection, String[] selectionArgs) {
        if (uri.getBooleanQueryParameter(PARAM_FETCH, false)) {
            return handleFetch(uri, db, selection, selectionArgs);
        } else if (uri.getBooleanQueryParameter(PARAM_COMMENT_REPLY, false)) {
            handleReply(uri, db, values);
        } else if (uri.getBooleanQueryParameter(PARAM_COMMENT_DELETE, false)) {
            handleDelete(uri, db);
        }
        return null;
    }

    private Selection handleFetch(Uri uri, SQLiteDatabase db, String selection, String[] selectionArgs) {
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String subredditName = uri.getQueryParameter(PARAM_SUBREDDIT);
            String query = uri.getQueryParameter(PARAM_QUERY);
            String profileUser = uri.getQueryParameter(PARAM_PROFILE_USER);
            String messageUser = uri.getQueryParameter(PARAM_MESSAGE_USER);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);
            String linkId = uri.getQueryParameter(PARAM_LINK_ID);
            String filterParameter = uri.getQueryParameter(PARAM_FILTER);
            int filter = filterParameter != null ? Integer.parseInt(filterParameter) : 0;
            String more = uri.getQueryParameter(PARAM_MORE);

            String cookie = AccountUtils.getCookie(context, accountName);
            if (cookie == null) {
                return null;
            }

            Listing listing = null;
            switch (MATCHER.match(uri)) {
                case MATCH_SUBREDDIT:
                    listing = new ThingListing(context, accountName, subredditName, query, profileUser, messageUser, filter, more, cookie);
                    break;

                case MATCH_COMMENTS:
                    listing = new CommentListing(context, helper, accountName, thingId, linkId, cookie);
                    break;
            }

            long sessionId = getListingSession(listing, db, Things.TABLE_NAME);

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

    private void handleReply(Uri uri, SQLiteDatabase db, ContentValues values) {
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

    private void handleDelete(Uri uri, SQLiteDatabase db) {
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

    @Override
    protected void notifyChange(Uri uri) {
        super.notifyChange(uri);
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(ThingProvider.THINGS_URI, null);
            cr.notifyChange(ThingProvider.COMMENTS_URI, null);
        }
    }
}
