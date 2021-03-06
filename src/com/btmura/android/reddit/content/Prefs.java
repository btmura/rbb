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
import android.content.SharedPreferences;

class Prefs {

  // The first preferences were stored in "accountPreferences" so keep this
  // even if the key is not correct.
  private static final String PREFS_NAME = "accountPreferences";

  private static SharedPreferences PREFS_INSTANCE;

  synchronized static SharedPreferences getInstance(Context ctx) {
    if (PREFS_INSTANCE == null) {
      PREFS_INSTANCE = ctx.getApplicationContext()
          .getSharedPreferences(PREFS_NAME, 0);
    }
    return PREFS_INSTANCE;
  }
}
