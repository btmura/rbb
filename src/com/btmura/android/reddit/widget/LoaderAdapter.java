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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

public abstract class LoaderAdapter extends BaseCursorAdapter {

    LoaderAdapter(Context context) {
        super(context, null, 0);
    }

    public abstract boolean isLoadable();

    public abstract Loader<Cursor> getLoader(Context context, Bundle args);

    public void setAccountName(String accountName) {
        // Override this if applicable to your adapter.
    }

    public void setSessionId(long sessionId) {
        // Override this if applicable to your adapter.
    }

    public void setThingId(String thingId) {
        // Override this if applicable to your adapter.
    }
}
