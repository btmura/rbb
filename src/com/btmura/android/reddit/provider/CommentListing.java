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
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;

import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.data.Urls;

class CommentListing extends JsonParser implements Listing {

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(360);
    private final HashMap<String, ContentValues> valueMap = new HashMap<String, ContentValues>();

    private final Context context;
    private final String cookie;
    private final String thingId;
    private final URL url;

    CommentListing(Context context, String cookie, String thingId) {
        this.context = context;
        this.cookie = cookie;
        this.thingId = thingId;
        this.url = Urls.commentsUrl(thingId);
    }

    public void process() throws IOException {
        HttpURLConnection conn = NetApi.connect(context, url, cookie);
        InputStream input = conn.getInputStream();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingArray(reader);
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    public String getParent() {
        return thingId;
    }

    public ArrayList<ContentValues> getValues() {
        return values;
    }

    public HashMap<String, ContentValues> getValueMap() {
        return valueMap;
    }

    @Override
    public boolean shouldParseReplies() {
        return true;
    }

    @Override
    public void onEntityStart(int index) {
        values.add(new ContentValues(Comments.NUM_COLUMNS));
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_AUTHOR, readTrimmedString(reader, ""));
    }

    @Override
    public void onBody(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_BODY, readTrimmedString(reader, ""));
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_CREATED_UTC, reader.nextLong());
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_DOWNS, reader.nextInt());
    }

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        ContentValues v = values.get(index);
        v.put(Comments.COLUMN_NESTING, replyNesting);

        String kind = reader.nextString();
        if ("more".equalsIgnoreCase(kind)) {
            v.put(Comments.COLUMN_KIND, Comments.KIND_MORE);
        } else if (index != 0) {
            v.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
        } else {
            v.put(Comments.COLUMN_KIND, Comments.KIND_HEADER);
        }
    }

    @Override
    public void onLikes(JsonReader reader, int index) throws IOException {
        int likes = 0;
        if (reader.peek() == JsonToken.BOOLEAN) {
            likes = reader.nextBoolean() ? 1 : -1;
        } else {
            reader.skipValue();
        }
        values.get(index).put(Comments.COLUMN_LIKES, likes);
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        String id = readTrimmedString(reader, "");
        values.get(index).put(Comments.COLUMN_THING_ID, id);
        valueMap.put(id, values.get(index));
    }

    @Override
    public void onNumComments(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_NUM_COMMENTS, reader.nextInt());
    }

    @Override
    public void onSelfText(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_BODY, readTrimmedString(reader, ""));
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_TITLE, readTrimmedString(reader, ""));
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        values.get(index).put(Comments.COLUMN_UPS, reader.nextInt());
    }

    @Override
    public void onEntityEnd(int index) {
        values.get(index).put(Comments.COLUMN_PARENT_ID, thingId);
    }

    @Override
    public void onParseEnd() {
        int size = values.size();
        for (int i = 0; i < size;) {
            ContentValues v = values.get(i);
            Integer type = v.getAsInteger(Comments.COLUMN_KIND);
            if (type.intValue() == Comments.KIND_MORE) {
                String id = v.getAsString(Comments.COLUMN_THING_ID);
                values.remove(i);
                valueMap.remove(id);
                size--;
            } else {
                i++;
            }
        }
    }
}