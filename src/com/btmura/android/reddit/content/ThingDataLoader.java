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
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Array;

public class ThingDataLoader extends BaseAsyncTaskLoader<ThingData> {

    private static final String TAG = "ThingDataLoader";

    public static class ThingData {
        public final ThingBundle parent;
        public final ThingBundle child;

        private final String accountName;
        private final Cursor saveActionCursor;

        private ThingData(String accountName, ThingBundle parent, ThingBundle child,
                Cursor saveActionCursor) {
            this.accountName = accountName;
            this.parent = parent;
            this.child = child;
            this.saveActionCursor = saveActionCursor;
        }

        public boolean isParentSaveable() {
            return AccountUtils.isAccount(accountName)
                    && parent.getKind() == Kinds.KIND_LINK;
        }

        public boolean isParentSaved() {
            // If no local save actions are pending, then rely on server info.
            if (saveActionCursor == null || !saveActionCursor.moveToFirst()) {
                return parent.isSaved();
            }

            // We have a local pending action so use that to indicate if it's read.
            return saveActionCursor.getInt(INDEX_ACTION) == SaveActions.ACTION_SAVE;
        }

        public void recycle() {
            if (saveActionCursor != null && !saveActionCursor.isClosed()) {
                saveActionCursor.close();
            }
        }
    }

    private static final String[] PROJECTION = {
            SaveActions._ID,
            SaveActions.COLUMN_ACTION,
    };

    private static final int INDEX_ACTION = 1;

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
            ThingBundle parentBundle;
            ThingBundle childBundle;

            switch (thingBundle.getType()) {
                case ThingBundle.TYPE_LINK:
                case ThingBundle.TYPE_MESSAGE:
                    parentBundle = thingBundle;
                    childBundle = null;
                    break;

                case ThingBundle.TYPE_COMMENT:
                    String cookie = getCookie();
                    Formatter formatter = getFormatter();
                    parentBundle = RedditApi.getInfo(getContext(), thingBundle.getLinkId(),
                            cookie, formatter);
                    childBundle = thingBundle;
                    break;

                case ThingBundle.TYPE_LINK_REFERENCE:
                    cookie = getCookie();
                    formatter = getFormatter();
                    parentBundle = RedditApi.getInfo(getContext(), thingBundle.getThingId(),
                            cookie, formatter);
                    childBundle = null;
                    break;

                case ThingBundle.TYPE_COMMENT_REFERENCE:
                    cookie = getCookie();
                    formatter = getFormatter();
                    parentBundle = RedditApi.getInfo(getContext(), thingBundle.getLinkId(),
                            cookie, formatter);
                    childBundle = RedditApi.getInfo(getContext(), thingBundle.getThingId(),
                            cookie, formatter);
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            Cursor saveActionCursor = null;
            if (AccountUtils.isAccount(accountName)) {
                ContentResolver cr = getContext().getContentResolver();
                saveActionCursor = cr.query(ThingProvider.SAVE_ACTIONS_URI, PROJECTION,
                        SaveActions.SELECT_BY_ACCOUNT_AND_THING_ID,
                        Array.of(accountName, parentBundle.getThingId()),
                        null);
                if (saveActionCursor != null) {
                    saveActionCursor.getCount();
                    saveActionCursor.registerContentObserver(observer);
                }
            }

            return new ThingData(accountName, parentBundle, childBundle, saveActionCursor);
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
}
