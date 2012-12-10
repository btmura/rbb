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
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Sessions;

public class MessageProvider extends SessionProvider {

    public static final String TAG = "MessageProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.messages";

    static final String PATH_INBOX = "message/inbox";
    static final String PATH_SENT = "message/sent";
    static final String PATH_MESSAGES = "message/messages";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri INBOX_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_INBOX);
    public static final Uri SENT_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SENT);
    public static final Uri MESSAGES_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_MESSAGES);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_INBOX = 0;
    private static final int MATCH_SENT = 1;
    private static final int MATCH_MESSAGE = 2;
    static {
        MATCHER.addURI(AUTHORITY, PATH_INBOX, MATCH_INBOX);
        MATCHER.addURI(AUTHORITY, PATH_SENT, MATCH_SENT);
        MATCHER.addURI(AUTHORITY, PATH_MESSAGES + "/*", MATCH_MESSAGE);
    }

    public static final String PARAM_ACCOUNT = "account";

    public static final String PARAM_FETCH = "fetch";

    public MessageProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        return Messages.TABLE_NAME;
    }

    @Override
    protected void processUri(Uri uri, SQLiteDatabase db, ContentValues values,
            String[] selectionArgs) {
        if (uri.getBooleanQueryParameter(PARAM_FETCH, false)) {
            handleFetch(uri, db, selectionArgs);
        }
    }

    private void handleFetch(Uri uri, SQLiteDatabase db, String[] selectionArgs) {
        try {
            // Collect the ingredients we need to get the listing.
            Context context = getContext();
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String thingId = uri.getLastPathSegment();
            String cookie = AccountUtils.getCookie(context, accountName);
            if (cookie == null) {
                return;
            }

            // Create the session and modify the selection args to use it.
            MessageThreadListing listing = new MessageThreadListing(accountName, thingId, cookie);
            long sessionId = getListingSession(Sessions.TYPE_MESSAGE_THREAD_LISTING, thingId,
                    listing, db, Messages.TABLE_NAME, Messages.COLUMN_SESSION_ID);
            selectionArgs[selectionArgs.length - 1] = Long.toString(sessionId);

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
