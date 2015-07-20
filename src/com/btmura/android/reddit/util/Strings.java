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

package com.btmura.android.reddit.util;

import android.text.TextUtils;

public class Strings {

  static final String ELLIPSIS = "…";

  public static CharSequence ellipsize(CharSequence text, int maxLength) {
    if (maxLength < 0 || TextUtils.isEmpty(
        text) || text.length() <= maxLength) {
      return text;
    }

    // Trim extra whitespace if we are cutting it on a blank spot.
    int i = maxLength - 1;
    for (; i > 0 && Character.isWhitespace(text.charAt(i)); i--) {
    }

    return new StringBuilder(text.subSequence(0, i + 1)).append(ELLIPSIS);
  }

  public static String emptyToNull(String string) {
    return TextUtils.isEmpty(string) ? null : string;
  }

  public static String toString(Object object) {
    return object != null ? object.toString() : null;
  }
}
