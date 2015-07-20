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

package com.btmura.android.reddit.util;

import com.btmura.android.reddit.app.ComparableFragment;

public class ComparableFragments {

  public static boolean equalClasses(
      ComparableFragment o1,
      ComparableFragment o2) {
    return o1 != null && o2 != null && o1.getClass() == o2.getClass();
  }

  public static boolean equalStrings(
      ComparableFragment o1,
      ComparableFragment o2,
      String key) {
    return Objects.equals(o1.getArguments().getString(key),
        o2.getArguments().getString(key));
  }

  public static boolean equalInts(
      ComparableFragment o1,
      ComparableFragment o2,
      String key) {
    return Objects.equals(o1.getArguments().getInt(key),
        o2.getArguments().getInt(key));
  }
}
