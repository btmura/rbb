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

package com.btmura.android.reddit.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.test.AndroidTestCase;


public class ResponseParserTest extends AndroidTestCase {

    public void testErrors_empty() throws IOException {
        Result result = parse("{\"json\": {\"errors\": []}}");
        assertNull(result.errors);
    }

    public void testErrors_nullToken() throws IOException {
        Result result = parse("{\"json\": {\"errors\": [[\"QUOTA_FILLED\","
                + " \"You've submitted too many links recently. Please try again in an hour.\","
                + " null]]}}");
        assertNotNull(result.errors);
    }

    private Result parse(String json) throws IOException {
        InputStream in = new ByteArrayInputStream(json.getBytes());
        try {
            return ResponseParser.parseResponse(in);
        } finally {
            in.close();
        }
    }
}
