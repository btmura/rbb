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
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.ThingProjection;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Views;
import com.btmura.android.reddit.widget.AbstractThingListAdapter;

class ThingTableActionModeController
    implements ThingActionModeController, ThingProjection {

  private final Context ctx;
  private final String accountName;
  private final int swipeAction;
  private final AbstractThingListAdapter adapter;
  private ActionMode actionMode;

  ThingTableActionModeController(
      Context ctx,
      String accountName,
      int swipeAction,
      AbstractThingListAdapter adapter) {
    this.ctx = ctx;
    this.accountName = accountName;
    this.swipeAction = swipeAction;
    this.adapter = adapter;
  }

  @Override
  public void invalidateActionMode() {
    if (actionMode != null) {
      actionMode.invalidate();
    }
  }

  @Override
  public boolean onCreateActionMode(
      ActionMode mode,
      Menu menu,
      ListView listView) {
    if (adapter.getCursor() == null) {
      listView.clearChoices();
      return false;
    }
    actionMode = mode;

    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.thing_action_menu, menu);
    return true;
  }

  @Override
  public boolean onPrepareActionMode(
      ActionMode mode,
      Menu menu,
      ListView lv) {
    int count = lv.getCheckedItemCount();
    int position = Views.getCheckedPosition(lv);

    if (count == 1 && !TextUtils.isEmpty(getDomain(position))) {
      mode.setTitle(getDomain(position));
    } else {
      mode.setTitle(ctx.getResources()
          .getQuantityString(R.plurals.things, count, count));
    }

    prepareAuthorActionItem(menu, lv, position);
    prepareCopyUrlActionItem(menu, lv, position);
    prepareHideActionItems(menu, lv, position);
    prepareSaveActionItems(menu, lv, position);
    prepareSubredditActionItem(menu, lv, position);
    prepareShareActionItem(menu, lv);
    return true;
  }

  private void prepareAuthorActionItem(Menu menu, ListView lv, int pos) {
    String author = getAuthor(pos);
    MenuItem item = menu.findItem(R.id.menu_author);
    item.setVisible(isCheckedCount(lv, 1)
        && MenuHelper.isUserItemVisible(author));
    if (item.isVisible()) {
      item.setTitle(MenuHelper.getUserTitle(ctx, author));
    }
  }

  private void prepareCopyUrlActionItem(Menu menu, ListView lv, int pos) {
    MenuItem item = menu.findItem(R.id.menu_copy_url);
    item.setVisible(isCheckedCount(lv, 1) && hasThingId(pos));
  }

  private void prepareHideActionItems(Menu menu, ListView lv, int pos) {
    menu.findItem(R.id.menu_hide)
        .setVisible(isCheckedCount(lv, 1) && isHidable(pos, true));
    menu.findItem(R.id.menu_unhide)
        .setVisible(isCheckedCount(lv, 1) && isHidable(pos, false));
  }

  private boolean isHidable(int pos, boolean hide) {
    return hasAccount()
        && actionMode == null
        && isKind(pos, Kinds.KIND_LINK)
        && (hide && !isHidden(pos) || !hide && isHidden(pos));
  }

  private boolean isHidden(int pos) {
    return adapter.getBoolean(pos, INDEX_HIDDEN);
  }

  private void prepareSaveActionItems(Menu menu, ListView lv, int pos) {
    boolean saveable = isCheckedCount(lv, 1) && hasAccount()
        && isKind(pos, Kinds.KIND_LINK);
    boolean saved = isCheckedCount(lv, 1) && hasAccount() && isSaved(pos);
    menu.findItem(R.id.menu_saved).setVisible(saveable && saved);
    menu.findItem(R.id.menu_unsaved).setVisible(saveable && !saved);
  }

  private boolean isSaved(int position) {
    return adapter.getBoolean(position, INDEX_SAVED);
  }

  private void prepareShareActionItem(Menu menu, ListView lv) {
    MenuItem item = menu.findItem(R.id.menu_share_thing);
    item.setVisible(isCheckedCount(lv, 1));
  }

  private String getThingTitle(int pos) {
    // Link and comment posts have a title.
    String title = getTitle(pos);
    if (!TextUtils.isEmpty(title)) {
      return title;
    }

    // CommentActions don't have titles so use the body.
    return getBody(pos);
  }

  private CharSequence getThingUrl(int pos) {
    // Most things and comments have the url attribute set.
    String url = getUrl(pos);
    if (!TextUtils.isEmpty(url)) {
      return url;
    }

    // Comment references just provide a thing and link id.
    return Urls.commentsLink(getThingId(pos), getLinkId(pos));
  }

  private void prepareSubredditActionItem(Menu menu, ListView lv, int pos) {
    String subreddit = getSubreddit(pos);
    MenuItem item = menu.findItem(R.id.menu_subreddit);
    item.setVisible(isCheckedCount(lv, 1) && Subreddits.hasSidebar(subreddit));
    if (item.isVisible()) {
      item.setTitle(MenuHelper.getSubredditTitle(ctx, subreddit));
    }
  }

  @Override
  public boolean onActionItemClicked(
      ActionMode mode,
      MenuItem item,
      ListView lv) {
    switch (item.getItemId()) {
      case R.id.menu_saved:
        handleSave(lv, false);
        mode.finish();
        return true;

      case R.id.menu_unsaved:
        handleSave(lv, true);
        mode.finish();
        return true;

      case R.id.menu_hide:
        handleHide(lv, true);
        mode.finish();
        return true;

      case R.id.menu_unhide:
        handleHide(lv, false);
        mode.finish();
        return true;

      case R.id.menu_share_thing:
        handleShare(lv);
        mode.finish();
        return true;

      case R.id.menu_copy_url:
        handleCopyUrl(lv);
        mode.finish();
        return true;

      case R.id.menu_author:
        handleAuthor(lv);
        mode.finish();
        return true;

      case R.id.menu_subreddit:
        handleSubreddit(lv);
        mode.finish();
        return true;
    }
    return false;
  }

  private void handleSave(ListView lv, boolean save) {
    int pos = Views.getCheckedPosition(lv);
    save(pos, save);
  }

  private void handleHide(ListView lv, boolean hide) {
    int pos = Views.getCheckedPosition(lv);
    hide(pos, hide);
  }

  private void handleShare(ListView lv) {
    int pos = Views.getCheckedPosition(lv);
    MenuHelper.share(ctx, getThingTitle(pos), getThingUrl(pos));
  }

  private void handleCopyUrl(ListView lv) {
    int pos = Views.getCheckedPosition(lv);
    MenuHelper.copyUrl(ctx, getThingTitle(pos), getThingUrl(pos));
  }

  private void handleAuthor(ListView lv) {
    int pos = Views.getCheckedPosition(lv);
    MenuHelper.startProfileActivity(ctx, getAuthor(pos));
  }

  private void handleSubreddit(ListView lv) {
    int pos = Views.getCheckedPosition(lv);
    MenuHelper.startSidebarActivity(ctx, getSubreddit(pos));
  }

  @Override
  public void onItemCheckedStateChanged(
      ActionMode mode,
      int pos,
      long id,
      boolean checked) {
    mode.invalidate();
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    actionMode = null;
  }

  @Override
  public boolean isSwipeable(int position) {
    switch (swipeAction) {
      case ThingListController.SWIPE_ACTION_HIDE:
      case ThingListController.SWIPE_ACTION_UNHIDE:
        return isHidable(position,
            swipeAction == ThingListController.SWIPE_ACTION_HIDE);

      default:
        return false;
    }
  }

  @Override
  public void swipe(int pos) {
    hide(pos, swipeAction == ThingListController.SWIPE_ACTION_HIDE);
  }

  private void save(int pos, boolean save) {
    ThingBundle thingBundle = save ? getThingBundle(pos) : null;
    Provider.saveAsync(ctx, accountName, getThingId(pos), thingBundle, save);
  }

  private void hide(int pos, boolean hide) {
    ThingBundle thingBundle = hide ? getThingBundle(pos) : null;
    Provider.hideAsync(ctx, accountName, getThingId(pos), thingBundle, hide);
  }

  @Override
  public void vote(int pos, int action) {
    boolean isLink = getKind(pos) == Kinds.KIND_LINK;
    ThingBundle thingBundle = isLink ? getThingBundle(pos) : null;
    Provider.voteAsync(ctx, accountName, action, getThingId(pos), thingBundle);
  }

  private ThingBundle getThingBundle(int pos) {
    return ThingBundle.newLinkInstance(getAuthor(pos),
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

  // Utility methods

  private boolean hasAccount() {
    return AccountUtils.isAccount(accountName);
  }

  private boolean hasThingId(int pos) {
    return !TextUtils.isEmpty(getThingId(pos));
  }

  private boolean isCheckedCount(ListView lv, int checkedItemCount) {
    return lv.getCheckedItemCount() == checkedItemCount;
  }

  private boolean isKind(int pos, int kind) {
    return getKind(pos) == kind;
  }

  // Getters for thing attributes

  private String getAuthor(int pos) {
    return adapter.getString(pos, INDEX_AUTHOR);
  }

  private String getBody(int pos) {
    return adapter.getString(pos, INDEX_BODY);
  }

  private long getCreatedUtc(int pos) {
    return adapter.getLong(pos, INDEX_CREATED_UTC);
  }

  private String getDomain(int pos) {
    return adapter.getString(pos, INDEX_DOMAIN);
  }

  private int getDowns(int pos) {
    return adapter.getInt(pos, INDEX_DOWNS);
  }

  private int getKind(int pos) {
    return adapter.getInt(pos, INDEX_KIND);
  }

  private int getLikes(int pos) {
    return adapter.getInt(pos, INDEX_LIKES);
  }

  private String getLinkId(int pos) {
    return adapter.getString(pos, INDEX_LINK_ID);
  }

  private int getNumComments(int pos) {
    return adapter.getInt(pos, INDEX_NUM_COMMENTS);
  }

  private String getPermaLink(int pos) {
    return adapter.getString(pos, INDEX_PERMA_LINK);
  }

  private boolean getSaved(int pos) {
    return adapter.getBoolean(pos, INDEX_SAVED);
  }

  private int getScore(int pos) {
    return adapter.getInt(pos, INDEX_SCORE);
  }

  private String getSubreddit(int pos) {
    return adapter.getString(pos, INDEX_SUBREDDIT);
  }

  private String getThingId(int pos) {
    return adapter.getString(pos, INDEX_THING_ID);
  }

  private String getThumbnailUrl(int pos) {
    return adapter.getString(pos, INDEX_THUMBNAIL_URL);
  }

  private String getTitle(int pos) {
    return adapter.getString(pos, INDEX_TITLE);
  }

  private int getUps(int pos) {
    return adapter.getInt(pos, INDEX_UPS);
  }

  private String getUrl(int pos) {
    return adapter.getString(pos, INDEX_URL);
  }

  private boolean isOver18(int pos) {
    return adapter.getBoolean(pos, INDEX_OVER_18);
  }

  private boolean isSelf(int pos) {
    return adapter.getBoolean(pos, INDEX_SELF);
  }
}
