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

package com.btmura.android.reddit.database;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;

/**
 * {@link CursorWrapper} that allows sending back some extras with its
 * {@link Cursor}. This is a workaround for the fact that there is no public API
 * to set the extras, but there is a public API method to get extras.
 */
public class CursorExtrasWrapper extends CursorWrapper {

    /** Bundle of extras returned by {@link #getExtras}. */
    private final Bundle extras;

    public CursorExtrasWrapper(Cursor cursor, Bundle extras) {
        super(cursor);
        this.extras = extras != null ? extras : Bundle.EMPTY;
    }

    @Override
    public Bundle getExtras() {
        return extras;
    }
}
