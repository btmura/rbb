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

package com.btmura.android.reddit.util;

import android.util.JsonReader;
import android.util.JsonToken;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JsonParser {

  public int replyNesting;

  private int entityIndex;

  public void parseListingArray(JsonReader r) throws IOException {
    reset();
    onParseStart();
    doParseListingArray(r);
    onParseEnd();
  }

  public void parseListingObject(JsonReader r) throws IOException {
    reset();
    onParseStart();
    doParseListingObject(r);
    onParseEnd();
  }

  public void parseEntity(JsonReader r) throws IOException {
    reset();
    onParseStart();
    doParseEntityObject(r);
    onParseEnd();
  }

  public void parseEntityData(JsonReader r) throws IOException {
    reset();
    onParseStart();
    doParseEntityData(r, 0);
    onParseEnd();
  }

  private void reset() {
    entityIndex = -1;
    replyNesting = 0;
  }

  private void doParseListingArray(JsonReader r) throws IOException {
    if (JsonToken.BEGIN_ARRAY == r.peek()) {
      r.beginArray();
      while (r.hasNext()) {
        doParseListingObject(r);
      }
      r.endArray();
    } else {
      r.skipValue();
    }
  }

  private void doParseListingObject(JsonReader r) throws IOException {
    if (JsonToken.BEGIN_OBJECT == r.peek()) {
      r.beginObject();
      while (r.hasNext()) {
        String name = r.nextName();
        if ("data".equals(name)) {
          doParseListingData(r);
        } else {
          r.skipValue();
        }
      }
      r.endObject();
    } else {
      r.skipValue();
    }
  }

  private void doParseListingData(JsonReader r) throws IOException {
    r.beginObject();
    while (r.hasNext()) {
      String name = r.nextName();
      if ("children".equals(name)) {
        doParseListingChildren(r);
      } else if ("after".equals(name)) {
        onAfter(r);
      } else {
        r.skipValue();
      }
    }
    r.endObject();
  }

  private void doParseListingChildren(JsonReader r) throws IOException {
    r.beginArray();
    while (r.hasNext()) {
      doParseEntityObject(r);
    }
    r.endArray();
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
      if (isNextNull(r)) {
        r.skipValue();
        continue;
      }
      String name = r.nextName();
      if ("author".equals(name)) {
        onAuthor(r, i);
      } else if ("body".equals(name)) {
        onBody(r, i);
      } else if ("children".equals(name)) {
        onChildren(r, i);
      } else if ("comment_karma".equals(name)) {
        onCommentKarma(r, i);
      } else if ("context".equals(name)) {
        onContext(r, i);
      } else if ("created_utc".equals(name)) {
        onCreatedUtc(r, i);
      } else if ("description".equals(name)) {
        onDescription(r, i);
      } else if ("dest".equals(name)) {
        onDestination(r, i);
      } else if ("display_name".equals(name)) {
        onDisplayName(r, i);
      } else if ("domain".equals(name)) {
        onDomain(r, i);
      } else if ("downs".equals(name)) {
        onDowns(r, i);
      } else if ("has_mail".equals(name)) {
        onHasMail(r, i);
      } else if ("header_img".equals(name)) {
        onHeaderImage(r, i);
      } else if ("hidden".equals(name)) {
        onHidden(r, i);
      } else if ("id".equals(name)) {
        onId(r, i);
      } else if ("is_self".equals(name)) {
        onIsSelf(r, i);
      } else if ("likes".equals(name)) {
        onLikes(r, i);
      } else if ("link_id".equals(name)) {
        onLinkId(r, i);
      } else if ("link_karma".equals(name)) {
        onLinkKarma(r, i);
      } else if ("link_title".equals(name)) {
        onLinkTitle(r, i);
      } else if ("name".equals(name)) {
        onName(r, i);
      } else if ("new".equals(name)) {
        onNew(r, i);
      } else if ("num_comments".equals(name)) {
        onNumComments(r, i);
      } else if ("over_18".equals(name) || "over18".equals(name)) {
        onOver18(r, i);
      } else if ("permalink".equals(name)) {
        onPermaLink(r, i);
      } else if ("replies".equals(name)) {
        onReplies(r, i);
      } else if ("saved".equals(name)) {
        onSaved(r, i);
      } else if ("score".equals(name)) {
        onScore(r, i);
      } else if ("selftext".equals(name)) {
        onSelfText(r, i);
      } else if ("subject".equals(name)) {
        onSubject(r, i);
      } else if ("subreddit".equals(name)) {
        onSubreddit(r, i);
      } else if ("subreddit_id".equals(name)) {
        onSubredditId(r, i);
      } else if ("subscribers".equals(name)) {
        onSubscribers(r, i);
      } else if ("title".equals(name)) {
        onTitle(r, i);
      } else if ("thumbnail".equals(name)) {
        onThumbnail(r, i);
      } else if ("ups".equals(name)) {
        onUps(r, i);
      } else if ("url".equals(name)) {
        onUrl(r, i);
      } else if ("was_comment".equals(name)) {
        onWasComment(r, i);
      } else {
        r.skipValue();
      }
    }
    r.endObject();
  }

  public void onParseStart() {
  }

  public void onEntityStart(int i) {
  }

  public void onAuthor(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onBody(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onChildren(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onCommentKarma(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onContext(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onCreatedUtc(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onDescription(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onDestination(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onDisplayName(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onDomain(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onDowns(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onHeaderImage(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onKind(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onHasMail(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onHidden(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onId(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onIsSelf(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onLikes(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onLinkId(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onLinkKarma(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onLinkTitle(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onName(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onNew(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onNumComments(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onOver18(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onPermaLink(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onReplies(JsonReader r, int i) throws IOException {
    if (shouldParseReplies()) {
      replyNesting++;
      doParseListingObject(r);
      replyNesting--;
    } else {
      r.skipValue();
    }
  }

  public void onSaved(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onScore(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onSelfText(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onSubscribers(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onSubject(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onSubreddit(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onSubredditId(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onTitle(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onThumbnail(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onUps(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onUrl(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onWasComment(JsonReader r, int i) throws IOException {
    r.skipValue();
  }

  public void onEntityEnd(int i) {
  }

  public void onAfter(JsonReader r) throws IOException {
    r.skipValue();
  }

  public void onParseEnd() {
  }

  protected boolean shouldParseReplies() {
    return false;
  }

  protected static boolean readBoolean(JsonReader r, boolean defaultValue)
      throws IOException {
    if (isNextNull(r)) {
      r.skipValue();
      return defaultValue;
    }
    return r.nextBoolean();
  }

  protected static int readInt(JsonReader r, int defaultValue)
      throws IOException {
    if (isNextNull(r)) {
      r.skipValue();
      return defaultValue;
    }
    return r.nextInt();
  }

  protected static long readLong(JsonReader r, long defaultValue)
      throws IOException {
    if (isNextNull(r)) {
      r.skipValue();
      return defaultValue;
    }
    return r.nextLong();
  }

  protected static String readString(JsonReader r, String defaultValue)
      throws IOException {
    if (isNextNull(r)) {
      r.skipValue();
      return defaultValue;
    }
    return r.nextString().trim();
  }

  protected static boolean isNextBoolean(JsonReader r) throws IOException {
    return JsonToken.BOOLEAN.equals(r.peek());
  }

  protected static boolean isNextNull(JsonReader r) throws IOException {
    return JsonToken.NULL.equals(r.peek());
  }
}
