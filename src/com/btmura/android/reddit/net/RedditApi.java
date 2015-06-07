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

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class RedditApi {

  public static final String TAG = "RedditApi";

  static final String CHARSET = "UTF-8";
  static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=" + CHARSET;
  static final String USER_AGENT = "falling for reddit v3.4 by /u/btmura";

  /**
   * Logs entire response and returns a fresh InputStream as if nothing
   * happened. Make sure to delete all usages of this method, since it is only
   * for debugging.
   */
  static InputStream logResponse(InputStream in) throws IOException {
    // Make a copy of the InputStream.
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    for (int read = 0; (read = in.read(buffer)) != -1; ) {
      out.write(buffer, 0, read);
    }
    in.close();

    // Print out the response for debugging purposes.
    in = new ByteArrayInputStream(out.toByteArray());
    Scanner sc = new Scanner(in);
    while (sc.hasNextLine()) {
      Log.d(TAG, sc.nextLine());
    }
    sc.close();

    // Return a new InputStream as if nothing happened...
    return new BufferedInputStream(new ByteArrayInputStream(out.toByteArray()));
  }
}
