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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.content.ThingProjection;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingView.OnThingViewClickListener;

public class ThingListAdapter extends AbstractThingListAdapter
    implements ThingProjection {

  public static final String TAG = "ThingListAdapter";

  private static final int[] LINK_DETAILS = {
      ThingView.DETAIL_UP_VOTES,
      ThingView.DETAIL_DOWN_VOTES,
      ThingView.DETAIL_DOMAIN,
  };

  private static final int[] COMMENT_DETAILS = {
      ThingView.DETAIL_UP_VOTES,
      ThingView.DETAIL_DOWN_VOTES,
      ThingView.DETAIL_SUBREDDIT,
  };

  private final MarkdownFormatter formatter = new MarkdownFormatter();
  private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
  private final OnThingViewClickListener listener;

  private String parentSubreddit;
  private String subreddit;

  public ThingListAdapter(
      Context ctx,
      String accountName,
      OnThingViewClickListener listener,
      boolean singleChoice) {
    super(ctx, accountName, singleChoice);
    this.listener = listener;
  }

  public void setParentSubreddit(String parentSubreddit) {
    this.parentSubreddit = parentSubreddit;
  }

  public void setSubreddit(String subreddit) {
    this.subreddit = subreddit;
  }

  public String getParentSubreddit() {
    return parentSubreddit;
  }

  public String getSubreddit() {
    return subreddit;
  }

  public boolean isSingleChoice() {
    return singleChoice;
  }

  @Override
  public void bindView(View v, Context ctx, Cursor c) {
    if (v instanceof ThingView) {
      bindThingView(v, ctx, c);
    }
  }

  private void bindThingView(View v, Context ctx, Cursor c) {
    final String author = c.getString(INDEX_AUTHOR);
    final String body = c.getString(INDEX_BODY);
    final long createdUtc = c.getLong(INDEX_CREATED_UTC);
    final String destination = null; // Only messages have destinations.
    final String domain = c.getString(INDEX_DOMAIN);
    final int downs = c.getInt(INDEX_DOWNS);
    final boolean expanded = true; // Expanded only for comments handled by different adapter.
    final boolean isNew = false; // Only messages can be new.
    final int kind = c.getInt(INDEX_KIND);
    final int likes = c.getInt(INDEX_LIKES);
    final String linkId = c.getString(INDEX_LINK_ID);
    final String linkTitle = c.getString(INDEX_LINK_TITLE);
    final int nesting = 0; // Nesting only for comments handled by different adapter.
    final int numComments = c.getInt(INDEX_NUM_COMMENTS);
    final boolean over18 = c.getInt(INDEX_OVER_18) == 1;
    final String subreddit = c.getString(INDEX_SUBREDDIT);
    final String thingId = c.getString(INDEX_THING_ID);
    final String thumbnailUrl = c.getString(INDEX_THUMBNAIL_URL);
    final String title = c.getString(INDEX_TITLE);
    final int ups = c.getInt(INDEX_UPS);

    // Comments don't have a score so calculate our own.
    final int score = kind == Kinds.KIND_COMMENT
        ? ups - downs
        : c.getInt(INDEX_SCORE);

    final boolean drawVotingArrows = AccountUtils.isAccount(accountName)
        && kind != Kinds.KIND_MESSAGE;
    final boolean showThumbnail = !TextUtils.isEmpty(thumbnailUrl);
    final boolean showStatusPoints = !AccountUtils.isAccount(accountName)
        || kind == Kinds.KIND_COMMENT;

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
        likes,
        linkTitle,
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
    tv.setChosen(singleChoice
        && Objects.equals(selectedThingId, thingId)
        && Objects.equals(selectedLinkId, linkId));
    tv.setThingViewOnClickListener(listener);
    setThingDetails(tv, kind);
    thumbnailLoader.setThumbnail(ctx, tv, thumbnailUrl);
  }

  private void setThingDetails(ThingView tv, int kind) {
    switch (kind) {
      case Kinds.KIND_LINK:
        tv.setDetails(LINK_DETAILS);
        break;

      case Kinds.KIND_COMMENT:
        tv.setDetails(COMMENT_DETAILS);
        break;

      default:
        throw new IllegalArgumentException();
    }
  }

  public ThingBundle getThingBundle(int pos) {
    Cursor cursor = getCursor();
    if (cursor != null && cursor.moveToPosition(pos)) {
      if (cursor.isNull(INDEX_LINK_ID)) {
        return ThingBundle.newLinkInstance(cursor.getString(INDEX_AUTHOR),
            cursor.getLong(INDEX_CREATED_UTC),
            cursor.getString(INDEX_DOMAIN),
            cursor.getInt(INDEX_DOWNS),
            cursor.getInt(INDEX_LIKES),
            cursor.getInt(INDEX_KIND),
            cursor.getInt(INDEX_NUM_COMMENTS),
            cursor.getInt(INDEX_OVER_18) == 1,
            cursor.getString(INDEX_PERMA_LINK),
            cursor.getInt(INDEX_SAVED) == 1,
            cursor.getInt(INDEX_SCORE),
            cursor.getInt(INDEX_SELF) == 1,
            cursor.getString(INDEX_SUBREDDIT),
            cursor.getString(INDEX_THING_ID),
            cursor.getString(INDEX_THUMBNAIL_URL),
            cursor.getString(INDEX_TITLE),
            cursor.getInt(INDEX_UPS),
            cursor.getString(INDEX_URL));
      } else {
        return ThingBundle.newCommentInstance(cursor.getString(INDEX_AUTHOR),
            cursor.getLong(INDEX_CREATED_UTC),
            cursor.getString(INDEX_DOMAIN),
            cursor.getInt(INDEX_DOWNS),
            cursor.getInt(INDEX_LIKES),
            cursor.getInt(INDEX_KIND),
            cursor.getString(INDEX_LINK_ID),
            cursor.getString(INDEX_LINK_TITLE),
            cursor.getInt(INDEX_NUM_COMMENTS),
            cursor.getInt(INDEX_OVER_18) == 1,
            cursor.getString(INDEX_PERMA_LINK),
            cursor.getInt(INDEX_SAVED) == 1,
            cursor.getInt(INDEX_SCORE),
            cursor.getInt(INDEX_SELF) == 1,
            cursor.getString(INDEX_SUBREDDIT),
            cursor.getString(INDEX_THING_ID),
            cursor.getString(INDEX_THUMBNAIL_URL),
            cursor.getString(INDEX_TITLE),
            cursor.getInt(INDEX_UPS),
            cursor.getString(INDEX_URL));
      }
    }
    return null;
  }

  @Override
  int getKindIndex() {
    return INDEX_KIND;
  }

  @Override
  String getLinkId(int pos) {
    return getString(pos, INDEX_LINK_ID);
  }

  @Override
  String getThingId(int pos) {
    return getString(pos, INDEX_THING_ID);
  }
}
