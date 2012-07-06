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

package com.btmura.android.reddit.text;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilters {

    public static InputFilter LOGIN_FILTER = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (Character.isWhitespace(c)) {
                    return "";
                }
            }
            return null;
        }
    };

    public static InputFilter NO_SPACES_FILTER = LOGIN_FILTER;

    public static InputFilter SUBREDDIT_NAME_FILTER = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return "";
                }
            }
            return null;
        }
    };
}
