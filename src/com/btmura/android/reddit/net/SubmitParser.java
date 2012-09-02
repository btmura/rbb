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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.net.RedditApi.SubmitResult;

class SubmitParser {

    public static final String TAG = "SubmitParser";

    static SubmitResult parse(InputStream in) throws IOException {
        SubmitResult result = new SubmitResult();
        JsonReader reader = new JsonReader(new InputStreamReader(in));
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("json".equals(name)) {
                parseJson(reader, result);
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, name);
                }
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.close();
        return result;
    }

    private static void parseJson(JsonReader reader, SubmitResult result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("captcha".equals(name)) {
                parseCaptcha(reader, result);
            } else if ("data".equals(name)) {
                parseData(reader, result);
            } else if ("errors".equals(name)) {
                parseErrors(reader, result);
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, name);
                }
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    // {"json": {"captcha": "D5GggaXa0GWshObkjzzEPzzrK8zpQfeB", "errors":
    // [["BAD_CAPTCHA", "care to try these again?", "captcha"]]}}
    private static void parseCaptcha(JsonReader reader, SubmitResult result) throws IOException {
        result.captcha = reader.nextString();
    }

    // {"json": {"errors": [], "data": {"url":
    // "http://www.reddit.com/r/rbb/comments/w5mhh/test/", "id": "w5mhh",
    // "name": "t3_w5mhh"}}}
    private static void parseData(JsonReader reader, SubmitResult result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("url".equals(name)) {
                result.url = reader.nextString();
            } else if ("name".equals(name)) {
                result.fullName = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static void parseErrors(JsonReader reader, SubmitResult result) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            parseError(reader, result);
        }
        reader.endArray();
    }

    private static void parseError(JsonReader reader, SubmitResult result) throws IOException {
        String[] error = new String[3];
        reader.beginArray();
        for (int i = 0; i < 3; i++) {
            error[i] = reader.nextString();
        }
        reader.endArray();
        if (result.errors == null) {
            result.errors = new ArrayList<String[]>();
        }
        result.errors.add(error);
    }
}
