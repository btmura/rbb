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

import android.util.JsonReader;

import com.btmura.android.reddit.util.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AccessTokenResult extends JsonParser {

  public String accessToken;  // Example: 12345-abcdef
  public String tokenType;    // Example: bearer
  public long expiresIn;      // Example: 3600
  public String scope;        // Example: read
  public String refreshToken; // Example: 12345-67890

  /** Expiration time in milliseconds of the access token. */
  public long expirationMs;

  static AccessTokenResult getAccessToken(JsonReader r, long retrievalTimeMs)
      throws IOException {
    AccessTokenResult result = new AccessTokenResult();
    r.beginObject();
    while (r.hasNext()) {
      String key = r.nextName();
      if ("access_token".equals(key)) {
        result.accessToken = readString(r, "");
      } else if ("token_type".equals(key)) {
        result.tokenType = readString(r, "");
      } else if ("expires_in".equals(key)) {
        result.expiresIn = readLong(r, 0);
      } else if ("scope".equals(key)) {
        result.scope = readString(r, "");
      } else if ("refresh_token".equals(key)) {
        result.refreshToken = readString(r, "");
      }
    }
    r.endObject();
    result.expirationMs = retrievalTimeMs
        + TimeUnit.SECONDS.toMillis(result.expiresIn);
    return result;
  }

  @Override
  public String toString() {
    return "accessToken: " + accessToken
        + " tokenType: " + tokenType
        + " expiresIn: " + expiresIn
        + " scope: " + scope
        + " refreshToken: " + refreshToken
        + " expirationMs: " + expirationMs;
  }
}
