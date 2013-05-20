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

import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

/**
 * {@link Listing} is an internal interface to enforce some uniformity on grabbing values to present
 * to the user.
 */
interface Listing {

    /** Returns the type of session this listing creates. */
    int getSessionType();

    /** Returns the tag that can be used to identify an existing session. */
    String getSessionTag();

    /** Get the values for this listing possibly using the network. */
    ArrayList<ContentValues> getValues() throws IOException;

    /** Called after the insertion transaction to perform additional ops. */
    void performExtraWork(Context context);

    /** Add additional extras to a bundle if applicable. */
    void addCursorExtras(Bundle bundle);

    /** Return the name of the table where the values should be inserted. */
    String getTargetTable();

    /** Return whether this query is appending to an existing data set. */
    boolean isAppend();

    /** Return the network time for statistical purposes. */
    long getNetworkTimeMs();

    /** Return the parsing time for statistical purposes. */
    long getParseTimeMs();
}
