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

import com.btmura.android.reddit.util.Strings;

import junit.framework.TestCase;

public class StringsTest extends TestCase {

    public void testEllipsize() throws Exception {
        assertEquals("bri…", Strings.ellipsize("brian", 3));
        assertEquals("brian", Strings.ellipsize("brian", 5));
        assertEquals("", Strings.ellipsize("", 5));
        assertEquals(null, Strings.ellipsize(null, 7));
    }
}
