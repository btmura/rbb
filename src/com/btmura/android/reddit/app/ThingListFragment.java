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

package com.btmura.android.reddit.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.view.SwipeDismissTouchListener;
import com.btmura.android.reddit.view.SwipeDismissTouchListener.OnSwipeDismissListener;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingListAdapter;
import com.btmura.android.reddit.widget.ThingView;

public class ThingListFragment extends ThingProviderListFragment implements
        OnScrollListener, OnSwipeDismissListener, OnVoteListener, MultiChoiceModeListener {

    public static final String TAG = "ThingListFragment";

    /** String argument specifying the account being used. */
    private static final String ARG_ACCOUNT_NAME = "accountName";

    /** String argument specifying the subreddit to load. */
    private static final String ARG_SUBREDDIT = "subreddit";

    /** String argument specifying the search query to use. */
    private static final String ARG_QUERY = "query";

    /** String argument specifying the profileUser profile to load. */
    private static final String ARG_PROFILE_USER = "profileUser";

    /** String argument specifying whose messages to load. */
    private static final String ARG_MESSAGE_USER = "messageUser";

    /** Integer argument to filter things, profile, or messages. */
    private static final String ARG_FILTER = "filter";

    /** String argument that is used to paginate things. */
    private static final String LOADER_MORE_ID = "moreId";

    /** Optional bit mask for controlling fragment appearance. */
    private static final String ARG_FLAGS = "flags";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String STATE_ACCOUNT_NAME = ARG_ACCOUNT_NAME;

    private static final String STATE_PARENT_SUBREDDIT = "parentSubreddit";

    private static final String STATE_SUBREDDIT = ARG_SUBREDDIT;

    private static final String STATE_SELECTED_THING_ID = "selectedThingId";
    private static final String STATE_SELECTED_LINK_ID = "selectedLinkId";
    private static final String STATE_EMPTY_TEXT = "emptyText";

    public interface OnThingSelectedListener {
        void onThingSelected(View view, Bundle thingBundle, int pageType);

        int onMeasureThingBody();
    }

    private OnThingSelectedListener listener;
    private OnSubredditEventListener eventListener;
    private ThingBundleHolder thingBundleHolder;
    private ThingListAdapter adapter;
    private ThingListController controller;
    private int emptyText;
    private boolean scrollLoading;

    public static ThingListFragment newSubredditInstance(String accountName, String subreddit,
            int filter, int flags) {
        Bundle args = new Bundle(4);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putInt(ARG_FILTER, filter);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    public static ThingListFragment newQueryInstance(String accountName, String subreddit,
            String query, int flags) {
        Bundle args = new Bundle(4);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    public static ThingListFragment newInstance(String accountName, String subreddit, String query,
            String profileUser, String messageUser, int filter, int flags) {
        Bundle args = new Bundle(7);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_QUERY, query);
        args.putString(ARG_PROFILE_USER, profileUser);
        args.putString(ARG_MESSAGE_USER, messageUser);
        args.putInt(ARG_FILTER, filter);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    private static ThingListFragment newFragment(Bundle args) {
        ThingListFragment frag = new ThingListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingSelectedListener) {
            listener = (OnThingSelectedListener) activity;
        }
        if (activity instanceof OnSubredditEventListener) {
            eventListener = (OnSubredditEventListener) activity;
        }
        if (activity instanceof ThingBundleHolder) {
            thingBundleHolder = (ThingBundleHolder) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int flags = getArguments().getInt(ARG_FLAGS);
        boolean singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);

        adapter = new ThingListAdapter(getActivity(),
                getArguments().getString(ARG_SUBREDDIT),
                getArguments().getString(ARG_QUERY),
                getArguments().getString(ARG_PROFILE_USER),
                getArguments().getString(ARG_MESSAGE_USER),
                getArguments().getInt(ARG_FILTER),
                this, singleChoice);

        adapter.setAccountName(getArguments().getString(ARG_ACCOUNT_NAME));
        adapter.setParentSubreddit(getArguments().getString(ARG_SUBREDDIT));

        if (savedInstanceState != null) {
            adapter.setAccountName(savedInstanceState.getString(STATE_ACCOUNT_NAME));
            adapter.setParentSubreddit(savedInstanceState.getString(STATE_PARENT_SUBREDDIT));
            adapter.setSubreddit(savedInstanceState.getString(STATE_SUBREDDIT));

            String thingId = savedInstanceState.getString(STATE_SELECTED_THING_ID);
            String linkId = savedInstanceState.getString(STATE_SELECTED_LINK_ID);
            adapter.setSelectedThing(thingId, linkId);

            emptyText = savedInstanceState.getInt(STATE_EMPTY_TEXT);
        }

        controller = createController();
        controller.restoreState(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) v.findViewById(android.R.id.list);
        listView.setVerticalScrollBarEnabled(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);

        SwipeDismissTouchListener touchListener = new SwipeDismissTouchListener(listView, this);
        listView.setOnTouchListener(touchListener);

        final OnScrollListener scrollListener = touchListener.makeScrollListener();
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisible, int visibleCount,
                    int totalCount) {
                scrollListener.onScroll(view, firstVisible, visibleCount, totalCount);
                ThingListFragment.this.onScroll(view, firstVisible, visibleCount, totalCount);
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                scrollListener.onScrollStateChanged(view, scrollState);
                ThingListFragment.this.onScrollStateChanged(view, scrollState);
            }
        });
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter.setThingBodyWidth(getThingBodyWidth());
        setListAdapter(adapter);
        setListShown(false);
        if (emptyText == 0) {
            loadIfPossible();
        } else {
            showEmpty();
        }
    }

    public void loadIfPossible() {
        if (controller.isLoadable()) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public void setEmpty(boolean error) {
        this.emptyText = error ? R.string.error : R.string.empty_list;
        showEmpty();
    }

    private void showEmpty() {
        setEmptyText(getString(emptyText));
        setListShown(true);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        args = Objects.nullToEmpty(args);
        controller.setMoreId(args.getString(LOADER_MORE_ID));
        return controller.createLoader();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Process ThingProvider results.
        super.onLoadFinished(loader, cursor);

        scrollLoading = false;
        controller.setMoreId(null);
        adapter.swapCursor(cursor);

        Bundle extras = Objects.nullToEmpty(cursor.getExtras());
        controller.setSessionId(extras.getLong(ThingProvider.EXTRA_SESSION_ID));

        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
        getActivity().invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        adapter.setParentSubreddit(subreddit);
        adapter.setSubreddit(subreddit);
        if (eventListener != null) {
            eventListener.onSubredditDiscovery(subreddit);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (visibleItemCount <= 0 || scrollLoading) {
            return;
        }
        if (firstVisibleItem + visibleItemCount * 2 >= totalItemCount
                && getLoaderManager().getLoader(0) != null
                && controller.hasNextMoreId()) {
            scrollLoading = true;
            Bundle args = new Bundle(1);
            args.putString(LOADER_MORE_ID, controller.getNextMoreId());
            getLoaderManager().restartLoader(0, args, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectThing(v, position, ThingPagerAdapter.TYPE_LINK);
        if (adapter.isSingleChoice() && v instanceof ThingView) {
            ((ThingView) v).setChosen(true);
        }
    }

    private void selectThing(View v, int position, int pageType) {
        adapter.setSelectedPosition(position);
        if (listener != null) {
            listener.onThingSelected(v, controller.getThingBundle(position), pageType);
        }
        controller.select(position);
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return controller.isSwipeDismissable(position);
    }

    @Override
    public void onSwipeDismiss(ListView listView, View view, int position) {
        controller.hide(position, true);
    }

    public void onVote(View v, int action) {
        if (!TextUtils.isEmpty(adapter.getAccountName())) {
            int position = getListView().getPositionForView(v);
            controller.vote(position, action);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_list_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        String subreddit = getSubreddit();
        boolean isQuery = isQuery();
        boolean hasAccount = AccountUtils.isAccount(getAccountName());
        boolean hasSubreddit = subreddit != null;
        boolean hasThing = thingBundleHolder != null && thingBundleHolder.getThingBundle() != null;
        boolean hasSidebar = Subreddits.hasSidebar(subreddit);

        boolean showNewPost = !isQuery && hasAccount && hasSubreddit && !hasThing;
        boolean showAddSubreddit = !isQuery && hasSubreddit && !hasThing;
        boolean showSubreddit = !isQuery && hasSubreddit && !hasThing && hasSidebar;

        menu.findItem(R.id.menu_new_post).setVisible(showNewPost);
        menu.findItem(R.id.menu_add_subreddit).setVisible(showAddSubreddit);

        MenuItem subredditItem = menu.findItem(R.id.menu_subreddit);
        subredditItem.setVisible(showSubreddit);
        if (showSubreddit) {
            subredditItem.setTitle(MenuHelper.getSubredditTitle(getActivity(), subreddit));
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (adapter.getCursor() == null) {
            getListView().clearChoices();
            return false;
        }
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thing_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        mode.setTitle(getResources().getQuantityString(R.plurals.things, count, count));
        controller.prepareActionMenu(menu, getListView(), getFirstCheckedPosition());
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_saved:
                controller.save(getFirstCheckedPosition(), false);
                mode.finish();
                return true;

            case R.id.menu_unsaved:
                controller.save(getFirstCheckedPosition(), true);
                mode.finish();
                return true;

            case R.id.menu_hide:
                controller.hide(getFirstCheckedPosition(), true);
                mode.finish();
                return true;

            case R.id.menu_unhide:
                controller.hide(getFirstCheckedPosition(), false);
                mode.finish();
                return true;

            case R.id.menu_comments:
                handleComments();
                mode.finish();
                return true;

            case R.id.menu_copy_url:
                controller.copyUrl(getFirstCheckedPosition());
                mode.finish();
                return true;

            case R.id.menu_author:
                controller.author(getFirstCheckedPosition());
                mode.finish();
                return true;

            case R.id.menu_subreddit:
                controller.subreddit(getFirstCheckedPosition());
                mode.finish();
                return true;

            default:
                return false;
        }
    }

    private void handleComments() {
        selectThing(null, getFirstCheckedPosition(), ThingPagerAdapter.TYPE_COMMENTS);
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveState(outState);
        outState.putString(STATE_ACCOUNT_NAME, adapter.getAccountName());
        outState.putString(STATE_PARENT_SUBREDDIT, adapter.getParentSubreddit());
        outState.putString(STATE_SUBREDDIT, adapter.getSubreddit());
        outState.putString(STATE_SELECTED_THING_ID, adapter.getSelectedThingId());
        outState.putString(STATE_SELECTED_LINK_ID, adapter.getSelectedLinkId());
        outState.putInt(STATE_EMPTY_TEXT, emptyText);
    }

    private int getThingBodyWidth() {
        return listener != null ? listener.onMeasureThingBody() : 0;
    }

    public void setAccountName(String accountName) {
        adapter.setAccountName(accountName);
    }

    public void setSelectedThing(String thingId, String linkId) {
        adapter.setSelectedThing(thingId, linkId);
    }

    public String getAccountName() {
        return adapter.getAccountName();
    }

    public String getSubreddit() {
        return adapter.getSubreddit();
    }

    public void setSubreddit(String subreddit) {
        if (!Objects.equalsIgnoreCase(subreddit, adapter.getSubreddit())) {
            adapter.setSubreddit(subreddit);
        }
    }

    public String getQuery() {
        return adapter.getQuery();
    }

    public boolean isQuery() {
        return !TextUtils.isEmpty(getQuery());
    }

    public int getFilter() {
        return adapter.getFilterValue();
    }

    private int getFirstCheckedPosition() {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int size = adapter.getCount();
        for (int i = 0; i < size; i++) {
            if (checked.get(i)) {
                return i;
            }
        }
        return -1;
    }

    // Methods for creating the correct controller based upon the fragment arguments.
    private ThingListController createController() {
        if (isProfileActivity()) {
            return new ProfileThingListController(getActivity(),
                    getAccountNameArgument(),
                    getProfileUserArgument(),
                    getFilterArgument(),
                    null,
                    adapter);
        } else if (isMessageActivity()) {
            return new MessageThingListController(getActivity(),
                    getAccountNameArgument(),
                    getMessageUserArgument(),
                    getFilterArgument(),
                    null,
                    adapter);
        } else if (isSearchActivity()) {
            return new SearchThingListController(getActivity(),
                    getAccountNameArgument(),
                    adapter);
        } else if (isBrowserActivity()) {
            return new SubredditThingListController(getActivity(),
                    getAccountNameArgument(),
                    getSubredditArgument(),
                    getFilterArgument(),
                    adapter);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private boolean isProfileActivity() {
        return !TextUtils.isEmpty(getProfileUserArgument());
    }

    private boolean isMessageActivity() {
        return !TextUtils.isEmpty(getMessageUserArgument());
    }

    private boolean isSearchActivity() {
        return !TextUtils.isEmpty(getQueryArgument());
    }

    private boolean isBrowserActivity() {
        return getSubredditArgument() != null; // Empty but non-null subreddit means front page.
    }

    // Getters for fragment arguments.

    private String getAccountNameArgument() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }

    private String getSubredditArgument() {
        return getArguments().getString(ARG_SUBREDDIT);
    }

    private String getQueryArgument() {
        return getArguments().getString(ARG_QUERY);
    }

    private String getProfileUserArgument() {
        return getArguments().getString(ARG_PROFILE_USER);
    }

    private String getMessageUserArgument() {
        return getArguments().getString(ARG_MESSAGE_USER);
    }

    private int getFilterArgument() {
        return getArguments().getInt(ARG_FILTER);
    }

    private int getFlags() {
        return getArguments().getInt(ARG_FLAGS);
    }
}
