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

import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.util.JsonParser;

import java.io.IOException;
import java.text.Collator;
import java.util.Set;
import java.util.TreeSet;

public class SubredditResult extends JsonParser {

  public final Set<String> subreddits =
      new TreeSet<String>(Collator.getInstance());

  public static SubredditResult getSubreddits(JsonReader r) throws IOException {
    SubredditResult result = new SubredditResult();
    result.parseListingObject(r);
    return result;
  }

  @Override
  public void onDisplayName(JsonReader r, int i) throws IOException {
    String subreddit = readString(r, "");
    if (!TextUtils.isEmpty(subreddit)) {
      subreddits.add(subreddit);
    }
  }
}