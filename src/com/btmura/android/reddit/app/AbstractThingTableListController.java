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
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.ThingProjection;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.ListViewUtils;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingListAdapter;

abstract class AbstractThingTableListController
        implements ThingListController<ThingListAdapter>, ThingProjection {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_PARENT_SUBREDDIT = "parentSubreddit";
    static final String EXTRA_SUBREDDIT = "subreddit";
    static final String EXTRA_FILTER = "filter";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";
    static final String EXTRA_SELECTED_LINK_ID = "selectedLinkId";
    static final String EXTRA_SELECTED_THING_ID = "selectedThingId";
    static final String EXTRA_SESSION_ID = "sessionId";
    static final String EXTRA_EMPTY_TEXT = "emptyText";

    protected final Context context;
    protected final ThingListAdapter adapter;

    private String accountName;
    private int emptyText;
    private int filter;
    private String moreId;
    private long sessionId;

    AbstractThingTableListController(Context context, Bundle args, OnVoteListener listener) {
        this.context = context;
        this.adapter = new ThingListAdapter(context, listener, getSingleChoiceExtra(args));
        restoreInstanceState(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        setAccountName(getAccountNameExtra(savedInstanceState));
        setParentSubreddit(getParentSubredditExtra(savedInstanceState));
        setSubreddit(getSubredditExtra(savedInstanceState));
        setFilter(getFilterExtra(savedInstanceState));
        setEmptyText(getEmptyTextExtra(savedInstanceState));
        setSelectedThing(getSelectedThingId(savedInstanceState),
                getSelectedLinkId(savedInstanceState));
        this.sessionId = getSessionIdExtra(savedInstanceState);
    }

    @Override
    public void saveInstanceState(Bundle state) {
        state.putString(EXTRA_ACCOUNT_NAME, getAccountName());
        state.putInt(EXTRA_EMPTY_TEXT, getEmptyText());
        state.putInt(EXTRA_FILTER, getFilter());
        state.putString(EXTRA_PARENT_SUBREDDIT, getParentSubreddit());
        state.putString(EXTRA_SELECTED_LINK_ID, getSelectedLinkId());
        state.putString(EXTRA_SELECTED_THING_ID, getSelectedThingId());
        state.putLong(EXTRA_SESSION_ID, getSessionId());
        state.putString(EXTRA_SUBREDDIT, getSubreddit());
    }

    @Override
    public void swapCursor(Cursor cursor) {
        setMoreId(null);
        adapter.swapCursor(cursor);
        if (cursor != null && cursor.getExtras() != null) {
            Bundle extras = cursor.getExtras();
            setSessionId(extras.getLong(ThingProvider.EXTRA_SESSION_ID));
        }
    }

    @Override
    public ThingBundle getThingBundle(int position) {
        return adapter.getThingBundle(position);
    }

    // Actions to be done on things.

    @Override
    public void author(int position) {
        MenuHelper.startProfileActivity(context, getAuthor(position), -1);
    }

    @Override
    public void copyUrl(int position) {
        MenuHelper.setClipAndToast(context, getThingTitle(position), getThingUrl(position));
    }

    private String getThingTitle(int position) {
        // Link and comment posts have a title.
        String title = getTitle(position);
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        // CommentActions don't have titles so use the body.
        return getBody(position);
    }

    private CharSequence getThingUrl(int position) {
        // Most things and comments have the url attribute set.
        String url = getUrl(position);
        if (!TextUtils.isEmpty(url)) {
            return url;
        }

        // Comment references just provide a thing and link id.
        String thingId = getThingId(position);
        String linkId = getLinkId(position);
        return Urls.commentListing(thingId, linkId, -1, Urls.TYPE_HTML);
    }

    @Override
    public void hide(int position, boolean hide) {
        if (hide) {
            Provider.hideAsync(context, accountName,
                    getAuthor(position),
                    getCreatedUtc(position),
                    getDomain(position),
                    getDowns(position),
                    getLikes(position),
                    getNumComments(position),
                    isOver18(position),
                    getPermaLink(position),
                    getScore(position),
                    isSelf(position),
                    getSubreddit(position),
                    getThingId(position),
                    getThumbnailUrl(position),
                    getTitle(position),
                    getUps(position),
                    getUrl(position));
        } else {
            Provider.unhideAsync(context, accountName, getThingId(position));
        }
    }

    @Override
    public void save(int position, boolean save) {
        if (save) {
            Provider.saveAsync(context, accountName,
                    getAuthor(position),
                    getCreatedUtc(position),
                    getDomain(position),
                    getDowns(position),
                    getLikes(position),
                    getNumComments(position),
                    isOver18(position),
                    getPermaLink(position),
                    getScore(position),
                    isSelf(position),
                    getSubreddit(position),
                    getThingId(position),
                    getThumbnailUrl(position),
                    getTitle(position),
                    getUps(position),
                    getUrl(position));
        } else {
            Provider.unsaveAsync(context, accountName, getThingId(position));
        }
    }

    public void select(int position) {
        // TODO: What was this for?
    }

    @Override
    public void subreddit(int position) {
        MenuHelper.startSidebarActivity(context, getSubreddit(position));
    }

    @Override
    public void vote(int position, int action) {
        Provider.voteAsync(context, accountName, action,
                getAuthor(position),
                getCreatedUtc(position),
                getDomain(position),
                getDowns(position),
                getLikes(position),
                getNumComments(position),
                isOver18(position),
                getPermaLink(position),
                getScore(position),
                isSelf(position),
                getSubreddit(position),
                getThingId(position),
                getThumbnailUrl(position),
                getTitle(position),
                getUps(position),
                getUrl(position));
    }

    // Getters.

    @Override
    public String getNextMoreId() {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Kinds.KIND_MORE) {
                return c.getString(INDEX_THING_ID);
            }
        }
        return null;
    }

    @Override
    public boolean hasAccountName() {
        return !TextUtils.isEmpty(getAccountName());
    }

    @Override
    public boolean hasCursor() {
        return adapter.getCursor() != null;
    }

    @Override
    public boolean hasNextMoreId() {
        return !TextUtils.isEmpty(getNextMoreId());
    }

    @Override
    public boolean isSingleChoice() {
        return adapter.isSingleChoice();
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return isHidable(context, position, true);
    }

    // Menu preparation methods.

    @Override
    public void onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        int position = ListViewUtils.getFirstCheckedPosition(listView);

        if (count == 1 && !TextUtils.isEmpty(getDomain(position))) {
            mode.setTitle(getDomain(position));
        } else {
            mode.setTitle(context.getResources().getQuantityString(R.plurals.things, count, count));
        }

        prepareAuthorActionItem(menu, listView, position);
        prepareCopyUrlActionItem(menu, listView, position);
        prepareHideActionItems(menu, listView, position);
        prepareSaveActionItems(menu, listView, position);
        prepareSubredditActionItem(menu, listView, position);
        prepareShareActionItem(menu, listView, position);
    }

    private void prepareAuthorActionItem(Menu menu, ListView listView, int position) {
        String author = getAuthor(position);
        MenuItem item = menu.findItem(R.id.menu_author);
        item.setVisible(isCheckedCount(listView, 1) && MenuHelper.isUserItemVisible(author));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getUserTitle(context, author));
        }
    }

    private void prepareCopyUrlActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_copy_url);
        item.setVisible(isCheckedCount(listView, 1) && hasThingId(position));
    }

    private void prepareHideActionItems(Menu menu, ListView listView, int position) {
        menu.findItem(R.id.menu_hide)
                .setVisible(isCheckedCount(listView, 1) && isHidable(context, position, true));
        menu.findItem(R.id.menu_unhide)
                .setVisible(isCheckedCount(listView, 1) && isHidable(context, position, false));
    }

    private void prepareSaveActionItems(Menu menu, ListView listView, int position) {
        boolean saveable = isCheckedCount(listView, 1) && hasAccount()
                && isKind(position, Kinds.KIND_LINK);
        boolean saved = isCheckedCount(listView, 1) && hasAccount()
                && isSaved(position);
        menu.findItem(R.id.menu_saved).setVisible(saveable && saved);
        menu.findItem(R.id.menu_unsaved).setVisible(saveable && !saved);
    }

    private void prepareShareActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_share_thing);
        item.setVisible(isCheckedCount(listView, 1));
        if (item.isVisible()) {
            String title = getThingTitle(position);
            CharSequence url = getThingUrl(position);
            MenuHelper.setShareProvider(item, title, url);
        }
    }

    private void prepareSubredditActionItem(Menu menu, ListView listView, int position) {
        String subreddit = getSubreddit(position);
        MenuItem item = menu.findItem(R.id.menu_subreddit);
        item.setVisible(isCheckedCount(listView, 1) && Subreddits.hasSidebar(subreddit));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getSubredditTitle(context, subreddit));
        }
    }

    // More complicated getters.

    private boolean hasAccount() {
        return AccountUtils.isAccount(accountName);
    }

    private boolean hasThingId(int position) {
        return !TextUtils.isEmpty(getThingId(position));
    }

    private boolean isCheckedCount(ListView listView, int checkedItemCount) {
        return listView.getCheckedItemCount() == checkedItemCount;
    }

    private boolean isHidable(Context context, int position, boolean hide) {
        return hasAccount()
                && isKind(position, Kinds.KIND_LINK)
                && (hide && isUnhidden(position) || !hide && isHidden(position));
    }

    private boolean isHidden(int position) {
        return !adapter.isNull(position, INDEX_HIDE_ACTION)
                && adapter.getInt(position, INDEX_HIDE_ACTION) == HideActions.ACTION_HIDE
                || adapter.getBoolean(position, INDEX_HIDDEN);
    }

    private boolean isKind(int position, int kind) {
        return getKind(position) == kind;
    }

    private boolean isSaved(int position) {
        // If no local save actions are pending, then rely on server info.
        if (adapter.isNull(position, INDEX_SAVE_ACTION)) {
            return adapter.getBoolean(position, INDEX_SAVED);
        }

        // We have a local pending action so use that to indicate if it's read.
        return adapter.getInt(position, INDEX_SAVE_ACTION) == SaveActions.ACTION_SAVE;
    }

    private boolean isUnhidden(int position) {
        return !adapter.isNull(position, INDEX_HIDE_ACTION)
                && adapter.getInt(position, INDEX_HIDE_ACTION) == HideActions.ACTION_UNHIDE
                || !adapter.getBoolean(position, INDEX_HIDDEN);
    }

    // Simple getters for state members.

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public ThingListAdapter getAdapter() {
        return adapter;
    }

    @Override
    public int getEmptyText() {
        return emptyText;
    }

    @Override
    public int getFilter() {
        return filter;
    }

    @Override
    public String getMoreId() {
        return moreId;
    }

    @Override
    public String getParentSubreddit() {
        return adapter.getParentSubreddit();
    }

    // TODO: Remove this method.
    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSelectedLinkId() {
        return adapter.getSelectedLinkId();
    }

    @Override
    public String getSelectedThingId() {
        return adapter.getSelectedThingId();
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public String getSubreddit() {
        return adapter.getSubreddit();
    }

    @Override
    public boolean hasQuery() {
        return !TextUtils.isEmpty(getQuery());
    }

    // Simple setters for state members.

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
        adapter.setAccountName(accountName);
    }

    @Override
    public void setEmptyText(int emptyText) {
        this.emptyText = emptyText;
    }

    @Override
    public void setFilter(int filter) {
        this.filter = filter;
    }

    @Override
    public void setMoreId(String moreId) {
        this.moreId = moreId;
    }

    @Override
    public void setParentSubreddit(String parentSubreddit) {
        adapter.setParentSubreddit(parentSubreddit);
    }

    @Override
    public void setSelectedPosition(int position) {
        adapter.setSelectedPosition(position);
    }

    @Override
    public void setSelectedThing(String thingId, String linkId) {
        adapter.setSelectedThing(thingId, linkId);
    }

    @Override
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void setSubreddit(String subreddit) {
        adapter.setSubreddit(subreddit);
    }

    @Override
    public void setThingBodyWidth(int thingBodyWidth) {
        adapter.setThingBodyWidth(thingBodyWidth);
    }

    // Getters for thing attributes.

    private String getAuthor(int position) {
        return adapter.getString(position, INDEX_AUTHOR);
    }

    private String getBody(int position) {
        return adapter.getString(position, INDEX_BODY);
    }

    private long getCreatedUtc(int position) {
        return adapter.getLong(position, INDEX_CREATED_UTC);
    }

    private String getDomain(int position) {
        return adapter.getString(position, INDEX_DOMAIN);
    }

    private int getDowns(int position) {
        return adapter.getInt(position, INDEX_DOWNS);
    }

    private int getKind(int position) {
        return adapter.getInt(position, INDEX_KIND);
    }

    private int getLikes(int position) {
        return adapter.getInt(position, INDEX_LIKES);
    }

    private String getLinkId(int position) {
        return adapter.getString(position, INDEX_LINK_ID);
    }

    private int getNumComments(int position) {
        return adapter.getInt(position, INDEX_NUM_COMMENTS);
    }

    private String getPermaLink(int position) {
        return adapter.getString(position, INDEX_PERMA_LINK);
    }

    private int getScore(int position) {
        return adapter.getInt(position, INDEX_SCORE);
    }

    private String getSubreddit(int position) {
        return adapter.getString(position, INDEX_SUBREDDIT);
    }

    private String getThingId(int position) {
        return adapter.getString(position, INDEX_THING_ID);
    }

    private String getThumbnailUrl(int position) {
        return adapter.getString(position, INDEX_THUMBNAIL_URL);
    }

    private String getTitle(int position) {
        return adapter.getString(position, INDEX_TITLE);
    }

    private int getUps(int position) {
        return adapter.getInt(position, INDEX_UPS);
    }

    private String getUrl(int position) {
        return adapter.getString(position, INDEX_URL);
    }

    private boolean isOver18(int position) {
        return adapter.getBoolean(position, INDEX_OVER_18);
    }

    private boolean isSelf(int position) {
        return adapter.getBoolean(position, INDEX_SELF);
    }

    // Getters for extras

    private static String getAccountNameExtra(Bundle extras) {
        return extras.getString(EXTRA_ACCOUNT_NAME);
    }

    private static String getParentSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_PARENT_SUBREDDIT);
    }

    private static String getSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_SUBREDDIT);
    }

    private static int getFilterExtra(Bundle extras) {
        return extras.getInt(EXTRA_FILTER);
    }

    private static boolean getSingleChoiceExtra(Bundle extras) {
        return extras.getBoolean(EXTRA_SINGLE_CHOICE);
    }

    private static String getSelectedThingId(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_THING_ID);
    }

    private static String getSelectedLinkId(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_LINK_ID);
    }

    private static long getSessionIdExtra(Bundle extras) {
        return extras.getLong(EXTRA_SESSION_ID);
    }

    private static int getEmptyTextExtra(Bundle extras) {
        return extras.getInt(EXTRA_EMPTY_TEXT);
    }
}
