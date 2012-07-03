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

import android.content.Context;
import android.util.JsonReader;

import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.entity.Subreddit;

class SidebarParser extends JsonParser {

    private final Formatter formatter = new Formatter();
    private final Context context;

    SidebarParser(Context context) {
        this.context = context;
    }

    Subreddit results;

    @Override
    public void onEntityStart(int index) {
        results = Subreddit.emptyInstance();
    }

    @Override
    public void onDisplayName(JsonReader reader, int index) throws IOException {
        results.name = reader.nextString();
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        results.title = formatter.formatTitle(context, readTrimmedString(reader, ""));
    }

    @Override
    public void onSubscribers(JsonReader reader, int index) throws IOException {
        results.subscribers = reader.nextInt();
    }

    @Override
    public void onDescription(JsonReader reader, int index) throws IOException {
        results.description = formatter.formatInfo(context, readTrimmedString(reader, ""));
    }

    @Override
    public void onEntityEnd(int index) {
        results.assureFormat(context);
    }
}