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

package com.btmura.android.reddit.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.CommentLogic.CommentList;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.CommentLoader;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Strings;
import com.btmura.android.reddit.widget.CommentAdapter;
import com.btmura.android.reddit.widget.ThingView.OnThingViewClickListener;

/**
 * Controller that handles all the logic required by {@link
 * CommentListFragment}.
 */
class CommentListController
    implements Controller<CommentAdapter>, Filterable, CommentList {

  static final String EXTRA_ACCOUNT_NAME = "accountName";
  static final String EXTRA_THING_ID = "thingId";
  static final String EXTRA_LINK_ID = "linkId";
  static final String EXTRA_FILTER = "filter";
  static final String EXTRA_CURSOR_EXTRAS = "cursorExtras";

  private final Context context;
  private final String accountName;
  private final String thingId;
  private final String linkId;
  private final CommentAdapter adapter;
  private int filter;
  private Bundle cursorExtras;

  CommentListController(
      Context context,
      Bundle args,
      OnThingViewClickListener listener) {
    this.context = context;
    this.accountName = getAccountNameExtra(args);
    this.thingId = getThingIdExtra(args);
    this.linkId = getLinkIdExtra(args);
    this.adapter = new CommentAdapter(context, accountName, listener);
    restoreInstanceState(args);
  }

  @Override
  public void restoreInstanceState(Bundle savedInstanceState) {
    int defFilter =
        AccountPrefs.getLastCommentFilter(context, Filter.COMMENTS_BEST);
    this.filter = savedInstanceState.getInt(EXTRA_FILTER, defFilter);
    this.cursorExtras = savedInstanceState.getBundle(EXTRA_CURSOR_EXTRAS);
  }

  @Override
  public void saveInstanceState(Bundle outState) {
    outState.putInt(EXTRA_FILTER, filter);
    outState.putBundle(EXTRA_CURSOR_EXTRAS, cursorExtras);
  }

  @Override
  public Loader<Cursor> createLoader() {
    return new CommentLoader(context, accountName, thingId, linkId, filter,
        cursorExtras);
  }

  @Override
  public void swapCursor(Cursor cursor) {
    adapter.swapCursor(cursor);
    cursorExtras = cursor != null ? cursor.getExtras() : null;
  }

  @Override
  public CommentAdapter getAdapter() {
    return adapter;
  }

  // Actions to be done on comments.

  public void author(int pos) {
    MenuHelper.startProfileActivity(context, getAuthor(pos));
  }

  public void copyUrl(int pos) {
    MenuHelper.copyUrl(context, getCommentLabel(pos), getCommentUrl(pos));
  }

  private CharSequence getCommentLabel(int pos) {
    String label = pos != 0 ? getBody(pos) : getTitle(0);
    return Strings.ellipsize(label, 50);
  }

  private CharSequence getCommentUrl(int pos) {
    String permaLink = getPermaLink(0);
    String thingId = pos != 0 ? getThingId(pos) : null;
    return Urls.permaLink(permaLink, thingId);
  }

  public void delete(ListView lv) {
    SparseBooleanArray checkedItemPositions = lv.getCheckedItemPositions();
    long[] checkedItemIds = lv.getCheckedItemIds();

    String[] checkedThingIds = new String[checkedItemIds.length];
    boolean[] hasChildren = new boolean[checkedItemIds.length];
    fillCheckedInfo(checkedItemPositions, checkedThingIds, hasChildren);

    Provider.deleteCommentAsync(context,
        accountName,
        hasChildren,
        checkedItemIds,
        getThingId(0),
        checkedThingIds);
  }

  private void fillCheckedInfo(
      SparseBooleanArray checkedPositions,
      String[] checkedThingIds,
      boolean[] hasChildren) {
    int count = adapter.getCount();
    int j = 0;
    for (int i = 0; i < count; i++) {
      if (checkedPositions.get(i)) {
        checkedThingIds[j] = getThingId(i);
        hasChildren[j] = CommentLogic.hasChildren(this, i);
        j++;
      }
    }
  }

  public void edit(int pos) {
    String title = Strings.toString(getCommentLabel(pos));
    String text = getBody(pos);
    String parentThingId = getThingId(0);
    String thingId = getThingId(pos);

    switch (pos) {
      case 0:
        MenuHelper.startEditPostComposer(context,
            accountName,
            title,
            text,
            parentThingId,
            thingId);
        break;

      default:
        MenuHelper.startEditCommentComposer(context,
            accountName,
            title,
            text,
            parentThingId,
            thingId);
        break;
    }
  }

  public void expandOrCollapse(int pos, long id) {
    if (pos == 0) {
      return; // Don't allow expanding or collapsing on the header comment.
    }

    if (isExpanded(pos)) {
      long[] childIds = CommentLogic.getChildren(this, pos);
      Provider.collapseCommentAsync(context, id, childIds);
    } else {
      long sessionId = getSessionId(pos);
      Provider.expandCommentAsync(context, id, sessionId);
    }
  }

  public void reply(int pos) {
    String messageDestination = getAuthor(pos);
    String title = Strings.toString(getCommentLabel(pos));
    String parentThingId = getThingId(0);
    String thingId = getThingId(pos);
    MenuHelper.startCommentReplyComposer(context,
        accountName,
        messageDestination,
        title,
        parentThingId,
        thingId);
  }

  public void save(boolean save) {
    ThingBundle thingBundle = save ? getThingBundle(0) : null;
    Provider.saveAsync(context, accountName, thingId, thingBundle, save);
  }

  public void share(int pos) {
    MenuHelper.share(context, getCommentLabel(pos), getCommentUrl(pos));
  }

  public void vote(int action, int pos) {
    // Store additional information when the user votes on the header comment which represents
    // the overall thing so that it appears in the liked and disliked listing when the vote is
    // still pending.
    ThingBundle thingBundle = pos == 0 ? getThingBundle(pos) : null;
    Provider.voteAsync(context, accountName, action, getThingId(pos),
        thingBundle);
  }

  private ThingBundle getThingBundle(int pos) {
    return ThingBundle.newLinkInstance(
        getAuthor(pos),
        getCreatedUtc(pos),
        getDomain(pos),
        getDowns(pos),
        getLikes(pos),
        getKind(pos),
        getNumComments(pos),
        isOver18(pos),
        getPermaLink(pos),
        getSaved(pos),
        getScore(pos),
        isSelf(pos),
        getSubreddit(pos),
        getThingId(pos),
        getThumbnailUrl(pos),
        getTitle(pos),
        getUps(pos),
        getUrl(pos));
  }

  // Menu preparation methods.

  public void prepareActionMenu(Menu menu, ListView lv, int pos) {
    prepareAuthorActionItem(menu, lv, pos);
    prepareCopyUrlActionItem(menu, lv, pos);
    prepareDeleteActionItem(menu, lv);
    prepareEditActionItem(menu, lv, pos);
    prepareReplyActionItem(menu, lv, pos);
    prepareShareActionItem(menu, lv, pos);
  }

  private void prepareAuthorActionItem(Menu menu, ListView lv, int pos) {
    String author = getAuthor(pos);
    MenuItem item = menu.findItem(R.id.menu_author);
    item.setVisible(MenuHelper.isUserItemVisible(author)
        && isCheckedCount(lv, 1));
    if (item.isVisible()) {
      item.setTitle(MenuHelper.getUserTitle(context, author));
    }
  }

  private void prepareCopyUrlActionItem(Menu menu, ListView lv, int pos) {
    MenuItem item = menu.findItem(R.id.menu_copy_url);
    item.setVisible(isCheckedCount(lv, 1) && hasThingId(pos));
  }

  private void prepareDeleteActionItem(Menu menu, ListView lv) {
    MenuItem item = menu.findItem(R.id.menu_delete);
    item.setVisible(hasAccount() && isAllDeletable(lv));
  }

  private void prepareEditActionItem(Menu menu, ListView lv, int pos) {
    MenuItem item = menu.findItem(R.id.menu_edit);
    item.setVisible(isCheckedCount(lv, 1)
        && hasAccount()
        && hasThingId(pos)
        && isEditable(pos));
  }

  private void prepareReplyActionItem(Menu menu, ListView lv, int pos) {
    MenuItem item = menu.findItem(R.id.menu_reply);
    item.setVisible(isCheckedCount(lv, 1)
        && hasAccount()
        && hasThingId(pos)
        && isNotDeleted(pos));
  }

  private void prepareShareActionItem(Menu menu, ListView lv, int pos) {
    MenuItem item = menu.findItem(R.id.menu_share_comment);
    item.setVisible(isCheckedCount(lv, 1) && hasThingId(pos));
  }

  // More complicated getters.

  private boolean hasAccount() {
    return AccountUtils.isAccount(accountName);
  }

  private boolean hasThingId(int pos) {
    return !TextUtils.isEmpty(getThingId(pos));
  }

  private boolean isAllDeletable(ListView lv) {
    SparseBooleanArray checked = lv.getCheckedItemPositions();
    int count = adapter.getCount();
    for (int i = 0; i < count; i++) {
      if (checked.get(i) && (!isAuthor(i) || !hasThingId(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean isAuthor(int pos) {
    return accountName.equals(getAuthor(pos));
  }

  private boolean isCheckedCount(ListView lv, int checkedItemCount) {
    return lv.getCheckedItemCount() == checkedItemCount;
  }

  private boolean isEditable(int pos) {
    return isAuthor(pos) && (pos != 0 || isSelf(0));

  }

  private boolean isNotDeleted(int pos) {
    return !Things.DELETED_AUTHOR.equals(getAuthor(pos));
  }

  // Getters for comment attributes.

  private String getAuthor(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_AUTHOR);
  }

  private String getBody(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_BODY);
  }

  private long getCreatedUtc(int pos) {
    return adapter.getLong(pos, CommentLoader.INDEX_CREATED_UTC);
  }

  private String getDomain(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_DOMAIN);
  }

  private int getDowns(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_DOWNS);
  }

  private int getKind(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_KIND);
  }

  private int getLikes(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_LIKES);
  }

  private long getId(int pos) {
    return adapter.getLong(pos, CommentLoader.INDEX_ID);
  }

  private int getNesting(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_NESTING);
  }

  private int getNumComments(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_NUM_COMMENTS);
  }

  private String getPermaLink(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_PERMA_LINK);
  }

  private boolean getSaved(int pos) {
    return adapter.getBoolean(pos, CommentLoader.INDEX_SAVED);
  }

  private int getScore(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_SCORE);
  }

  private int getSequence(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_SEQUENCE);
  }

  private long getSessionId(int pos) {
    return adapter.getLong(pos, CommentLoader.INDEX_SESSION_ID);
  }

  private String getSubreddit(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_SUBREDDIT);
  }

  private String getThingId(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_THING_ID);
  }

  private String getThumbnailUrl(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_THUMBNAIL_URL);
  }

  private String getTitle(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_TITLE);
  }

  private int getUps(int pos) {
    return adapter.getInt(pos, CommentLoader.INDEX_UPS);
  }

  private String getUrl(int pos) {
    return adapter.getString(pos, CommentLoader.INDEX_URL);
  }

  private boolean isExpanded(int pos) {
    return adapter.getBoolean(pos, CommentLoader.INDEX_EXPANDED);
  }

  private boolean isOver18(int pos) {
    return adapter.getBoolean(pos, CommentLoader.INDEX_OVER_18);
  }

  private boolean isSelf(int pos) {
    return adapter.getBoolean(pos, CommentLoader.INDEX_SELF);
  }

  // Filterable implementation

  @Override
  public int getFilter() {
    return filter;
  }

  @Override
  public void setFilter(int filter) {
    this.filter = filter;
    AccountPrefs.setLastCommentFilter(context, filter);
  }

  // CommentList implementation

  @Override
  public int getCommentCount() {
    return adapter.getCount();
  }

  @Override
  public long getCommentId(int pos) {
    return getId(pos);
  }

  @Override
  public int getCommentNesting(int pos) {
    return getNesting(pos);
  }

  @Override
  public int getCommentSequence(int pos) {
    return getSequence(pos);
  }

  // Getters for extras.

  private static String getAccountNameExtra(Bundle extras) {
    return extras.getString(EXTRA_ACCOUNT_NAME);
  }

  private static String getThingIdExtra(Bundle extras) {
    return extras.getString(EXTRA_THING_ID);
  }

  private static String getLinkIdExtra(Bundle extras) {
    return extras.getString(EXTRA_LINK_ID);
  }
}
