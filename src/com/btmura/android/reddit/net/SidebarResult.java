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

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.JsonParser;

import java.io.IOException;
import java.io.InputStream;

public class SidebarResult extends JsonParser {

  public String subreddit;
  public String headerImage;
  public CharSequence title;
  public CharSequence description;
  public int subscribers;

  public Bitmap headerImageBitmap;

  private final MarkdownFormatter formatter = new MarkdownFormatter();
  private final Context ctx;

  public static SidebarResult fromJson(Context ctx, InputStream in)
      throws IOException {
    JsonReader r = newReader(in);
    try {
      SidebarResult result = new SidebarResult(ctx);
      result.parseEntity(r);
      if (!TextUtils.isEmpty(result.headerImage)) {
        result.headerImageBitmap = RedditApi.getBitmap(result.headerImage);
      }
      return result;
    } finally {
      r.close();
    }
  }

  private SidebarResult(Context ctx) {
    this.ctx = ctx.getApplicationContext();
  }

  public void recycle() {
    if (headerImageBitmap != null) {
      headerImageBitmap.recycle();
      headerImageBitmap = null;
    }
  }

  // JSON attribute parsing methods

  @Override
  public void onDisplayName(JsonReader r, int i) throws IOException {
    subreddit = readString(r, "");
  }

  @Override
  public void onHeaderImage(JsonReader r, int i) throws IOException {
    headerImage = readString(r, "");
  }

  @Override
  public void onTitle(JsonReader r, int i) throws IOException {
    title = formatter.formatNoSpans(ctx, readString(r, ""));
  }

  @Override
  public void onDescription(JsonReader r, int i) throws IOException {
    description = formatter.formatAll(ctx, readString(r, ""));
  }

  @Override
  public void onSubscribers(JsonReader r, int i) throws IOException {
    subscribers = readInt(r, 0);
  }
}