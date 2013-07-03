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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.ThingDataLoader.ThingData;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingDataLoader extends BaseAsyncTaskLoader<ThingData> {

    private static final String TAG = "ThingDataLoader";

    public static class ThingData extends ThingBundle {
        private final String accountName;
        private final Cursor saveActionCursor;

        private ThingData(String accountName, ThingBundle thingBundle, Cursor saveActionCursor) {
            super(thingBundle);
            this.accountName = accountName;
            this.saveActionCursor = saveActionCursor;
        }

        public boolean isSaveable() {
            return AccountUtils.isAccount(accountName) && getKind() == Kinds.KIND_LINK;
        }

        public boolean isSaved() {
            // If no local save actions are pending, then rely on server info.
            if (saveActionCursor == null || !saveActionCursor.moveToFirst()) {
                return super.isSaved();
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

        String thingId = thingBundle.getLinkId();
        if (TextUtils.isEmpty(thingId)) {
            thingId = thingBundle.getThingId();
        }

        Cursor saveActionCursor = null;
        if (AccountUtils.isAccount(accountName)) {
            ContentResolver cr = getContext().getContentResolver();
            saveActionCursor = cr.query(ThingProvider.SAVE_ACTIONS_URI, PROJECTION,
                    SaveActions.SELECT_BY_ACCOUNT_AND_THING_ID, Array.of(accountName, thingId),
                    null);
            if (saveActionCursor != null) {
                saveActionCursor.getCount();
                saveActionCursor.registerContentObserver(observer);
            }
        }
        return new ThingData(accountName, thingBundle, saveActionCursor);
    }

    @Override
    protected void onNewDataDelivered(ThingData oldData, ThingData newData) {
        if (oldData != null && oldData != newData) {
            oldData.recycle();
        }
    }

    @Override
    public void onCanceled(ThingData data) {
        if (data != null) {
            data.recycle();
        }
    }
}
