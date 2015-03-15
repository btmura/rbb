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
import java.util.Collections;
import java.util.List;

public class Array {

    public static String[] of(long oneLong) {
        return new String[] {Long.toString(oneLong)};
    }

    public static String[] of(long oneLong, String element) {
        return new String[] {Long.toString(oneLong), element};
    }

    public static String[] of(int oneInt, String element) {
        return new String[] {Integer.toString(oneInt), element};
    }

    public static String[] of(String oneString) {
        return new String[] {oneString};
    }

    public static String[] of(String... elements) {
        return elements;
    }

    public static boolean[] newBooleanArray(int length, boolean fillValue) {
        boolean[] array = new boolean[length];
        Arrays.fill(array, fillValue);
        return array;
    }

    public static int[] newIntArray(int length, int fillValue) {
        int[] array = new int[length];
        Arrays.fill(array, fillValue);
        return array;
    }

    /**
     * Returns true if the array is null or empty
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
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

    public static <T> List<T> asList(T[] array) {
        if (array != null) {
            return Arrays.asList(array);
        }
        return Collections.emptyList();
    }
}
