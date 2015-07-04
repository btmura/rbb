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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.util.Array;

abstract class AbstractSessionLoader extends CursorLoader {

  private static final String TAG = "AbstractSessionLoader";

  protected static final String NO_SORT = null;
  protected static final String NO_MORE = null;
  protected static final int NO_COUNT = -1;

  private String more;
  private int count;
  private Bundle sessionData;

  AbstractSessionLoader(
      Context ctx,
      Uri uri,
      String[] projection,
      String selection,
      String sortOrder,
      String more,
      int count,
      Bundle sessionData) {
    super(ctx, uri, projection, selection, null, sortOrder);
    this.more = more;
    this.count = count;
    this.sessionData = sessionData;
  }

  @Override
  public Cursor loadInBackground() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "loadInBackground");
    }

    // 1. Use the new session data if non-null.
    // 2. If this is not a request for more data and it failed,
    //    then show no data but an error.
    // 3. If this is a request for more data and it failed,
    //    then just show existing data.
    Bundle newSessionData = getSession(sessionData, more, count);
    if (newSessionData != null) {
      sessionData = newSessionData;
    } else if (TextUtils.isEmpty(more)) {
      sessionData = null;
    }

    // Reset the request for more data after attempting to get it.
    more = null;

    return newCursor();
  }

  protected abstract Bundle getSession(
      Bundle sessionData,
      String more,
      int count);

  private Cursor newCursor() {
    if (sessionData != null) {
      long sessionId = CursorExtras.getSessionId(sessionData);
      setSelectionArgs(Array.of(sessionId));
      Cursor c = super.loadInBackground();
      return c != null ? new CursorExtrasWrapper(c, sessionData) : null;
    }
    return null;
  }
}
