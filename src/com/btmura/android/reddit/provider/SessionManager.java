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

package com.btmura.android.reddit.provider;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Sessions;

class SessionManager {

    private static final String TAG = "SessionManager";

    private static final int CLEAN_CYCLE = 20;

    private final SparseArray<AtomicInteger> counters =
            new SparseArray<AtomicInteger>(Sessions.NUM_TYPES);

    public void cleanIfNecessary(Context context, int sessionType) {
        if (needsCleaning(sessionType)) {
            SessionCleanerService.startService(context, sessionType);
        }
    }

    private boolean needsCleaning(int sessionType) {
        AtomicInteger counter = getCounterLocked(sessionType);
        int value = counter.getAndIncrement();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "sessionType: " + sessionType + " counter: " + value);
        }
        return value % CLEAN_CYCLE == 0;
    }

    private synchronized AtomicInteger getCounterLocked(int sessionType) {
        AtomicInteger counter = counters.get(sessionType);
        if (counter == null) {
            counter = new AtomicInteger();
            counters.put(sessionType, counter);
        }
        return counter;
    }
}
