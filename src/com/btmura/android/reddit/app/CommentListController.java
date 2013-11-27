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
import android.view.View;
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
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.ThingView.OnThingViewClickListener;

/**
 * Controller that handles all the logic required by {@link CommentListFragment}.
 */
class CommentListController implements Controller<CommentAdapter>, Filterable, CommentList {

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

    CommentListController(Context context, Bundle args, OnThingViewClickListener listener) {
        this.context = context;
        this.accountName = getAccountNameExtra(args);
        this.thingId = getThingIdExtra(args);
        this.linkId = getLinkIdExtra(args);
        this.adapter = new CommentAdapter(context, accountName, listener);
        restoreInstanceState(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        int defFilter = AccountPrefs.getLastCommentFilter(context, FilterAdapter.COMMENTS_BEST);
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
        return new CommentLoader(context, accountName, thingId, linkId, filter, cursorExtras);
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

    public void author(int position) {
        MenuHelper.startProfileActivity(context, getAuthor(position), -1);
    }

    public void copyUrl(int position) {
        MenuHelper.copyUrl(context, getCommentLabel(position), getCommentUrl(position));
    }

    private CharSequence getCommentLabel(int position) {
        String label = position != 0 ? getBody(position) : getTitle(0);
        return Strings.ellipsize(label, 50);
    }

    private CharSequence getCommentUrl(int position) {
        String permaLink = getPermaLink(0);
        String thingId = position != 0 ? getThingId(position) : null;
        return Urls.perma(permaLink, thingId);
    }

    public void delete(ListView listView) {
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        long[] checkedItemIds = listView.getCheckedItemIds();

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

    private void fillCheckedInfo(SparseBooleanArray checkedPositions, String[] checkedThingIds,
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

    public void edit(int position) {
        String title = Strings.toString(getCommentLabel(position));
        String text = getBody(position);
        String parentThingId = getThingId(0);
        String thingId = getThingId(position);

        switch (position) {
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

    public void expandOrCollapse(ListView listView, View view, int position, long id) {
        if (position == 0) {
            return; // Don't allow expanding or collapsing on the header comment.
        }

        if (isExpanded(position)) {
            long[] childIds = CommentLogic.getChildren(this, position);
            Provider.collapseCommentAsync(context, id, childIds);
        } else {
            long sessionId = getSessionId(position);
            Provider.expandCommentAsync(context, id, sessionId);
        }
    }

    public void reply(int position) {
        String messageDestination = getAuthor(position);
        String title = Strings.toString(getCommentLabel(position));
        String parentThingId = getThingId(0);
        String thingId = getThingId(position);
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

    public void vote(int action, int position) {
        // Store additional information when the user votes on the header comment which represents
        // the overall thing so that it appears in the liked and disliked listing when the vote is
        // still pending.
        ThingBundle thingBundle = position == 0 ? getThingBundle(position) : null;
        Provider.voteAsync(context, accountName, action, getThingId(position), thingBundle);
    }

    private ThingBundle getThingBundle(int position) {
        return ThingBundle.newLinkInstance(getAuthor(position),
                getCreatedUtc(position),
                getDomain(position),
                getDowns(position),
                getLikes(position),
                getKind(position),
                getNumComments(position),
                isOver18(position),
                getPermaLink(position),
                getSaved(position),
                getScore(position),
                isSelf(position),
                getSubreddit(position),
                getThingId(position),
                getThumbnailUrl(position),
                getTitle(position),
                getUps(position),
                getUrl(position));
    }

    // Menu preparation methods.

    public void prepareActionMenu(Menu menu, ListView listView, int position) {
        prepareAuthorActionItem(menu, listView, position);
        prepareCopyUrlActionItem(menu, listView, position);
        prepareDeleteActionItem(menu, listView, position);
        prepareEditActionItem(menu, listView, position);
        prepareReplyActionItem(menu, listView, position);
        prepareShareActionItem(menu, listView, position);
    }

    private void prepareAuthorActionItem(Menu menu, ListView listView, int position) {
        String author = getAuthor(position);
        MenuItem item = menu.findItem(R.id.menu_author);
        item.setVisible(MenuHelper.isUserItemVisible(author) && isCheckedCount(listView, 1));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getUserTitle(context, author));
        }
    }

    private void prepareCopyUrlActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_copy_url);
        item.setVisible(isCheckedCount(listView, 1) && hasThingId(position));
    }

    private void prepareDeleteActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_delete);
        item.setVisible(hasAccount() && isAllDeletable(listView));
    }

    private void prepareEditActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_edit);
        item.setVisible(isCheckedCount(listView, 1)
                && hasAccount()
                && hasThingId(position)
                && isEditable(position));
    }

    private void prepareReplyActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_reply);
        item.setVisible(isCheckedCount(listView, 1)
                && hasAccount()
                && hasThingId(position)
                && isNotDeleted(position));
    }

    private void prepareShareActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_share_comment);
        item.setVisible(isCheckedCount(listView, 1) && hasThingId(position));
        if (item.isVisible()) {
            MenuHelper.setShareProvider(item,
                    Strings.toString(getCommentLabel(position)),
                    getCommentUrl(position));
        }
    }

    // More complicated getters.

    private boolean hasAccount() {
        return AccountUtils.isAccount(accountName);
    }

    private boolean hasThingId(int position) {
        return !TextUtils.isEmpty(getThingId(position));
    }

    private boolean isAllDeletable(ListView listView) {
        SparseBooleanArray checked = listView.getCheckedItemPositions();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (checked.get(i) && (!isAuthor(i) || !hasThingId(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isAuthor(int position) {
        return accountName.equals(getAuthor(position));
    }

    private boolean isCheckedCount(ListView listView, int checkedItemCount) {
        return listView.getCheckedItemCount() == checkedItemCount;
    }

    private boolean isEditable(int position) {
        return isAuthor(position) && (position != 0 || isSelf(0));

    }

    private boolean isNotDeleted(int position) {
        return !Things.DELETED_AUTHOR.equals(getAuthor(position));
    }

    // Getters for comment attributes.

    private String getAuthor(int position) {
        return adapter.getString(position, CommentLoader.INDEX_AUTHOR);
    }

    private String getBody(int position) {
        return adapter.getString(position, CommentLoader.INDEX_BODY);
    }

    private long getCreatedUtc(int position) {
        return adapter.getLong(position, CommentLoader.INDEX_CREATED_UTC);
    }

    private String getDomain(int position) {
        return adapter.getString(position, CommentLoader.INDEX_DOMAIN);
    }

    private int getDowns(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_DOWNS);
    }

    private int getKind(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_KIND);
    }

    private int getLikes(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_LIKES);
    }

    private long getId(int position) {
        return adapter.getLong(position, CommentLoader.INDEX_ID);
    }

    private int getNesting(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_NESTING);
    }

    private int getNumComments(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_NUM_COMMENTS);
    }

    private String getPermaLink(int position) {
        return adapter.getString(position, CommentLoader.INDEX_PERMA_LINK);
    }

    private boolean getSaved(int position) {
        return adapter.getBoolean(position, CommentLoader.INDEX_SAVED);
    }

    private int getScore(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_SCORE);
    }

    private int getSequence(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_SEQUENCE);
    }

    private long getSessionId(int position) {
        return adapter.getLong(position, CommentLoader.INDEX_SESSION_ID);
    }

    private String getSubreddit(int position) {
        return adapter.getString(position, CommentLoader.INDEX_SUBREDDIT);
    }

    private String getThingId(int position) {
        return adapter.getString(position, CommentLoader.INDEX_THING_ID);
    }

    private String getThumbnailUrl(int position) {
        return adapter.getString(position, CommentLoader.INDEX_THUMBNAIL_URL);
    }

    private String getTitle(int position) {
        return adapter.getString(position, CommentLoader.INDEX_TITLE);
    }

    private int getUps(int position) {
        return adapter.getInt(position, CommentLoader.INDEX_UPS);
    }

    private String getUrl(int position) {
        return adapter.getString(position, CommentLoader.INDEX_URL);
    }

    private boolean isExpanded(int position) {
        return adapter.getBoolean(position, CommentLoader.INDEX_EXPANDED);
    }

    private boolean isOver18(int position) {
        return adapter.getBoolean(position, CommentLoader.INDEX_OVER_18);
    }

    private boolean isSelf(int position) {
        return adapter.getBoolean(position, CommentLoader.INDEX_SELF);
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
    public long getCommentId(int position) {
        return getId(position);
    }

    @Override
    public int getCommentNesting(int position) {
        return getNesting(position);
    }

    @Override
    public int getCommentSequence(int position) {
        return getSequence(position);
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
