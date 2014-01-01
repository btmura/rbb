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

import com.btmura.android.reddit.R;

public class ThemePrefs extends Prefs {

    private static final String PREF_THEME = "theme";
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;

    public static final int getTheme(Context context) {
        return pick(context, R.style.Theme_Light, R.style.Theme_Dark);
    }

    public static final int getDialogTheme(Context context) {
        return pick(context, R.style.Theme_Light_Dialog, R.style.Theme_Dark_Dialog);
    }

    public static final int getDialogWhenLargeTheme(Context context) {
        return pick(context, R.style.Theme_Light_DialogWhenLarge,
                R.style.Theme_Dark_DialogWhenLarge);
    }

    public static final int getDrawerIcon(Context context) {
        return pick(context, R.drawable.ic_drawer_light,
                R.drawable.ic_drawer_dark);
    }

    public static final void switchTheme(Context context) {
        int otherTheme = pick(context, THEME_DARK, THEME_LIGHT);
        getPrefsInstance(context).edit().putInt(PREF_THEME, otherTheme).apply();
    }

    private static final int pick(Context context, int lightValue, int darkValue) {
        return getPrefsInstance(context).getInt(PREF_THEME, THEME_DARK) == THEME_LIGHT ?
                lightValue : darkValue;
    }
}
