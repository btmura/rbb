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
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;

public class MessageProvider extends SessionProvider {

    public static final String TAG = "MessageProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.messages";

    static final String PATH_INBOX = "message/inbox";
    static final String PATH_SENT = "message/sent";
    static final String PATH_MESSAGES = "message/messages";
    static final String PATH_ACTIONS = "message/actions";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri INBOX_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_INBOX);
    public static final Uri SENT_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SENT);
    public static final Uri MESSAGES_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_MESSAGES);
    public static final Uri ACTIONS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_ACTIONS);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_INBOX = 0;
    private static final int MATCH_SENT = 1;
    private static final int MATCH_MESSAGE = 2;
    private static final int MATCH_ACTIONS = 3;
    static {
        MATCHER.addURI(AUTHORITY, PATH_INBOX, MATCH_INBOX);
        MATCHER.addURI(AUTHORITY, PATH_SENT, MATCH_SENT);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGES + "/*", MATCH_MESSAGE);
        MATCHER.addURI(AUTHORITY, PATH_ACTIONS, MATCH_ACTIONS);
    }

    // TODO: Remove this parameter because it can be enabled all the time.
    public static final String PARAM_FETCH = "fetch";
    public static final String PARAM_REPLY = "reply";

    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_THING_ID = "thingId";

    public MessageProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        switch (MATCHER.match(uri)) {
            case MATCH_ACTIONS:
                return MessageActions.TABLE_NAME;

            default:
                return Messages.TABLE_NAME;
        }
    }

    @Override
    protected Selection processUri(Uri uri, SQLiteDatabase db, ContentValues values,
            String selection, String[] selectionArgs) {
        if (uri.getBooleanQueryParameter(PARAM_FETCH, false)) {
            return handleFetch(uri, db, selection, selectionArgs);
        } else if (uri.getBooleanQueryParameter(PARAM_REPLY, false)) {
            return handleReply(uri, db, values);
        }
        return null;
    }

    /** Fetches a session containing data for a message thread. */
    private Selection handleFetch(Uri uri, SQLiteDatabase db, String selection,
            String[] selectionArgs) {
        try {
            // Collect the ingredients we need to get the listing.
            Context context = getContext();
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String thingId = uri.getLastPathSegment();

            // Get the account cookie or bail out.
            String cookie = AccountUtils.getCookie(context, accountName);
            if (cookie == null) {
                return null;
            }

            Listing listing;
            switch (MATCHER.match(uri)) {
                case MATCH_INBOX:
                    listing = MessageListing.newInboxInstance(accountName, cookie);
                    break;

                case MATCH_SENT:
                    listing = MessageListing.newSentInstance(accountName, cookie);
                    break;

                case MATCH_MESSAGE:
                    listing = MessageListing.newThreadInstance(accountName, thingId, cookie,
                            helper);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            // Get the session containing the data for this listing.
            long sessionId = getListingSession(listing, db, true);

            // Answer this query by modifying the selection arguments.
            Selection newSelection = new Selection();
            newSelection.selection = appendSelection(selection, Messages.SELECT_BY_SESSION_ID);
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

    private Selection handleReply(Uri uri, SQLiteDatabase db, ContentValues values) {
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

        // This doesn't affect any selection so return null.
        return null;
    }
}
