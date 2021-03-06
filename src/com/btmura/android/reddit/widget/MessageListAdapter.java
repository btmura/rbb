/*
 * Copyright (C) 2013 Brian Muramatsu
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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.Objects;

public class MessageListAdapter extends AbstractThingListAdapter {

  private static final int[] MESSAGE_DETAILS = {
      ThingView.DETAIL_TIMESTAMP,
      ThingView.DETAIL_AUTHOR,
      ThingView.DETAIL_DESTINATION,
  };

  private static final int[] MESSAGE_COMMENT_DETAILS = {
      ThingView.DETAIL_TIMESTAMP,
      ThingView.DETAIL_AUTHOR,
      ThingView.DETAIL_SUBREDDIT,
  };

  private final MarkdownFormatter formatter = new MarkdownFormatter();

  public MessageListAdapter(
      Context ctx,
      String accountName,
      boolean singleChoice) {
    super(ctx, accountName, singleChoice);
  }

  @Override
  public void bindView(View view, Context ctx, Cursor c) {
    if (view instanceof ThingView) {
      bindMessageThingView(view, c);
    }
  }

  private void bindMessageThingView(View v, Cursor c) {
    final String author = c.getString(MessageThingLoader.INDEX_AUTHOR);
    final String body = c.getString(MessageThingLoader.INDEX_BODY);
    final long createdUtc = c.getLong(
        MessageThingLoader.INDEX_CREATED_UTC);
    final String destination = c.getString(
        MessageThingLoader.INDEX_DESTINATION);
    final String domain = null; // No domain for messages.
    final int downs = 0; // No downs for messages.
    final boolean expanded = true; // Messages are always expanded.
    final boolean isNew = isNew(c.getPosition());
    final int kind = c.getInt(MessageThingLoader.INDEX_KIND);
    final int likes = 0; // No likes for messages.
    final int nesting = 0; // No nesting for messages.
    final int numComments = 0; // No comments for messages.
    final boolean over18 = false; // No over18 for messages.
    final String parentSubreddit = null; // No need for parentSubreddit for messages.
    final int score = 0; // No score for messages.
    final String subject = c.getString(MessageThingLoader.INDEX_SUBJECT);
    final String subreddit = c.getString(
        MessageThingLoader.INDEX_SUBREDDIT);
    final String thingId = c.getString(MessageThingLoader.INDEX_THING_ID);
    final String title = null; // No title for messages.
    final int ups = 0; // No upvotes for messages.

    final boolean drawVotingArrows = false; // No arrows for messages.
    final boolean showThumbnail = false; // No arrows for messages.
    final boolean showStatusPoints = false; // No points for messages.

    ThingView tv = (ThingView) v;
    tv.setData(author,
        body,
        createdUtc,
        destination,
        domain,
        downs,
        expanded,
        isNew,
        kind,
        likes, // actually linkTitle
        subject,
        nesting,
        nowTimeMs,
        numComments,
        over18,
        parentSubreddit,
        score,
        subreddit,
        thingBodyWidth,
        thingId,
        title,
        ups,
        drawVotingArrows,
        showThumbnail,
        showStatusPoints,
        formatter);
    tv.setChosen(singleChoice && Objects.equals(selectedThingId, thingId));
    setThingDetails(tv, kind);
  }

  public boolean isNew(int position) {
    Cursor c = getCursor();
    if (c != null && c.moveToPosition(position)) {
      return c.getInt(MessageThingLoader.INDEX_NEW) == 1;
    }
    return false;
  }

  private void setThingDetails(ThingView tv, int kind) {
    switch (kind) {
      case Kinds.KIND_MESSAGE:
        tv.setDetails(MESSAGE_DETAILS);
        break;

      case Kinds.KIND_COMMENT:
        tv.setDetails(MESSAGE_COMMENT_DETAILS);
        break;

      default:
        throw new IllegalArgumentException();
    }
  }

  public ThingBundle getThingBundle(int pos) {
    Cursor cursor = getCursor();
    if (cursor != null && cursor.moveToPosition(pos)) {
      switch (getKind(pos)) {
        case Kinds.KIND_MESSAGE:
          return ThingBundle.newMessageInstance(
              cursor.getString(MessageThingLoader.INDEX_AUTHOR),
              cursor.getInt(MessageThingLoader.INDEX_KIND),
              cursor.getString(MessageThingLoader.INDEX_SUBJECT),
              cursor.getString(MessageThingLoader.INDEX_THING_ID));

        case Kinds.KIND_COMMENT:
          return ThingBundle.newCommentReference(
              cursor.getString(MessageThingLoader.INDEX_SUBREDDIT),
              cursor.getString(MessageThingLoader.INDEX_THING_ID),
              getLinkId(cursor));
      }
    }
    return null;
  }

  private String getLinkId(Cursor c) {
    String context = c.getString(MessageThingLoader.INDEX_CONTEXT);
    String[] parts = context.split("/");
    if (parts != null && parts.length >= 5) {
      return parts[4];
    }
    throw new IllegalStateException();
  }

  @Override
  int getKindIndex() {
    return MessageThingLoader.INDEX_KIND;
  }

  @Override
  String getLinkId(int pos) {
    return null;
  }

  @Override
  String getThingId(int pos) {
    return getString(pos, MessageThingLoader.INDEX_THING_ID);
  }
}
