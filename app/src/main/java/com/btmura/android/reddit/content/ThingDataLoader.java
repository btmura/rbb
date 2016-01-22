/*
 * Copyright (C) 2013 Brian Muramatsu
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

package com.btmura.android.reddit.content;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.content.ThingDataLoader.ThingData;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.Array;

import java.io.IOException;

public class ThingDataLoader extends BaseAsyncTaskLoader<ThingData> {

  private static final String TAG = "ThingDataLoader";

  private static final String[] PROJECTION = {
      SaveActions._ID,
      SaveActions.COLUMN_ACTION,
  };

  private static final int INDEX_SAVE_ACTION = 1;

  private final String accountName;
  private final ThingBundle thingBundle;
  private final ForceLoadContentObserver observer =
      new ForceLoadContentObserver();

  private boolean initialized;
  private ThingBundle parent;
  private ThingBundle child;
  private Cursor saveActionCursor;
  private boolean savedState;

  public ThingDataLoader(
      Context ctx,
      String accountName,
      ThingBundle thingBundle) {
    super(ctx.getApplicationContext());
    this.accountName = accountName;
    this.thingBundle = thingBundle;
  }

  @Override
  public ThingData loadInBackground() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "loadInBackground");
    }

    try {
      if (!initialized) {
        resolveThingBundles();
        savedState = parent.isSaved();
        initialized = true;
      }
      reloadSaveActionCursor();
      return new ThingData(accountName, parent, child, savedState);
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
      return null;
    } catch (OperationCanceledException e) {
      Log.e(TAG, e.getMessage(), e);
      return null;
    } catch (AuthenticatorException e) {
      Log.e(TAG, e.getMessage(), e);
      return null;
    }
  }

  private void resolveThingBundles() throws OperationCanceledException,
      AuthenticatorException,
      IOException {
    switch (thingBundle.getType()) {
      case ThingBundle.TYPE_LINK:
      case ThingBundle.TYPE_MESSAGE:
        parent = thingBundle;
        child = null;
        break;

      case ThingBundle.TYPE_COMMENT:
        parent = RedditApi.getThingInfo(getContext(), accountName,
            thingBundle.getLinkId(), newFormatter());
        child = thingBundle;
        break;

      case ThingBundle.TYPE_LINK_REFERENCE:
        parent = RedditApi.getThingInfo(getContext(), accountName,
            thingBundle.getThingId(), newFormatter());
        child = null;
        break;

      case ThingBundle.TYPE_COMMENT_REFERENCE:
        MarkdownFormatter formatter = newFormatter();
        parent = RedditApi.getThingInfo(getContext(), accountName,
            thingBundle.getLinkId(), formatter);
        child = RedditApi.getThingInfo(getContext(), accountName,
            thingBundle.getThingId(), formatter);
        break;

      default:
        throw new IllegalArgumentException();
    }
  }

  private MarkdownFormatter newFormatter() {
    return new MarkdownFormatter();
  }

  private void reloadSaveActionCursor() {
    closeSaveActionCursor();
    saveActionCursor = getSaveActionCursor();
    if (saveActionCursor != null && saveActionCursor.moveToLast()) {
      savedState =
          saveActionCursor.getInt(INDEX_SAVE_ACTION) == SaveActions.ACTION_SAVE;
    }
  }

  private void closeSaveActionCursor() {
    if (saveActionCursor != null) {
      saveActionCursor.unregisterContentObserver(observer);
      saveActionCursor.close();
      saveActionCursor = null;
    }
  }

  private Cursor getSaveActionCursor() {
    if (AccountUtils.isAccount(accountName)) {
      ContentResolver cr = getContext().getContentResolver();
      Cursor cursor = cr.query(ThingProvider.SAVE_ACTIONS_URI,
          PROJECTION,
          SaveActions.SELECT_BY_ACCOUNT_AND_THING_ID,
          Array.of(accountName, parent.getThingId()),
          SaveActions.SORT_BY_ID);
      if (cursor != null) {
        cursor.getCount();
        cursor.registerContentObserver(observer);
      }
      return cursor;
    }
    return null;
  }

  @Override
  protected void onCleanData(ThingData data) {
    closeSaveActionCursor();
  }

  public static class ThingData {

    public final ThingBundle parent;
    public final ThingBundle child;

    private final String accountName;
    private final boolean isParentSaved;

    private ThingData(
        String accountName,
        ThingBundle parent,
        ThingBundle child,
        boolean isParentSaved) {
      this.accountName = accountName;
      this.parent = parent;
      this.child = child;
      this.isParentSaved = isParentSaved;
    }

    public boolean isParentSaveable() {
      return AccountUtils.isAccount(accountName)
          && parent.getKind() == Kinds.KIND_LINK;
    }

    public boolean isParentSaved() {
      return isParentSaved;
    }
  }
}
