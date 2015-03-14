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

import android.app.backup.BackupManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;

/**
 * Provider is a collection of static methods that do user actions which correspond to multiple
 * content provider operations.
 */
public class Provider {

    public static void addSubredditsAsync(Context context,
            final String accountName,
            final String... subreddits) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                SubredditProvider.addSubreddits(appContext, accountName, subreddits);
            }
        });
    }

    public static void removeSubredditsAsync(Context context,
            final String accountName,
            final String... subreddits) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                SubredditProvider.removeSubreddits(appContext, accountName, subreddits);
            }
        });
    }

    public static void expandCommentAsync(Context context, final long id, final long sessionId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.expandComment(appContext, id, sessionId);
            }
        });
    }

    public static void collapseCommentAsync(Context context, final long id,
            final long[] childIds) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.collapseComment(appContext, id, childIds);
            }
        });
    }

    public static void insertCommentAsync(Context context,
            final String accountName,
            final String body,
            final String parentThingId,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.insertComment(appContext, accountName, body, parentThingId, thingId);
            }
        });
    }

    public static void editCommentAsync(Context context,
            final String accountName,
            final String body,
            final String parentThingId,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.editComment(appContext, accountName, body, parentThingId, thingId);
            }
        });
    }

    public static void deleteCommentAsync(Context context,
            final String accountName,
            final boolean[] hasChildren,
            final long[] ids,
            final String parentThingId,
            final String[] thingIds) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.deleteComment(appContext,
                        accountName,
                        hasChildren,
                        ids,
                        parentThingId,
                        thingIds);
            }
        });
    }

    public static void insertMessageAsync(Context context,
            final String accountName,
            final String body,
            final String parentThingId,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.insertMessage(appContext, accountName, body, parentThingId, thingId);
            }
        });
    }

    public static void markMessagesReadAsync(Context context, final String accountName) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                AccountProvider.markMessagesRead(appContext, accountName);
            }
        });
    }

    public static void readMessageAsync(final Context context,
            final String accountName,
            final String thingId,
            final boolean read) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                int action = read ? ReadActions.ACTION_READ : ReadActions.ACTION_UNREAD;
                ThingProvider.readMessage(appContext, accountName, action, thingId);
            }
        });
    }

    public static void hideAsync(Context context,
            final String accountName,
            final String thingId,
            final ThingBundle thingBundle,
            final boolean hide) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                int action = hide ? HideActions.ACTION_HIDE : HideActions.ACTION_UNHIDE;
                ThingProvider.hide(appContext, accountName, action, thingId, thingBundle);
            }
        });
    }

    public static void saveAsync(final Context context,
            final String accountName,
            final String thingId,
            final ThingBundle thingBundle,
            final boolean save) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                int action = save ? SaveActions.ACTION_SAVE : SaveActions.ACTION_UNSAVE;
                ThingProvider.save(appContext, accountName, action, thingId, thingBundle);
            }
        });
    }

    public static void voteAsync(Context context,
            final String accountName,
            final int action,
            final String thingId,
            final ThingBundle thingBundle) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.vote(appContext, accountName, action, thingId, thingBundle);
            }
        });
    }

    static Bundle call(Context context, Uri uri, String method, String arg, Bundle extras) {
        return context.getApplicationContext().getContentResolver().call(uri, method, arg, extras);
    }

    static void scheduleBackup(Context context, String accountName) {
        if (!AccountUtils.isAccount(accountName)) {
            new BackupManager(context).dataChanged();
        }
    }
}
