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

package com.btmura.android.reddit.net;

import java.io.IOException;

import android.util.JsonReader;

/**
 * {@link LoginResult} is the result of calling the
 * {@link RedditApi#login(android.content.Context, String, String)} method.
 */
public class LoginResult {

    public String cookie;
    public String modhash;
    public String error;

    public static LoginResult fromJsonReader(JsonReader reader) throws IOException {
        LoginResult result = new LoginResult();
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
        reader.close();
        return result;
    }

    private static void parseJson(JsonReader reader, LoginResult result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("errors".equals(name)) {
                parseErrorsArray(reader, result);
            } else if ("data".equals(name)) {
                parseDataObject(reader, result);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static void parseErrorsArray(JsonReader reader, LoginResult result) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            parseSingleErrorArray(reader, result);
        }
        reader.endArray();
    }

    private static void parseSingleErrorArray(JsonReader reader, LoginResult result)
            throws IOException {
        reader.beginArray();
        for (int i = 0; reader.hasNext(); i++) {
            if (i < 2) {
                result.error = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endArray();
    }

    private static void parseDataObject(JsonReader reader, LoginResult result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("modhash".equals(name)) {
                result.modhash = reader.nextString();
            } else if ("cookie".equals(name)) {
                result.cookie = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}