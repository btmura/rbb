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
import java.util.List;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.backup.BackupManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.SubredditSearches;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class Provider extends SessionProvider {

    public static final String TAG = "Provider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider";

    public static final String ACCOUNT_PARAM = "account";
    public static final String FILTER_PARAM = "filter";
    public static final String MORE_PARAM = "more";
    public static final String QUERY_PARAM = "query";
    public static final String SESSION_ID_PARAM = "sessionId";
    public static final String SUBREDDIT_PARAM = "subreddit";
    public static final String SYNC_PARAM = "sync";

    public static final String PARAM_SYNC = "sync";
    public static final String PARAM_REPLY = "reply";
    public static final String PARAM_DELETE = "delete";

    public static final String PARAM_ACCOUNT_NAME = "accountName";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_PARENT_THING_ID = "parentThingId";
    public static final String PARAM_THING_ID = "thingId";

    public static final String PARAM_NOTIFY_OTHERS = "notifyOthers";

    static final String BASE_URI = "content://" + AUTHORITY;
    public static final Uri SUBREDDITS_URI = Uri.parse(BASE_URI + "/subreddits");
    public static final Uri SUBREDDIT_SEARCHES_URI = Uri.parse(BASE_URI + "/subreddits/search");
    public static final Uri THING_SESSIONS_URI = Uri.parse(BASE_URI + "/things/sessions");
    public static final Uri COMMENT_ACTIONS_URI = Uri.parse(BASE_URI + "/comments/actions");
    public static final Uri COMMENT_SESSIONS_URI = Uri.parse(BASE_URI + "/comments/sessions");
    public static final Uri VOTE_ACTIONS_URI = Uri.parse(BASE_URI + "/votes/actions");

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDIT_DIR = 1;
    private static final int MATCH_SUBREDDIT_ITEM = 2;
    private static final int MATCH_SUBREDDIT_SEARCH_DIR = 3;
    private static final int MATCH_THING_SESSION_DIR = 4;
    private static final int MATCH_THING_SESSION_ITEM = 5;
    private static final int MATCH_COMMENT_ACTION_DIR = 6;
    private static final int MATCH_COMMENT_ACTION_ITEM = 7;
    private static final int MATCH_COMMENT_SESSION_DIR = 8;
    private static final int MATCH_COMMENT_SESSION_ITEM = 9;
    private static final int MATCH_VOTE_ACTION_DIR = 10;
    private static final int MATCH_VOTE_ACTION_ITEM = 11;
    static {
        MATCHER.addURI(AUTHORITY, "subreddits", MATCH_SUBREDDIT_DIR);
        MATCHER.addURI(AUTHORITY, "subreddits/#", MATCH_SUBREDDIT_ITEM);
        MATCHER.addURI(AUTHORITY, "subreddits/search", MATCH_SUBREDDIT_SEARCH_DIR);
        MATCHER.addURI(AUTHORITY, "things/sessions", MATCH_THING_SESSION_DIR);
        MATCHER.addURI(AUTHORITY, "things/sessions/#", MATCH_THING_SESSION_ITEM);
        MATCHER.addURI(AUTHORITY, "comments/actions", MATCH_COMMENT_ACTION_DIR);
        MATCHER.addURI(AUTHORITY, "comments/actions/#", MATCH_COMMENT_ACTION_ITEM);
        MATCHER.addURI(AUTHORITY, "comments/sessions", MATCH_COMMENT_SESSION_DIR);
        MATCHER.addURI(AUTHORITY, "comments/sessions/#", MATCH_COMMENT_SESSION_ITEM);
        MATCHER.addURI(AUTHORITY, "votes/actions", MATCH_VOTE_ACTION_DIR);
        MATCHER.addURI(AUTHORITY, "votes/actions/#", MATCH_VOTE_ACTION_ITEM);
    }

    static final String BASE_MIME_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/" + AUTHORITY + ".";
    static final String BASE_MIME_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/" + AUTHORITY + ".";

    private static final String THINGS_WITH_VOTES = Things.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")";

    private static final String COMMENTS_WITH_VOTES = Comments.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Comments.COLUMN_THING_ID + ")";

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        processQueryUri(uri);

        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDIT_ITEM:
            case MATCH_THING_SESSION_ITEM:
            case MATCH_COMMENT_ACTION_ITEM:
            case MATCH_COMMENT_SESSION_ITEM:
            case MATCH_VOTE_ACTION_ITEM:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        String tableName = getTableName(uri, true);
        Cursor c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = -1;
        boolean syncToNetwork = false;
        String tableName = getTableName(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            id = db.insert(tableName, null, values);
            syncToNetwork = processInsertUri(uri, db, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "insert tableName: " + tableName + " id: " + id
                    + " syncToNetwork: " + syncToNetwork);
        }
        if (id != -1) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null, syncToNetwork);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(Provider.THING_SESSIONS_URI, null);
                cr.notifyChange(Provider.COMMENT_SESSIONS_URI, null);
            }
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDIT_ITEM:
            case MATCH_THING_SESSION_ITEM:
            case MATCH_COMMENT_ACTION_ITEM:
            case MATCH_COMMENT_SESSION_ITEM:
            case MATCH_VOTE_ACTION_ITEM:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        String tableName = getTableName(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(tableName, values, selection, selectionArgs);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "update tableName: " + tableName + " count: " + count);
        }
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();

            // Sync updated votes back to the server.
            // TODO: Figure out whether this will conflict with inserts.
            cr.notifyChange(uri, null, false);

            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(Provider.THING_SESSIONS_URI, null);
                cr.notifyChange(Provider.COMMENT_SESSIONS_URI, null);
            }
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDIT_ITEM:
            case MATCH_THING_SESSION_ITEM:
            case MATCH_COMMENT_ACTION_ITEM:
            case MATCH_COMMENT_SESSION_ITEM:
            case MATCH_VOTE_ACTION_ITEM:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        int count = 0;
        boolean syncToNetwork = false;
        String tableName = getTableName(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = db.delete(tableName, selection, selectionArgs);
            syncToNetwork = processDeleteUri(uri, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "delete tableName: " + tableName + " count: " + count
                    + " syncToNetwork: " + syncToNetwork);
        }
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null, syncToNetwork);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(Provider.THING_SESSIONS_URI, null);
                cr.notifyChange(Provider.COMMENT_SESSIONS_URI, null);
            }
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDIT_DIR:
            case MATCH_SUBREDDIT_SEARCH_DIR:
                return BASE_MIME_TYPE_DIR + "subreddits";
            case MATCH_SUBREDDIT_ITEM:
                return BASE_MIME_TYPE_ITEM + "subreddits";

            case MATCH_THING_SESSION_DIR:
                return BASE_MIME_TYPE_DIR + "things";
            case MATCH_THING_SESSION_ITEM:
                return BASE_MIME_TYPE_ITEM + "things";

            case MATCH_COMMENT_ACTION_DIR:
                return BASE_MIME_TYPE_DIR + "commentActions";
            case MATCH_COMMENT_ACTION_ITEM:
                return BASE_MIME_TYPE_ITEM + "commentActions";

            case MATCH_COMMENT_SESSION_DIR:
                return BASE_MIME_TYPE_DIR + "comments";
            case MATCH_COMMENT_SESSION_ITEM:
                return BASE_MIME_TYPE_ITEM + "comments";

            case MATCH_VOTE_ACTION_DIR:
                return BASE_MIME_TYPE_DIR + "votes";
            case MATCH_VOTE_ACTION_ITEM:
                return BASE_MIME_TYPE_ITEM + "votes";

            default:
                return null;
        }
    }

    public static void addSubredditInBackground(final Context context, final String accountName,
            final String subredditName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(2);
                ops.add(ContentProviderOperation.newDelete(SUBREDDITS_URI)
                        .withSelection(Subreddits.SELECTION_ACCOUNT_AND_NAME, new String[] {
                                accountName,
                                subredditName,
                        })
                        .build());
                ops.add(ContentProviderOperation.newInsert(SUBREDDITS_URI)
                        .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                        .withValue(Subreddits.COLUMN_NAME, subredditName)
                        .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_INSERTING)
                        .build());

                ContentResolver cr = context.getContentResolver();
                try {
                    ContentProviderResult[] r = cr.applyBatch(AUTHORITY, ops);
                    startSyncOperation(context, accountName, r[1].uri);
                } catch (RemoteException e) {
                    Log.e(TAG, "addInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "addInBackground", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, 1);
                scheduleBackup(context, accountName);
            }
        }.execute();
    }

    public static void deleteSubredditInBackground(final Context context, final String accountName,
            final long[] ids) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int count = ids.length;
                Uri[] uris = new Uri[count];
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        count);
                for (int i = 0; i < count; i++) {
                    uris[i] = ContentUris.withAppendedId(SUBREDDITS_URI, ids[i]);
                    ops.add(ContentProviderOperation.newUpdate(uris[i])
                            .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_DELETING)
                            .build());
                }

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(AUTHORITY, ops);
                    startSyncOperation(context, accountName, uris);
                } catch (RemoteException e) {
                    Log.e(TAG, "deleteInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "deleteInBackground", e);
                }
                return count;
            }

            @Override
            protected void onPostExecute(Integer count) {
                showChangeToast(context, -count);
                scheduleBackup(context, accountName);
            }
        }.execute();
    }

    public static void combineSubredditsInBackground(final Context context,
            final List<String> subredditNames, final long[] ids) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int size = subredditNames.size();
                StringBuilder combinedName = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    combinedName.append(subredditNames.get(i));
                    if (i + 1 < size) {
                        combinedName.append("+");
                    }
                }

                int count = ids.length;
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        count + 1);
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newDelete(SUBREDDITS_URI)
                            .withSelection(ID_SELECTION, new String[] {Long.toString(ids[i])})
                            .build());
                }
                ops.add(ContentProviderOperation.newInsert(SUBREDDITS_URI)
                        .withValue(Subreddits.COLUMN_NAME, combinedName.toString())
                        .build());

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, "combineInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "combineInBackground", e);
                }
                return size;
            }

            @Override
            protected void onPostExecute(Integer deleted) {
                showChangeToast(context, 1);
                scheduleBackup(context, null);
            }
        }.execute();
    }

    public static void splitSubredditsInBackground(final Context context,
            final String subredditName,
            final long id) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                String[] parts = subredditName.split("\\+");
                int numParts = parts.length;

                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        numParts + 1);
                for (int i = 0; i < numParts; i++) {
                    ops.add(ContentProviderOperation.newInsert(SUBREDDITS_URI)
                            .withValue(Subreddits.COLUMN_NAME, parts[i])
                            .build());
                }

                ops.add(ContentProviderOperation.newDelete(SUBREDDITS_URI)
                        .withSelection(ID_SELECTION, new String[] {Long.toString(id)})
                        .build());

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, "splitInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "splitInBackground", e);
                }
                return numParts;
            }

            @Override
            protected void onPostExecute(Integer added) {
                showChangeToast(context, added);
                scheduleBackup(context, null);
            }
        }.execute();
    }

    public static void addMultipleSubredditsInBackground(final Context context,
            final ContentValues[] values) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver cr = context.getContentResolver();
                cr.bulkInsert(SUBREDDITS_URI, values);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, values.length);
                scheduleBackup(context, null);
            }
        }.execute();
    }

    private static void showChangeToast(Context context, int count) {
        // TODO: Use Resources#getQuantityString
        int resId;
        if (count == 1) {
            resId = R.string.subreddit_one_added;
        } else if (count == -1) {
            resId = R.string.subreddit_one_deleted;
        } else if (count >= 0) {
            resId = R.string.subreddit_x_added;
        } else {
            resId = R.string.subreddit_x_deleted;
        }
        Toast.makeText(context, context.getString(resId, Math.abs(count)), Toast.LENGTH_SHORT)
                .show();
    }

    private static void scheduleBackup(Context context, String accountName) {
        if (TextUtils.isEmpty(accountName)) {
            new BackupManager(context).dataChanged();
        }
    }

    private static void startSyncOperation(Context context, String accountName, Uri... uris) {
        if (!TextUtils.isEmpty(accountName)) {
            for (int i = 0; i < uris.length; i++) {
                Intent intent = new Intent(context, SyncOperationService.class);
                intent.setData(uris[i]);
                context.startService(intent);
            }
        }
    }

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void insertCommentPlaceholderInBackground(Context context,
            final String accountName,
            final String body, final int nesting, final String parentThingId, final int sequence,
            final String sessionId, final long sessionCreationTime, final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                Uri uri = COMMENT_SESSIONS_URI.buildUpon()
                        .appendQueryParameter(PARAM_REPLY, Boolean.toString(true))
                        .appendQueryParameter(PARAM_PARENT_THING_ID, parentThingId)
                        .appendQueryParameter(PARAM_THING_ID, thingId)
                        .build();

                ContentValues v = new ContentValues(8);
                v.put(Comments.COLUMN_ACCOUNT, accountName);
                v.put(Comments.COLUMN_AUTHOR, accountName);
                v.put(Comments.COLUMN_BODY, body);
                v.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
                v.put(Comments.COLUMN_NESTING, nesting);
                v.put(Comments.COLUMN_SEQUENCE, sequence);
                v.put(Comments.COLUMN_SESSION_ID, sessionId);
                v.put(Comments.COLUMN_SESSION_TIMESTAMP, sessionCreationTime);

                ContentResolver cr = appContext.getContentResolver();
                cr.insert(uri, v);
            }
        });
    }

    public static void deleteCommentInBackground(Context context, final String accountName,
            final String parentThingId, final long[] ids, final String[] thingIds) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                int count = ids.length;
                for (int i = 0; i < count; i++) {
                    Uri uri = COMMENT_SESSIONS_URI.buildUpon()
                            .appendQueryParameter(PARAM_DELETE, Boolean.toString(true))
                            .appendQueryParameter(PARAM_ACCOUNT_NAME, accountName)
                            .appendQueryParameter(PARAM_PARENT_THING_ID, parentThingId)
                            .appendQueryParameter(PARAM_THING_ID, thingIds[i])
                            .build();
                    ops.add(ContentProviderOperation.newDelete(uri)
                            .withSelection(ID_SELECTION, Array.of(ids[i]))
                            .build());
                }

                ContentResolver cr = appContext.getContentResolver();
                try {
                    cr.applyBatch(AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }

    public static void voteInBackground(final Context context, final String accountName,
            final String thingId, final int likes) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                String[] selectionArgs = Array.of(accountName, thingId);

                ContentValues values = new ContentValues(3);
                values.put(Votes.COLUMN_ACCOUNT, accountName);
                values.put(Votes.COLUMN_THING_ID, thingId);
                values.put(Votes.COLUMN_VOTE, likes);

                Uri uri = VOTE_ACTIONS_URI.buildUpon()
                        .appendQueryParameter(PARAM_NOTIFY_OTHERS,
                                Boolean.toString(true))
                        .build();
                int count = cr.update(uri, values, Votes.SELECTION_BY_ACCOUNT_AND_THING_ID,
                        selectionArgs);
                if (count == 0) {
                    cr.insert(uri, values);
                }
            }
        });
    }

    private String getTableName(Uri uri, boolean joinVotes) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDIT_DIR:
            case MATCH_SUBREDDIT_ITEM:
                return Subreddits.TABLE_NAME;

            case MATCH_SUBREDDIT_SEARCH_DIR:
                return SubredditSearches.TABLE_NAME;

            case MATCH_THING_SESSION_DIR:
                return joinVotes ? THINGS_WITH_VOTES : Things.TABLE_NAME;

            case MATCH_COMMENT_ACTION_DIR:
                return CommentActions.TABLE_NAME;

            case MATCH_COMMENT_SESSION_DIR:
                return joinVotes ? COMMENTS_WITH_VOTES : Comments.TABLE_NAME;

            case MATCH_VOTE_ACTION_DIR:
            case MATCH_VOTE_ACTION_ITEM:
                return Votes.TABLE_NAME;

            default:
                throw new IllegalStateException();
        }
    }

    private void processQueryUri(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDIT_SEARCH_DIR:
                processSubredditSearchQuery(uri);
                break;

            case MATCH_THING_SESSION_DIR:
                processThingQuery(uri);
                break;

            case MATCH_COMMENT_SESSION_DIR:
                processCommentQuery(uri);
                break;
        }
    }

    private void processSubredditSearchQuery(Uri uri) {
        if (!uri.getBooleanQueryParameter(SYNC_PARAM, false)) {
            return;
        }

        try {
            // Determine the cutoff first to avoid deleting synced data.
            long timestampCutoff = getSessionTimestampCutoff();
            long sessionTimestamp = System.currentTimeMillis();

            String accountName = uri.getQueryParameter(ACCOUNT_PARAM);
            String sessionId = uri.getQueryParameter(SESSION_ID_PARAM);
            String query = uri.getQueryParameter(QUERY_PARAM);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            SubredditSearchListing listing = SubredditSearchListing.get(context, accountName,
                    sessionId, sessionTimestamp, query, cookie);

            long cleaned;
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old results that can't be possibly viewed anymore.
                cleaned = db.delete(SubredditSearches.TABLE_NAME,
                        SubredditSearches.SELECTION_BEFORE_TIMESTAMP,
                        Array.of(timestampCutoff));
                InsertHelper insertHelper = new InsertHelper(db, SubredditSearches.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "cleaned: " + cleaned);
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void processThingQuery(Uri uri) {
        if (!uri.getBooleanQueryParameter(SYNC_PARAM, false)) {
            return;
        }
        try {
            // Determine the cutoff first to avoid deleting synced data when
            // appending more data.
            long timestampCutoff = getSessionTimestampCutoff();

            // Appended data may have a different timestamp but that is OK.
            long sessionTimestamp = System.currentTimeMillis();

            String accountName = uri.getQueryParameter(ACCOUNT_PARAM);
            String sessionId = uri.getQueryParameter(SESSION_ID_PARAM);
            String subredditName = uri.getQueryParameter(SUBREDDIT_PARAM);
            int filter = Integer.parseInt(uri.getQueryParameter(FILTER_PARAM));
            String more = uri.getQueryParameter(MORE_PARAM);
            String query = uri.getQueryParameter(QUERY_PARAM);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            ThingListing listing = ThingListing.get(context, accountName, sessionId,
                    sessionTimestamp, subredditName, filter, more, query, cookie);

            long cleaned;
            long t1 = System.currentTimeMillis();
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old things that can't possibly be viewed anymore.
                cleaned = db.delete(Things.TABLE_NAME, Things.SELECTION_BEFORE_TIMESTAMP,
                        Array.of(timestampCutoff));

                // Delete the loading more element before appending more.
                db.delete(Things.TABLE_NAME, Things.SELECTION_BY_SESSION_ID_AND_MORE,
                        Array.of(sessionId));

                InsertHelper insertHelper = new InsertHelper(db, Things.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                long t2 = System.currentTimeMillis();
                Log.d(TAG, "sync network: " + listing.networkTimeMs
                        + " parse: " + listing.parseTimeMs
                        + " db: " + (t2 - t1)
                        + " cleaned: " + cleaned);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void processCommentQuery(Uri uri) {
        if (!uri.getBooleanQueryParameter(PARAM_SYNC, false)) {
            return;
        }
        try {
            // Determine the cutoff first to avoid deleting synced data.
            long timestampCutoff = getSessionTimestampCutoff();
            long sessionTimestamp = System.currentTimeMillis();

            String sessionId = uri.getQueryParameter(PARAM_SESSION_ID);
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT_NAME);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            CommentListing listing = CommentListing.get(context, helper, accountName, sessionId,
                    sessionTimestamp, thingId, cookie);

            long cleaned;
            long t1 = System.currentTimeMillis();
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old comments that can't possibly be viewed anymore.
                cleaned = db.delete(Comments.TABLE_NAME, Comments.SELECTION_BEFORE_TIMESTAMP,
                        Array.of(timestampCutoff));

                InsertHelper insertHelper = new InsertHelper(db, Comments.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                long t2 = System.currentTimeMillis();
                Log.d(TAG, "sync network: " + listing.networkTimeMs
                        + " parse: " + listing.parseTimeMs
                        + " db: " + (t2 - t1)
                        + " cleaned: " + cleaned);
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private boolean processInsertUri(Uri uri, SQLiteDatabase db, ContentValues values) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_COMMENT_ACTION_DIR:
                return processCommentActionInsert(uri, db, values);

            case MATCH_VOTE_ACTION_DIR:
                return true;
        }
        return false;
    }

    private boolean processCommentActionInsert(Uri uri, SQLiteDatabase db, ContentValues values) {
        if (!uri.getBooleanQueryParameter(PARAM_REPLY, false)) {
            return false;
        }

        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(5);
        v.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_INSERT);
        v.put(CommentActions.COLUMN_ACCOUNT, values.getAsString(Comments.COLUMN_ACCOUNT));
        v.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(CommentActions.COLUMN_THING_ID, thingId);
        v.put(CommentActions.COLUMN_TEXT, values.getAsString(Comments.COLUMN_BODY));
        db.insert(CommentActions.TABLE_NAME, null, values);
        return true;
    }

    private boolean processDeleteUri(Uri uri, SQLiteDatabase db) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_COMMENT_ACTION_DIR:
                return processCommentActionDelete(uri, db);
        }
        return false;
    }

    private boolean processCommentActionDelete(Uri uri, SQLiteDatabase db) {
        if (!uri.getBooleanQueryParameter(PARAM_DELETE, false)) {
            return false;
        }

        String accountName = uri.getQueryParameter(PARAM_ACCOUNT_NAME);
        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(4);
        v.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_DELETE);
        v.put(CommentActions.COLUMN_ACCOUNT, accountName);
        v.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(CommentActions.COLUMN_THING_ID, thingId);
        db.insert(CommentActions.TABLE_NAME, null, v);
        return true;
    }

}
