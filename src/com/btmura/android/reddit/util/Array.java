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

    public static String[] of(Object... elements) {
        int length = elements.length;
        String[] array = new String[length];
        for (int i = 0; i < length; i++) {
            array[i] = elements[i].toString();
        }
        return array;
    }

    public static <T> T[] ensureLength(T[] original, int capacity) {
        if (original.length < capacity) {
            original = Arrays.copyOf(original, capacity);
        }
        return original;
    }
}
