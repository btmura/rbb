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
 * Provider is a collection of static methods that do user actions which
 * correspond to multiple content provider operations.
 */
public class Provider {

  public static void addSubredditsAsync(
      Context ctx,
      final String accountName,
      final String... subreddits) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        SubredditProvider.addSubreddits(appCtx, accountName, subreddits);
      }
    });
  }

  public static void removeSubredditsAsync(
      Context ctx,
      final String accountName,
      final String... subreddits) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        SubredditProvider.removeSubreddits(appCtx, accountName, subreddits);
      }
    });
  }

  public static void expandCommentAsync(
      Context ctx,
      final long id,
      final long sessionId) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.expandComment(appCtx, id, sessionId);
      }
    });
  }

  public static void collapseCommentAsync(
      Context ctx,
      final long id,
      final long[] childIds) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.collapseComment(appCtx, id, childIds);
      }
    });
  }

  public static void insertCommentAsync(
      Context ctx,
      final String accountName,
      final String body,
      final String parentThingId,
      final String thingId) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.insertComment(appCtx, accountName, body, parentThingId,
            thingId);
      }
    });
  }

  public static void editCommentAsync(
      Context ctx,
      final String accountName,
      final String body,
      final String parentThingId,
      final String thingId) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.editComment(appCtx, accountName, body, parentThingId,
            thingId);
      }
    });
  }

  public static void deleteCommentAsync(
      Context ctx,
      final String accountName,
      final boolean[] hasChildren,
      final long[] ids,
      final String parentThingId,
      final String[] thingIds) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.deleteComment(appCtx, accountName, hasChildren, ids,
            parentThingId, thingIds);
      }
    });
  }

  public static void insertMessageAsync(
      Context ctx,
      final String accountName,
      final String body,
      final String parentThingId,
      final String thingId) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.insertMessage(appCtx, accountName, body, parentThingId,
            thingId);
      }
    });
  }

  public static void readMessageAsync(
      final Context ctx,
      final String accountName,
      final String thingId,
      final boolean read) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        int action = read ? ReadActions.ACTION_READ : ReadActions.ACTION_UNREAD;
        ThingProvider.readMessage(appCtx, accountName, action, thingId);
      }
    });
  }

  public static void hideAsync(
      Context ctx,
      final String accountName,
      final String thingId,
      final ThingBundle thingBundle,
      final boolean hide) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        int action = hide ? HideActions.ACTION_HIDE : HideActions.ACTION_UNHIDE;
        ThingProvider.hide(appCtx, accountName, action, thingId, thingBundle);
      }
    });
  }

  public static void saveAsync(
      final Context ctx,
      final String accountName,
      final String thingId,
      final ThingBundle thingBundle,
      final boolean save) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        int action = save ? SaveActions.ACTION_SAVE : SaveActions.ACTION_UNSAVE;
        ThingProvider.save(appCtx, accountName, action, thingId, thingBundle);
      }
    });
  }

  public static void voteAsync(
      Context ctx,
      final String accountName,
      final int action,
      final String thingId,
      final ThingBundle thingBundle) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        ThingProvider.vote(appCtx, accountName, action, thingId, thingBundle);
      }
    });
  }

  public static void clearMailIndicatorAsync(Context ctx) {
    final Context appCtx = ctx.getApplicationContext();
    AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        AccountProvider.clearMailIndicator(appCtx);
      }
    });
  }

  static Bundle call(
      Context ctx,
      Uri uri,
      String method,
      String arg,
      Bundle extras) {
    return ctx.getApplicationContext().getContentResolver()
        .call(uri, method, arg, extras);
  }

  static void scheduleBackup(Context ctx, String accountName) {
    if (!AccountUtils.isAccount(accountName)) {
      new BackupManager(ctx).dataChanged();
    }
  }
}
