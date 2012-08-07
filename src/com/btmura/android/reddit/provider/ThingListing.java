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

package com.btmura.android.reddit.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;

import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.data.Urls;

class ThingListing extends JsonParser {

    final ArrayList<ContentValues> values = new ArrayList<ContentValues>(30);

    private final Formatter formatter = new Formatter();
    private final Context context;
    private final String cookie;
    private final String subredditName;
    private final URL url;

    ThingListing(Context context, String cookie, String subredditName, int filter, String more) {
        this.context = context;
        this.cookie = cookie;
        this.subredditName = subredditName;
        this.url = Urls.subredditUrl(subredditName, filter, more);
    }

    public void process() throws IOException {
        HttpURLConnection conn = NetApi.connect(context, url, cookie);
        InputStream input = conn.getInputStream();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingObject(reader);
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    @Override
    public void onEntityStart(int index) {
        values.add(new ContentValues(Things.NUM_COLUMNS));
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_AUTHOR, readTrimmedString(reader, ""));
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_CREATED_UTC, reader.nextLong());
    }

    @Override
    public void onDomain(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_DOMAIN, readTrimmedString(reader, ""));
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_DOWNS, reader.nextInt());
    }

    @Override
    public void onLikes(JsonReader reader, int index) throws IOException {
        int likes = 0;
        if (reader.peek() == JsonToken.BOOLEAN) {
            likes = reader.nextBoolean() ? 1 : -1;
        } else {
            reader.skipValue();
        }
        values.get(index).put(Things.COLUMN_LIKES, likes);
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        String name = readTrimmedString(reader, "");
        values.get(index).put(Things.COLUMN_THING_ID, name);
    }

    @Override
    public void onNumComments(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_NUM_COMMENTS, reader.nextInt());
    }

    @Override
    public void onOver18(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_OVER_18, reader.nextBoolean());
    }

    @Override
    public void onPermaLink(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_PERMA_LINK, readTrimmedString(reader, ""));
    }

    @Override
    public void onScore(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SCORE, reader.nextInt());
    }

    @Override
    public void onIsSelf(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SELF, reader.nextBoolean());
    }

    @Override
    public void onSubreddit(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SUBREDDIT, readTrimmedString(reader, ""));
    }

    @Override
    public void onSubredditName(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_PARENT, readTrimmedString(reader, ""));
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        CharSequence title = formatter.formatTitle(context, readTrimmedString(reader, ""));
        values.get(index).put(Things.COLUMN_TITLE, title.toString());
    }

    @Override
    public void onThumbnail(JsonReader reader, int index) throws IOException {
        String thumbnail = readTrimmedString(reader, null);
        if (!TextUtils.isEmpty(thumbnail) && thumbnail.startsWith("http")) {
            values.get(index).put(Things.COLUMN_THUMBNAIL_URL, thumbnail);
        }
    }

    @Override
    public void onUrl(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_URL, readTrimmedString(reader, ""));
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_UPS, reader.nextInt());
    }

    @Override
    public void onEntityEnd(int index) {
        values.get(index).put(Things.COLUMN_PARENT, subredditName);
    }
}
