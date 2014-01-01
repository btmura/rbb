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

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.JsonParser;

/**
 * {@link SidebarResult} is the result of calling the
 * {@link RedditApi#getSidebar(Context, String, String)} method. It contains information about the
 * subreddit itself.
 */
public class SidebarResult extends JsonParser {

    public String subreddit;
    public String headerImage;
    public CharSequence title;
    public CharSequence description;
    public int subscribers;

    /** {@link Bitmap} of the header. Call {@link #fetchHeaderImage()} to get it. */
    public Bitmap headerImageBitmap;

    private final MarkdownFormatter formatter = new MarkdownFormatter();
    private final Context context;

    public static SidebarResult fromJsonReader(Context context, JsonReader reader)
            throws IOException {
        SidebarResult result = new SidebarResult(context);
        result.parseEntity(reader);
        if (!TextUtils.isEmpty(result.headerImage)) {
            result.headerImageBitmap = RedditApi.getBitmap(result.headerImage);
        }
        return result;
    }

    private SidebarResult(Context context) {
        this.context = context.getApplicationContext();
    }

    public void recycle() {
        if (headerImageBitmap != null) {
            headerImageBitmap.recycle();
            headerImageBitmap = null;
        }
    }

    // JSON attribute parsing methods

    @Override
    public void onDisplayName(JsonReader reader, int index) throws IOException {
        subreddit = reader.nextString();
    }

    @Override
    public void onHeaderImage(JsonReader reader, int index) throws IOException {
        headerImage = readString(reader, null);
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        title = formatter.formatNoSpans(context, readString(reader, ""));
    }

    @Override
    public void onDescription(JsonReader reader, int index) throws IOException {
        description = formatter.formatAll(context, readString(reader, ""));
    }

    @Override
    public void onSubscribers(JsonReader reader, int index) throws IOException {
        subscribers = reader.nextInt();
    }
}