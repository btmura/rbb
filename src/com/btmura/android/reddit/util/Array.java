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

import java.util.Arrays;

public class Array {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Returns true if the array is null or empty
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static String[] of(Long oneLong) {
        return new String[] {oneLong.toString()};
    }

    public static String[] of(Long oneLong, String element) {
        return new String[] {oneLong.toString(), element};
    }

    public static String[] of(String... elements) {
        return elements;
    }

    public static String[] append(String[] original, String element) {
        if (original == null) {
            return new String[] {element};
        } else {
            original = ensureLength(original, original.length + 1);
            original[original.length - 1] = element;
            return original;
        }
    }

    public static <T> T[] ensureLength(T[] original, int capacity) {
        if (original.length < capacity) {
            original = Arrays.copyOf(original, capacity);
        }
        return original;
    }
}
