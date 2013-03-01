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

package com.btmura.android.reddit.app;

import android.content.Context;
import android.content.SharedPreferences;

class Preferences {

    private static final String PREFS_NAME = "preferences";

    private static final String PREF_THEME = "theme";
    private static final int THEME_LIGHT = android.R.style.Theme_Holo_Light;
    private static final int THEME_DARK = android.R.style.Theme_Holo;

    private static SharedPreferences PREFS_INSTANCE;

    public static final int getTheme(Context context) {
        return getPrefsInstance(context).getInt(PREF_THEME, THEME_LIGHT);
    }

    public static final void switchTheme(Context context) {
        int otherTheme = getTheme(context) == THEME_LIGHT ? THEME_DARK : THEME_LIGHT;
        getPrefsInstance(context).edit().putInt(PREF_THEME, otherTheme).apply();
    }

    private synchronized static SharedPreferences getPrefsInstance(Context context) {
        if (PREFS_INSTANCE == null) {
            PREFS_INSTANCE = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        }
        return PREFS_INSTANCE;
    }
}
