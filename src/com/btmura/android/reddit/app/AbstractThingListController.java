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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AbstractThingLoader;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.util.StringUtil;
import com.btmura.android.reddit.widget.AbstractThingListAdapter;
import com.btmura.android.reddit.widget.ThingBundle;

abstract class AbstractThingListController implements ThingListController {

    private static final String STATE_SESSION_ID = "sessionId";

    private final Formatter formatter = new Formatter();
    private final Context context;
    private final String accountName;
    private final AbstractThingListAdapter adapter;

    protected String moreId;
    protected long sessionId;

    AbstractThingListController(Context context, String accountName,
            AbstractThingListAdapter adapter) {
        this.context = context;
        this.accountName = accountName;
        this.adapter = adapter;
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putLong(STATE_SESSION_ID, sessionId);
    }

    @Override
    public void restoreState(Bundle savedInstanceState) {
        savedInstanceState = Objects.nullToEmpty(savedInstanceState);
        sessionId = savedInstanceState.getLong(STATE_SESSION_ID);
    }

    @Override
    public Bundle getThingBundle(int position) {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToPosition(position)) {
            return makeThingBundle(c);
        }
        return null;
    }

    private Bundle makeThingBundle(Cursor c) {
        Bundle b = new Bundle(8);
        ThingBundle.putAuthor(b, c.getString(AbstractThingLoader.THING_AUTHOR));
        ThingBundle.putSubreddit(b, c.getString(AbstractThingLoader.THING_SUBREDDIT));
        ThingBundle.putKind(b, c.getInt(AbstractThingLoader.THING_KIND));

        String title = c.getString(AbstractThingLoader.THING_TITLE);
        ThingBundle.putTitle(b, !TextUtils.isEmpty(title)
                ? format(title)
                : format(c.getString(AbstractThingLoader.THING_LINK_TITLE)));

        String thingId = c.getString(AbstractThingLoader.THING_THING_ID);
        ThingBundle.putThingId(b, thingId);

        String linkId = c.getString(AbstractThingLoader.THING_LINK_ID);
        ThingBundle.putLinkId(b, linkId);

        boolean isSelf = c.getInt(AbstractThingLoader.THING_SELF) == 1;
        if (!isSelf) {
            ThingBundle.putLinkUrl(b, c.getString(AbstractThingLoader.THING_URL));
        }

        String permaLink = c.getString(AbstractThingLoader.THING_PERMA_LINK);
        if (!TextUtils.isEmpty(permaLink)) {
            ThingBundle.putCommentUrl(b, Urls.perma(permaLink, null));
        }

        return b;
    }

    private String format(String text) {
        return StringUtil.safeString(formatter.formatAll(context, text));
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

    @Override
    public void select(int position) {
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
            if (c.getInt(AbstractThingLoader.THING_KIND) == Kinds.KIND_MORE) {
                return c.getString(AbstractThingLoader.THING_THING_ID);
            }
        }
        return null;
    }

    @Override
    public boolean hasNextMoreId() {
        return !TextUtils.isEmpty(getNextMoreId());
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return isHidable(context, position, true);
    }

    // Menu preparation methods.

    @Override
    public void prepareActionMenu(Menu menu, ListView listView, int position) {
        prepareAuthorActionItem(menu, listView, position);
        prepareCommentsActionItem(menu, listView, position);
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

    private void prepareCommentsActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_comments);
        item.setVisible(isCheckedCount(listView, 1) && isKind(position, Kinds.KIND_LINK));
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
        return !adapter.isNull(position, AbstractThingLoader.THING_LOCAL_HIDDEN)
                && adapter.getInt(position, AbstractThingLoader.THING_LOCAL_HIDDEN) == HideActions.ACTION_HIDE
                || adapter.getBoolean(position, AbstractThingLoader.THING_HIDDEN);
    }

    private boolean isKind(int position, int kind) {
        return getKind(position) == kind;
    }

    private boolean isSaved(int position) {
        // If no local save actions are pending, then rely on server info.
        if (adapter.isNull(position, AbstractThingLoader.THING_SAVE_ACTION)) {
            return adapter.getBoolean(position, AbstractThingLoader.THING_SAVED);
        }

        // We have a local pending action so use that to indicate if it's read.
        return adapter.getInt(position, AbstractThingLoader.THING_SAVE_ACTION) == SaveActions.ACTION_SAVE;
    }

    private boolean isUnhidden(int position) {
        return !adapter.isNull(position, AbstractThingLoader.THING_LOCAL_HIDDEN)
                && adapter.getInt(position, AbstractThingLoader.THING_LOCAL_HIDDEN) == HideActions.ACTION_UNHIDE
                || !adapter.getBoolean(position, AbstractThingLoader.THING_HIDDEN);
    }

    // Simple getters for state members.

    @Override
    public String getMoreId() {
        return moreId;
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    // Simple setters for state members.

    @Override
    public void setMoreId(String moreId) {
        this.moreId = moreId;
    }

    @Override
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    // Getters for thing attributes.

    private String getAuthor(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_AUTHOR);
    }

    private String getBody(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_BODY);
    }

    private long getCreatedUtc(int position) {
        return adapter.getLong(position, AbstractThingLoader.THING_CREATED_UTC);
    }

    private String getDomain(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_DOMAIN);
    }

    private int getDowns(int position) {
        return adapter.getInt(position, AbstractThingLoader.THING_DOWNS);
    }

    private int getKind(int position) {
        return adapter.getInt(position, AbstractThingLoader.THING_KIND);
    }

    private int getLikes(int position) {
        return adapter.getInt(position, AbstractThingLoader.THING_LIKES);
    }

    private String getLinkId(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_LINK_ID);
    }

    private int getNumComments(int position) {
        return adapter.getInt(position, AbstractThingLoader.THING_NUM_COMMENTS);
    }

    private String getPermaLink(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_PERMA_LINK);
    }

    private int getScore(int position) {
        return adapter.getInt(position, AbstractThingLoader.THING_SCORE);
    }

    private String getSubreddit(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_SUBREDDIT);
    }

    private String getThingId(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_THING_ID);
    }

    private String getThumbnailUrl(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_THUMBNAIL_URL);
    }

    private String getTitle(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_TITLE);
    }

    private int getUps(int position) {
        return adapter.getInt(position, AbstractThingLoader.THING_UPS);
    }

    private String getUrl(int position) {
        return adapter.getString(position, AbstractThingLoader.THING_URL);
    }

    private boolean isOver18(int position) {
        return adapter.getBoolean(position, AbstractThingLoader.THING_OVER_18);
    }

    private boolean isSelf(int position) {
        return adapter.getBoolean(position, AbstractThingLoader.THING_SELF);
    }
}
