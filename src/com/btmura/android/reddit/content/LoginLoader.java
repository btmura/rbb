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

package com.btmura.android.reddit.content;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.content.LoginLoader.LoginResult;
import com.btmura.android.reddit.data.Urls;

public class LoginLoader extends AsyncTaskLoader<LoginResult> {

    public static final String TAG = "LoginLoader";
    
    public static class LoginResult {
        public String error;
        public String cookie;
        public String modhash;
    }

    private static final String CHARSET_VALUE = "UTF-8";
    private static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded;charset="
            + CHARSET_VALUE;

    public final String login;
    public final String password;
    
    private LoginResult results;
    
    public LoginLoader(Context context, String login, String password) {
        super(context);
        this.login = login;
        this.password = password;
    }
    
    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (results != null) {
            deliverResult(results);
        } else {
            forceLoad();
        }
    }
    
    @Override
    public LoginResult loadInBackground() {
        HttpsURLConnection conn = null;
        try {
            URL url = Urls.loginUrl(login);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Accept-Charset", CHARSET_VALUE);
            conn.setRequestProperty("Content-Type", CONTENT_TYPE_VALUE);
            conn.setDoOutput(true);
            conn.connect();

            writeLoginQuery(conn, login, password);
            return parseLoginResponse(conn);

        } catch (IOException e) {
            Log.e(TAG, "loadInBackground", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private static void writeLoginQuery(HttpsURLConnection conn, String login, String password)
            throws IOException {
        OutputStream output = null;
        try {
            output = conn.getOutputStream();
            output.write(Urls.loginQuery(login, password).getBytes(CHARSET_VALUE));
            output.close();
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static LoginResult parseLoginResponse(HttpsURLConnection conn) throws IOException {
        LoginResult result = new LoginResult();
        JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
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
