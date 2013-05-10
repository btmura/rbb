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
import android.support.v4.content.AsyncTaskLoader;

/**
 * {@link AsyncTaskLoader} that starts loading automatically if there is no existing data. It
 * provides methods for hooking into when new data is delivered, when old data needs to be cleaned,
 * and when listeners should be registered and unregistered.
 */
abstract class BaseAsyncTaskLoader<T> extends AsyncTaskLoader<T> {

    private T data;
    private boolean listening;

    BaseAsyncTaskLoader(Context context) {
        super(context);
    }

    /** Set the current data. Use it to seed the loader's data to avoid launching the async task. */
    protected final void setData(T data) {
        this.data = data;
    }

    /** Get the current data. */
    protected final T getData() {
        return data;
    }

    /**
     * Callback for when old data is being discarded and new data has been delivered. The old and
     * new data is provided to check if they are the same or not.
     */
    protected void onNewDataDelivered(T oldData, T newData) {
    }

    /** Callback for when the current data is being discarded when the loader is cancelled. */
    protected void onCleanData(T data) {
    }

    /** Callback for registering listeners. */
    protected void onStartListening() {
    }

    /** Callback for unregistering listeners. */
    protected void onStopListening() {
    }

    @Override
    public final void deliverResult(T data) {
        if (isReset()) {
            return;
        }

        T oldData = this.data;
        this.data = data;

        if (isStarted()) {
            super.deliverResult(data);
        }

        onNewDataDelivered(oldData, data);
    }

    @Override
    protected final void onStartLoading() {
        if (data != null) {
            deliverResult(data);
        }
        if (!listening) {
            onStartListening();
            listening = true;
        }
        if (takeContentChanged() || data == null) {
            forceLoad();
        }
    }

    @Override
    protected final void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    protected final void onReset() {
        super.onReset();
        onStopLoading();
        onCleanData(data);
        data = null;
        if (listening) {
            onStopListening();
            listening = false;
        }
    }
}
