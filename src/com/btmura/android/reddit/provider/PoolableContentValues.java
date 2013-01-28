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

import android.content.ContentValues;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.util.pool.Pool;
import com.btmura.android.reddit.util.pool.Poolable;
import com.btmura.android.reddit.util.pool.PoolableManager;
import com.btmura.android.reddit.util.pool.Pools;

class PoolableContentValues implements Poolable<PoolableContentValues> {

    public static final String TAG = "PoolableContentValues";

    private static Pool<PoolableContentValues> POOL = Pools.synchronizedPool(Pools.finitePool(
            new PoolableManager<PoolableContentValues>() {
                public PoolableContentValues newInstance() {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "newInstance");
                    }
                    return new PoolableContentValues(20);
                }

                public void onAcquired(PoolableContentValues element) {
                }

                public void onReleased(PoolableContentValues element) {
                    element.values.clear();
                }
            }, 360));

    private final ContentValues values;

    private PoolableContentValues nextPoolable;
    private boolean isPooled;

    public static PoolableContentValues acquire() {
        return POOL.acquire();
    }

    public void release() {
        POOL.release(this);
    }

    PoolableContentValues(int size) {
        this.values = new ContentValues(size);
    }

    public PoolableContentValues getNextPoolable() {
        return nextPoolable;
    }

    public void setNextPoolable(PoolableContentValues nextPoolable) {
        this.nextPoolable = nextPoolable;
    }

    public boolean isPooled() {
        return isPooled;
    }

    public void setPooled(boolean isPooled) {
        this.isPooled = isPooled;
    }

    public void put(String key, String value) {
        values.put(key, value);
    }

    public void put(String key, Integer value) {
        values.put(key, value);
    }

    public void put(String key, Long value) {
        values.put(key, value);
    }

    public void put(String key, Boolean value) {
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getAsString(String key) {
        return values.getAsString(key);
    }

    public ContentValues getContentValues() {
        return values;
    }
}
