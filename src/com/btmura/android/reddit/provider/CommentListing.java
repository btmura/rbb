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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.app.CommentLogic;
import com.btmura.android.reddit.app.CommentLogic.CommentList;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class CommentListing extends JsonParser implements Listing, CommentList {

  public static final String TAG = "CommentListing";

  private static final String[] PROJECTION = {
      CommentActions._ID,
      CommentActions.COLUMN_ACCOUNT,
      CommentActions.COLUMN_ACTION,
      CommentActions.COLUMN_THING_ID,
      CommentActions.COLUMN_TEXT,
  };

  private static final int INDEX_ID = 0;
  private static final int INDEX_ACCOUNT_NAME = 1;
  private static final int INDEX_ACTION = 2;
  private static final int INDEX_THING_ID = 3;
  private static final int INDEX_TEXT = 4;

  private final Context ctx;
  private final SQLiteOpenHelper dbHelper;
  private final String accountName;
  private final String thingId;
  private final String linkId;
  private final int filter;
  private final int numComments;
  private final MarkdownFormatter formatter = new MarkdownFormatter();

  // TODO: Pass estimate of size to CommentListing rather than doing this.
  private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(
      360);
  private final HashMap<String, ContentValues> valueMap = new HashMap<String, ContentValues>();
  private Map<String, Integer> saveActionMap;
  private Map<String, Integer> voteActionMap;

  static CommentListing newInstance(
      Context ctx,
      SQLiteOpenHelper dbHelper,
      String accountName,
      String thingId,
      String linkId,
      int filter,
      int numComments) {
    return new CommentListing(
        ctx,
        dbHelper,
        accountName,
        thingId,
        linkId,
        filter,
        numComments);
  }

  private CommentListing(
      Context ctx,
      SQLiteOpenHelper dbHelper,
      String accountName,
      String thingId,
      String linkId,
      int filter,
      int numComments) {
    this.ctx = ctx;
    this.dbHelper = dbHelper;
    this.accountName = accountName;
    this.thingId = thingId;
    this.linkId = linkId;
    this.filter = filter;
    this.numComments = numComments;
  }

  @Override
  public int getSessionType() {
    return Sessions.TYPE_COMMENTS;
  }

  @Override
  public String getSessionThingId() {
    return !TextUtils.isEmpty(linkId) ? linkId : thingId;
  }

  @Override
  public ArrayList<ContentValues> getValues()
      throws AuthenticatorException, OperationCanceledException, IOException {
    CharSequence url = Urls.comments(
        accountName,
        thingId,
        linkId,
        filter,
        numComments);
    HttpURLConnection conn = RedditApi.connect(ctx, accountName, url);
    InputStream input = null;
    try {
      input = new BufferedInputStream(conn.getInputStream());
      saveActionMap = SaveMerger.getActionMap(dbHelper, accountName);
      voteActionMap = VoteMerger.getActionMap(dbHelper, accountName);

      JsonReader reader = new JsonReader(new InputStreamReader(input));
      parseListingArray(reader);
      return values;
    } finally {
      if (input != null) {
        input.close();
      }
      conn.disconnect();
    }
  }

  @Override
  public void performExtraWork(Context context) {
  }

  @Override
  public String getTargetTable() {
    return Comments.TABLE_NAME;
  }

  @Override
  public boolean isAppend() {
    return false;
  }

  @Override
  public boolean shouldParseReplies() {
    return true;
  }

  @Override
  public void onEntityStart(int i) {
    ContentValues v = new ContentValues(20);
    v.put(Comments.COLUMN_ACCOUNT, accountName);
    values.add(v);
  }

  @Override
  public void onAuthor(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_AUTHOR, readString(r, ""));
  }

  @Override
  public void onBody(JsonReader r, int i) throws IOException {
    CharSequence body = formatter.formatNoSpans(readString(r, ""));
    values.get(i).put(Comments.COLUMN_BODY, body.toString());
  }

  @Override
  public void onCreatedUtc(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_CREATED_UTC, readLong(r, 0));
  }

  @Override
  public void onDomain(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_DOMAIN, readString(r, ""));
  }

  @Override
  public void onDowns(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_DOWNS, readInt(r, 0));
  }

  @Override
  public void onHidden(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_HIDDEN, readBoolean(r, false));
  }

  @Override
  public void onIsSelf(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_SELF, readBoolean(r, false));
  }

  @Override
  public void onKind(JsonReader r, int i) throws IOException {
    ContentValues v = values.get(i);
    v.put(Comments.COLUMN_NESTING, replyNesting);
    v.put(Comments.COLUMN_KIND, Kinds.parseKind(readString(r, "")));
  }

  @Override
  public void onLikes(JsonReader r, int i) throws IOException {
    int likes = 0;
    if (isNextBoolean(r)) {
      likes = readBoolean(r, false) ? 1 : -1;
    } else {
      r.skipValue();
    }
    values.get(i).put(Comments.COLUMN_LIKES, likes);
  }

  @Override
  public void onName(JsonReader r, int i) throws IOException {
    String id = readString(r, "");
    ContentValues v = values.get(i);
    v.put(Comments.COLUMN_THING_ID, id);
    valueMap.put(id, v);
  }

  @Override
  public void onNumComments(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_NUM_COMMENTS, readInt(r, 0));
  }

  @Override
  public void onOver18(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_OVER_18, readBoolean(r, false));
  }

  @Override
  public void onPermaLink(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_PERMA_LINK, readString(r, ""));
  }

  @Override
  public void onSaved(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_SAVED, readBoolean(r, false));
  }

  @Override
  public void onScore(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_SCORE, readInt(r, 0));
  }

  @Override
  public void onSelfText(JsonReader r, int i) throws IOException {
    CharSequence body = formatter.formatNoSpans(readString(r, ""));
    values.get(i).put(Comments.COLUMN_BODY, body.toString());
  }

  @Override
  public void onSubreddit(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_SUBREDDIT, readString(r, ""));
  }

  @Override
  public void onTitle(JsonReader r, int i) throws IOException {
    CharSequence title = formatter.formatNoSpans(readString(r, ""));
    values.get(i).put(Comments.COLUMN_TITLE, title.toString());
  }

  @Override
  public void onUps(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_UPS, readInt(r, 0));
  }

  @Override
  public void onUrl(JsonReader r, int i) throws IOException {
    values.get(i).put(Comments.COLUMN_URL, readString(r, ""));
  }

  @Override
  public void onThumbnail(JsonReader r, int i) throws IOException {
    // TODO: Remove code duplication with ThingListing.
    String thumbnail = readString(r, null);
    if (!TextUtils.isEmpty(thumbnail) && thumbnail.startsWith("http")) {
      values.get(i).put(Comments.COLUMN_THUMBNAIL_URL, thumbnail);
    }
  }

  @Override
  public void onParseEnd() {
    // Merge local inserts and deletes that haven't been synced yet.
    mergeActions();

    doFinalMerge();
  }

  private void mergeActions() {
    SQLiteDatabase db = dbHelper.getReadableDatabase();

    // Select by parent ID to see changes by all accounts, since the user can pick what account
    // to use when making a comment. Do the same for edits and deletes to be consistent.
    Cursor c = db.query(CommentActions.TABLE_NAME,
        PROJECTION,
        CommentActions.SELECT_BY_PARENT_THING_ID,
        Array.of(thingId),
        null,
        null,
        CommentActions.SORT_BY_ID);
    try {
      while (c.moveToNext()) {
        long actionId = c.getLong(INDEX_ID);
        String actionAccountName = c.getString(INDEX_ACCOUNT_NAME);
        int action = c.getInt(INDEX_ACTION);
        String text = c.getString(INDEX_TEXT);
        String actionThingId = c.getString(INDEX_THING_ID);
        switch (action) {
          case CommentActions.ACTION_INSERT:
            insertThing(actionId, actionAccountName, actionThingId, text);
            break;

          case CommentActions.ACTION_DELETE:
            deleteThing(actionThingId);
            break;

          case CommentActions.ACTION_EDIT:
            editThing(actionId, actionThingId, text);
            break;

          default:
            throw new IllegalStateException();
        }
      }
    } finally {
      c.close();
    }
  }

  private boolean insertThing(
      long actionId, String actionAccountName, String actionThingId,
      String body) {
    int size = values.size();
    for (int i = 0; i < size; i++) {
      ContentValues v = values.get(i);
      String id = v.getAsString(Comments.COLUMN_THING_ID);

      // This thing could be a placeholder we previously inserted.
      if (TextUtils.isEmpty(id)) {
        continue;
      }

      if (id.equals(actionThingId)) {
        ContentValues p = new ContentValues(7 + 1); // +1 for session id.
        p.put(Comments.COLUMN_ACCOUNT, actionAccountName);
        p.put(Comments.COLUMN_AUTHOR, actionAccountName);
        p.put(Comments.COLUMN_BODY, body);
        p.put(Comments.COLUMN_COMMENT_ACTION_ID, actionId);
        p.put(Comments.COLUMN_KIND, Kinds.KIND_COMMENT);
        p.put(Comments.COLUMN_NESTING, CommentLogic.getInsertNesting(this, i));

        values.add(CommentLogic.getInsertPosition(this, i), p);
        size++;

        return true;
      }
    }
    return false;
  }

  private boolean deleteThing(String actionThingId) {
    int size = values.size();
    for (int i = 0; i < size; i++) {
      ContentValues v = values.get(i);
      String id = v.getAsString(Comments.COLUMN_THING_ID);
      if (actionThingId.equals(id)) {
        // Mark the header comment or comment with children as
        // [deleted] instead of completely removing it.
        if (i == 0) {
          v.put(Comments.COLUMN_AUTHOR, Comments.DELETED_AUTHOR);
          return false;
        } else if (CommentLogic.hasChildren(this, i)) {
          v.put(Comments.COLUMN_AUTHOR, Comments.DELETED_AUTHOR);
          v.put(Comments.COLUMN_BODY, Comments.DELETED_BODY);
          return false;
        } else {
          values.remove(i);
          size--;
          return true;
        }
      }
    }
    return false;
  }

  private void editThing(long actionId, String actionThingId, String text) {
    ContentValues v = valueMap.get(actionThingId);
    if (v != null) {
      v.put(Comments.COLUMN_BODY, text);
      v.put(Comments.COLUMN_COMMENT_ACTION_ID, actionId);
    }
  }

  private void doFinalMerge() {
    int count = values.size();
    for (int i = 0; i < count; i++) {
      ContentValues v = values.get(i);

      // Remove any load more rows. We don't support them yet.
      if (isLoadingMore(v)) {
        values.remove(i--);
        count--;
        continue;
      }

      SaveMerger.updateContentValues(v, saveActionMap);
      VoteMerger.updateContentValues(v, voteActionMap);
      applySequenceNumber(v, i);
    }
  }

  private boolean isLoadingMore(ContentValues v) {
    return ((Integer) v.get(Comments.COLUMN_KIND)) == Kinds.KIND_MORE;
  }

  private void applySequenceNumber(ContentValues v, int sequence) {
    v.put(Comments.COLUMN_SEQUENCE, sequence);
  }

  @Override
  public int getCommentCount() {
    return values.size();
  }

  @Override
  public long getCommentId(int position) {
    // Cast to avoid auto-boxing in the getAsLong method.
    return ((Long) values.get(position).get(Comments._ID));
  }

  @Override
  public int getCommentNesting(int position) {
    // Cast to avoid auto-boxing in the getAsInteger method.
    return ((Integer) values.get(position).get(Comments.COLUMN_NESTING));
  }

  @Override
  public int getCommentSequence(int position) {
    // Cast to avoid auto-boxing in the getAsInteger method.
    return ((Integer) values.get(position).get(Comments.COLUMN_SEQUENCE));
  }
}