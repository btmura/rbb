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

import android.os.Bundle;

import com.btmura.android.reddit.app.ComparableFragment;

/**
 * Utility class for handling {@link Object}s and other types.
 */
public class Objects {

    /** Check for object equality without worrying about NPEs. */
    public static boolean equals(Object o1, Object o2) {
        if (o1 != null) {
            return o1.equals(o2);
        } else if (o2 != null) {
            return o2.equals(o1);
        }
        return true;
    }

    /** Check for int equality to make the code look more uniform and avoid autoboxing. */
    public static boolean equals(int i1, int i2) {
        return i1 == i2;
    }

    /** Check for string equality without case sensitivity. */
    public static boolean equalsIgnoreCase(String s1, String s2) {
        if (s1 != null) {
            return s1.equalsIgnoreCase(s2);
        } else if (s2 != null) {
            return s2.equalsIgnoreCase(s1);
        }
        return true;
    }

    /** Check for fragment equality. */
    public static boolean fragmentEquals(ComparableFragment f1, ComparableFragment f2) {
        if (f1 != null) {
            return f1.fragmentEquals(f2);
        } else if (f2 != null) {
            return f2.fragmentEquals(f1);
        }
        return true;
    }

    public static Bundle nullToEmpty(Bundle bundle) {
        return bundle != null ? bundle : Bundle.EMPTY;
    }
}
