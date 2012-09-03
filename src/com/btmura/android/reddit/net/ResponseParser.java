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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.JsonReader;

import com.btmura.android.reddit.net.RedditApi.Result;
import com.btmura.android.reddit.util.Array;

class ResponseParser {

    static Result parseResponse(InputStream in) throws IOException {
        Result result = new Result();
        JsonReader reader = new JsonReader(new InputStreamReader(new BufferedInputStream(in)));
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("json".equals(name)) {
                    parseJson(reader, result);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
        return result;
    }

    private static void parseJson(JsonReader reader, Result result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("errors".equals(name)) {
                result.errors = parseErrorsArray(reader);
            } else if ("ratelimit".equals(name)) {
                result.rateLimit = reader.nextDouble();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static String[][] parseErrorsArray(JsonReader reader) throws IOException {
        String[][] errors = null;
        reader.beginArray();
        for (int i = 0; reader.hasNext(); i++) {
            // There should only be 1 error but permit expansion in emergency.
            if (errors == null) {
                errors = new String[1][];
            } else {
                errors = Array.ensureLength(errors, i + 1);
            }
            errors[i] = parseSingleErrorArray(reader);
        }
        reader.endArray();
        return errors;
    }

    private static String[] parseSingleErrorArray(JsonReader reader) throws IOException {
        // There should only be 3 elements per error but permit expansion.
        String[] error = new String[3];
        reader.beginArray();
        for (int i = 0; reader.hasNext(); i++) {
            error = Array.ensureLength(error, i + 1);
            error[i] = reader.nextString();
        }
        reader.endArray();
        return error;
    }
}
