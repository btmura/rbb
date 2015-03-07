/*
 * Copyright (C) 2015 Brian Muramatsu
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

import android.util.Base64;
import android.util.JsonReader;

import com.btmura.android.reddit.util.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AccessTokenResult extends JsonParser {

    public String accessToken;  // Example: 12345-abcdef
    public String tokenType;    // Example: bearer
    public long expiresIn;      // Example: 3600
    public String scope;        // Example: read
    public String refreshToken; // Example: 12345-67890

    public static AccessTokenResult getAccessToken(CharSequence clientId, CharSequence code, CharSequence redirectUri) throws IOException {
        HttpURLConnection conn = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            URL url = Urls.newUrl(Urls.API_ACCESS_TOKEN_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept-Charset", RedditApi.CHARSET);
            conn.setRequestProperty("User-Agent", RedditApi.USER_AGENT);

            StringBuilder sb = new StringBuilder(clientId).append(":");
            String auth = Base64.encodeToString(sb.toString().getBytes(RedditApi.CHARSET), Base64.DEFAULT);
            conn.setRequestProperty("Authorization", "Basic " + auth);

            conn.setRequestProperty("Content-Type", RedditApi.CONTENT_TYPE);
            conn.setDoOutput(true);

            conn.connect();

            sb = sb.delete(0, sb.length())
                    .append("grant_type=authorization_code&code=")
                    .append(code)
                    .append("&redirect_uri=")
                    .append(redirectUri);

            out = conn.getOutputStream();
            out.write(sb.toString().getBytes(RedditApi.CHARSET));

            in = conn.getInputStream();
            in = RedditApi.logResponse(in);
            return fromJson(new JsonReader(new InputStreamReader(in)));
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static AccessTokenResult fromJson(JsonReader r) throws IOException {
        AccessTokenResult result = new AccessTokenResult();
        r.beginObject();
        while (r.hasNext()) {
            String key = r.nextName();
            if ("access_token".equals(key)) {
                result.accessToken = r.nextString();
            } else if ("token_type".equals(key)) {
                result.tokenType = r.nextString();
            } else if ("expires_in".equals(key)) {
                result.expiresIn = r.nextLong();
            } else if ("scope".equals(key)) {
                result.scope = r.nextString();
            } else if ("refresh_token".equals(key)) {
                result.refreshToken = r.nextString();
            }
        }
        r.endObject();
        return result;
    }
}