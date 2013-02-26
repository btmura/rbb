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

/**
 * {@link ThingIds} is a utility class for manipulating thing ids.
 */
public class ThingIds {

    /** Add the given tag if the id does not have a tag at all. */
    public static String addTag(String id, String tag) {
        int sepIndex = id.indexOf('_');
        if (sepIndex != -1) {
            return id;
        }
        return tag + "_" + id;
    }

    /** Remove the tag if the id has one. */
    public static String removeTag(String id) {
        int sepIndex = id.indexOf('_');
        if (sepIndex != -1) {
            return id.substring(sepIndex + 1);
        }
        return id;
    }
}
