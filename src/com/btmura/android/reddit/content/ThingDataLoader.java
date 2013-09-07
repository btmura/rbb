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

import java.io.IOException;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.content.ThingDataLoader.ThingData;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Array;

public class ThingDataLoader extends BaseAsyncTaskLoader<ThingData> {

    private static final String TAG = "ThingDataLoader";

    private final ForceLoadContentObserver observer = new ForceLoadContentObserver();

    private final String accountName;
    private final ThingBundle thingBundle;

    public ThingDataLoader(Context context, String accountName, ThingBundle thingBundle) {
        super(context.getApplicationContext());
        this.accountName = accountName;
        this.thingBundle = thingBundle;
    }

    @Override
    public ThingData loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }

        try {
            ThingBundle parent;
            ThingBundle child;

            switch (thingBundle.getType()) {
                case ThingBundle.TYPE_LINK:
                case ThingBundle.TYPE_MESSAGE:
                    parent = thingBundle;
                    child = null;
                    break;

                case ThingBundle.TYPE_COMMENT:
                    parent = RedditApi.getInfo(getContext(),
                            thingBundle.getLinkId(),
                            getCookie(),
                            getFormatter());
                    child = thingBundle;
                    break;

                case ThingBundle.TYPE_LINK_REFERENCE:
                    parent = RedditApi.getInfo(getContext(),
                            thingBundle.getThingId(),
                            getCookie(),
                            getFormatter());
                    child = null;
                    break;

                case ThingBundle.TYPE_COMMENT_REFERENCE:
                    String cookie = getCookie();
                    Formatter formatter = getFormatter();
                    parent = RedditApi.getInfo(getContext(),
                            thingBundle.getLinkId(),
                            cookie,
                            formatter);
                    child = RedditApi.getInfo(getContext(),
                            thingBundle.getThingId(),
                            cookie,
                            formatter);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            return ThingData.newInstance(getContext(), accountName, parent, child, observer);
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

    private String getCookie()
            throws OperationCanceledException, AuthenticatorException, IOException {
        return AccountUtils.getCookie(getContext(), accountName);
    }

    private Formatter getFormatter() {
        return new Formatter();
    }

    @Override
    protected void onNewDataDelivered(ThingData oldData, ThingData newData) {
        if (oldData != null && oldData != newData) {
            oldData.recycle();
        }
    }

    @Override
    protected void onCleanData(ThingData data) {
        if (data != null) {
            data.recycle();
        }
    }

    public static class ThingData {

        private static final String[] PROJECTION = {
                Things._ID,
                Things.COLUMN_SAVED,
                SharedColumns.COLUMN_HIDE_ACTION,
                SharedColumns.COLUMN_SAVE_ACTION,
                SharedColumns.COLUMN_VOTE_ACTION,
        };

        private static final int INDEX_SAVED = 1;
        private static final int INDEX_SAVE_ACTION = 3;

        public final ThingBundle parent;
        public final ThingBundle child;

        private final String accountName;
        private final Cursor cursor;

        static ThingData newInstance(Context context,
                String accountName,
                ThingBundle parent,
                ThingBundle child,
                ContentObserver observer) {
            Cursor cursor = getCursor(context, accountName, parent.getThingId(), observer);
            return new ThingData(accountName, parent, child, cursor);
        }

        private static Cursor getCursor(Context context,
                String accountName,
                String thingId,
                ContentObserver observer) {
            if (AccountUtils.isAccount(accountName)) {
                ContentResolver cr = context.getContentResolver();
                Cursor cursor = cr.query(ThingProvider.THINGS_WITH_ACTIONS_URI,
                        PROJECTION,
                        Things.SELECT_BY_ACCOUNT_AND_THING_ID,
                        Array.of(accountName, thingId),
                        null);
                if (cursor != null) {
                    cursor.getCount();
                    cursor.registerContentObserver(observer);
                }
                return cursor;
            }
            return null;
        }

        private ThingData(String accountName,
                ThingBundle parent,
                ThingBundle child,
                Cursor cursor) {
            this.accountName = accountName;
            this.parent = parent;
            this.child = child;
            this.cursor = cursor;
        }

        public boolean isParentSaveable() {
            return AccountUtils.isAccount(accountName) && parent.getKind() == Kinds.KIND_LINK;
        }

        public boolean isParentSaved() {
            // If there is no cursor info available then reply on the bundle.
            if (cursor == null || !cursor.moveToFirst()) {
                return parent.isSaved();
            }

            // If no local save actions are pending, then rely on the server info.
            if (cursor.isNull(INDEX_SAVE_ACTION)) {
                return cursor.getInt(INDEX_SAVED) != 0;
            }

            // We have a local pending action so use that to indicate if it's read.
            return cursor.getInt(INDEX_SAVE_ACTION) == SaveActions.ACTION_SAVE;
        }

        public void recycle() {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

}
