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

package com.btmura.android.reddit.data;

import java.io.IOException;

import android.util.JsonReader;
import android.util.JsonToken;

public class JsonParser {

    public int replyNesting;

    private int entityIndex;

    public void parseListingArray(JsonReader reader) throws IOException {
        reset();
        onParseStart();
        doParseListingArray(reader);
        onParseEnd();
    }

    public void parseListingObject(JsonReader reader) throws IOException {
        reset();
        onParseStart();
        doParseListingObject(reader);
        onParseEnd();
    }

    public void parseEntity(JsonReader reader) throws IOException {
        reset();
        onParseStart();
        doParseEntityObject(reader);
        onParseEnd();
    }

    private void reset() {
        entityIndex = -1;
        replyNesting = 0;
    }

    private void doParseListingArray(JsonReader reader) throws IOException {
        if (JsonToken.BEGIN_ARRAY == reader.peek()) {
            reader.beginArray();
            while (reader.hasNext()) {
                doParseListingObject(reader);
            }
            reader.endArray();
        } else {
            reader.skipValue();
        }
    }

    private void doParseListingObject(JsonReader reader) throws IOException {
        if (JsonToken.BEGIN_OBJECT == reader.peek()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("data".equals(name)) {
                    doParseListingData(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } else {
            reader.skipValue();
        }
    }

    private void doParseListingData(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("children".equals(name)) {
                doParseListingChildren(reader);
            } else if ("after".equals(name)) {
                onAfter(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void doParseListingChildren(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            doParseEntityObject(reader);
        }
        reader.endArray();
    }

    private void doParseEntityObject(JsonReader r) throws IOException {
        int i = ++entityIndex;
        onEntityStart(i);
        r.beginObject();
        while (r.hasNext()) {
            String name = r.nextName();
            if ("kind".equals(name)) {
                onKind(r, i);
            } else if ("data".equals(name)) {
                doParseEntityData(r, i);
            } else {
                r.skipValue();
            }
        }
        r.endObject();
        onEntityEnd(i);
    }

    private void doParseEntityData(JsonReader r, int i) throws IOException {
        r.beginObject();
        while (r.hasNext()) {
            if (r.peek() == JsonToken.NULL) {
                r.skipValue();
                continue;
            }
            String name = r.nextName();
            if ("id".equals(name)) {
                onId(r, i);
            } else if ("name".equals(name)) {
                onName(r, i);
            } else if ("title".equals(name)) {
                onTitle(r, i);
            } else if ("over_18".equals(name)) {
                onOver18(r, i);
            } else if ("author".equals(name)) {
                onAuthor(r, i);
            } else if ("subreddit".equals(name)) {
                onSubreddit(r, i);
            } else if ("created_utc".equals(name)) {
                onCreatedUtc(r, i);
            } else if ("score".equals(name)) {
                onScore(r, i);
            } else if ("ups".equals(name)) {
                onUps(r, i);
            } else if ("downs".equals(name)) {
                onDowns(r, i);
            } else if ("likes".equals(name)) {
                onLikes(r, i);
            } else if ("num_comments".equals(name)) {
                onNumComments(r, i);
            } else if ("subreddit_id".equals(name)) {
                onSubredditId(r, i);
            } else if ("thumbnail".equals(name)) {
                onThumbnail(r, i);
            } else if ("is_self".equals(name)) {
                onIsSelf(r, i);
            } else if ("domain".equals(name)) {
                onDomain(r, i);
            } else if ("url".equals(name)) {
                onUrl(r, i);
            } else if ("permalink".equals(name)) {
                onPermaLink(r, i);
            } else if ("selftext".equals(name)) {
                onSelfText(r, i);
            } else if ("body".equals(name)) {
                onBody(r, i);
            } else if ("display_name".equals(name)) {
                onDisplayName(r, i);
            } else if ("description".equals(name)) {
                onDescription(r, i);
            } else if ("subscribers".equals(name)) {
                onSubscribers(r, i);
            } else if ("children".equals(name)) {
                onChildren(r, i);
            } else if ("replies".equals(name)) {
                if (shouldParseReplies()) {
                    replyNesting++;
                    doParseListingObject(r);
                    replyNesting--;
                } else {
                    r.skipValue();
                }
            } else {
                r.skipValue();
            }
        }
        r.endObject();
    }

    public boolean shouldParseReplies() {
        return false;
    }

    public void onParseStart() {
    }

    public void onAfter(JsonReader reader) throws IOException {
        reader.skipValue();
    }

    public void onEntityStart(int index) {
    }

    public void onKind(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onId(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onName(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onDisplayName(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onTitle(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onOver18(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onDescription(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onSubscribers(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onAuthor(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onSubreddit(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onUrl(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onSubredditId(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onThumbnail(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onPermaLink(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onIsSelf(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onSelfText(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onBody(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onNumComments(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onScore(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onUps(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onDowns(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onLikes(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onDomain(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onChildren(JsonReader reader, int index) throws IOException {
        reader.skipValue();
    }

    public void onEntityEnd(int index) {
    }

    public void onParseEnd() {
    }

    protected static String readTrimmedString(JsonReader reader, String nullValue)
            throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.skipValue();
            return nullValue;
        } else {
            return reader.nextString().trim();
        }
    }
}
