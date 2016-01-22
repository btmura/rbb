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
import android.content.Context;

import java.util.List;

/**
 * {@link Listing} is an internal interface to enforce some uniformity on
 * grabbing values to present to the user.
 */
interface Listing {

  /** Returns the type of session this listing creates. */
  int getSessionType();

  /** Returns the thing ID that may be used to identify a session. */
  String getSessionThingId();

  /** Get the values for this listing possibly using the network. */
  List<ContentValues> getValues() throws Exception;

  /** Return the name of the table where the values should be inserted. */
  String getTargetTable();

  /** Return whether this query is appending to an existing data set. */
  boolean isAppend();

}
