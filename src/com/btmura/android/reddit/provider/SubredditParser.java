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
import java.util.Collections;
import java.util.List;

import android.util.JsonReader;

import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.entity.Subreddit;

class SubredditParser extends JsonParser {

    List<Subreddit> results = new ArrayList<Subreddit>();

    @Override
    public void onEntityStart(int index) {
        results.add(Subreddit.emptyInstance());
    }

    @Override
    public void onDisplayName(JsonReader reader, int index) throws IOException {
        results.get(index).name = reader.nextString();
    }

    @Override
    public void onSubscribers(JsonReader reader, int index) throws IOException {
        results.get(index).subscribers = reader.nextInt();
    }

    @Override
    public void onParseEnd() {
        Collections.sort(results);
    }
}