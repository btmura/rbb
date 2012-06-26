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
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.JsonReader;

import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.entity.Thing;

class ThingParser extends JsonParser {

    private final Context context;
    private final String parentSubreddit;
    private final List<Thing> initThings;

    private final long now = System.currentTimeMillis();
    final ArrayList<Thing> things = new ArrayList<Thing>(30);
    private String moreKey;

    ThingParser(Context context, String parentSubreddit, List<Thing> initThings) {
        this.context = context;
        this.parentSubreddit = parentSubreddit;
        this.initThings = initThings;
    }

    @Override
    public void onEntityStart(int index) {
        Thing t = new Thing();
        t.type = Thing.TYPE_THING;
        things.add(t);
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        things.get(index).name = readTrimmedString(reader, "");
    }

    @Override
    public void onIsSelf(JsonReader reader, int index) throws IOException {
        things.get(index).isSelf = reader.nextBoolean();
    }

    @Override
    public void onUrl(JsonReader reader, int index) throws IOException {
        things.get(index).url = readTrimmedString(reader, "");
    }

    @Override
    public void onPermaLink(JsonReader reader, int index) throws IOException {
        things.get(index).permaLink = readTrimmedString(reader, "");
    }

    @Override
    public void onThumbnail(JsonReader reader, int index) throws IOException {
        things.get(index).thumbnail = readTrimmedString(reader, "");
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        things.get(index).rawTitle = readTrimmedString(reader, "");
    }

    @Override
    public void onOver18(JsonReader reader, int index) throws IOException {
        things.get(index).over18 = reader.nextBoolean();
    }

    @Override
    public void onSubreddit(JsonReader reader, int index) throws IOException {
        things.get(index).subreddit = readTrimmedString(reader, "");
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        things.get(index).author = readTrimmedString(reader, "");
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        things.get(index).createdUtc = reader.nextLong();
    }

    @Override
    public void onScore(JsonReader reader, int index) throws IOException {
        things.get(index).score = reader.nextInt();
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        things.get(index).ups = reader.nextInt();
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        things.get(index).downs = reader.nextInt();
    }

    @Override
    public void onDomain(JsonReader reader, int index) throws IOException {
        things.get(index).domain = readTrimmedString(reader, "");
    }

    @Override
    public void onNumComments(JsonReader reader, int index) throws IOException {
        things.get(index).numComments = reader.nextInt();
    }

    @Override
    public void onEntityEnd(int index) {
        things.get(index).assureFormat(context, parentSubreddit, now);
    }

    @Override
    public void onAfter(JsonReader reader) throws IOException {
        moreKey = readTrimmedString(reader, null);
    }

    @Override
    public void onParseEnd() {
        if (moreKey != null) {
            Thing t = new Thing();
            t.type = Thing.TYPE_MORE;
            t.moreKey = moreKey;
            things.add(t);
        }

        if (initThings != null) {
            int size = initThings.size() - 1;
            if (size > 0) {
                things.ensureCapacity(things.size() + size);
                things.addAll(0, initThings.subList(0, size));
            }
        }
    }
}