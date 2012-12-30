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
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.ThingAdapter;
import com.btmura.android.reddit.widget.OnVoteListener;

public class ThingListFragment extends ThingProviderListFragment implements
        OnScrollListener, OnVoteListener, MultiChoiceModeListener {

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
    private static final String ARG_MORE = "more";

    /** Optional bit mask for controlling fragment appearance. */
    private static final String ARG_FLAGS = "flags";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String STATE_ACCOUNT_NAME = ARG_ACCOUNT_NAME;

    private static final String STATE_PARENT_SUBREDDIT = "parentSubreddit";

    private static final String STATE_SUBREDDIT = ARG_SUBREDDIT;

    /** String argument specifying session ID of the data. */
    private static final String STATE_SESSION_ID = "sessionId";

    private static final String STATE_SELECTED_THING_ID = "selectedThingId";
    private static final String STATE_SELECTED_LINK_ID = "selectedLinkId";
    private static final String STATE_EMPTY_TEXT = "emptyText";

    public interface OnThingSelectedListener {
        void onThingSelected(Bundle thingBundle);

        int onMeasureThingBody();
    }

    private OnThingSelectedListener listener;
    private OnSubredditEventListener eventListener;
    private ThingAdapter adapter;
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

    public static ThingListFragment newQueryInstance(String accountName, String query, int flags) {
        Bundle args = new Bundle(3);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    public static ThingListFragment newInstance(String accountName, String query,
            String profileUser, String messageUser, int filter, int flags) {
        Bundle args = new Bundle(6);
        args.putString(ARG_ACCOUNT_NAME, accountName);
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int flags = getArguments().getInt(ARG_FLAGS);
        boolean singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);

        adapter = new ThingAdapter(getActivity(),
                getArguments().getString(ARG_SUBREDDIT),
                getArguments().getString(ARG_QUERY),
                getArguments().getString(ARG_PROFILE_USER),
                getArguments().getString(ARG_MESSAGE_USER),
                getArguments().getInt(ARG_FILTER),
                this, singleChoice);

        adapter.setAccountName(getArguments().getString(ARG_ACCOUNT_NAME));
        adapter.setParentSubreddit(getArguments().getString(ARG_SUBREDDIT));

        if (savedInstanceState != null) {
            adapter.setSessionId(savedInstanceState.getLong(STATE_SESSION_ID));
            adapter.setAccountName(savedInstanceState.getString(STATE_ACCOUNT_NAME));
            adapter.setParentSubreddit(savedInstanceState.getString(STATE_PARENT_SUBREDDIT));
            adapter.setSubreddit(savedInstanceState.getString(STATE_SUBREDDIT));

            String thingId = savedInstanceState.getString(STATE_SELECTED_THING_ID);
            String linkId = savedInstanceState.getString(STATE_SELECTED_LINK_ID);
            adapter.setSelectedThing(thingId, linkId);

            emptyText = savedInstanceState.getInt(STATE_EMPTY_TEXT);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        l.setVerticalScrollBarEnabled(false);
        l.setOnScrollListener(this);
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(this);
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
        if (adapter.isLoadable()) {
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
        if (args != null) {
            adapter.setMore(args.getString(ARG_MORE));
        }
        return adapter.getLoader(getActivity());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Process ThingProvider results.
        super.onLoadFinished(loader, cursor);

        scrollLoading = false;
        adapter.setMore(null);
        adapter.updateLoaderUri(getActivity(), loader);
        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
        getActivity().invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    protected void onSessionIdLoaded(long sessionId) {
        adapter.setSessionId(sessionId);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        adapter.setParentSubreddit(subreddit);
        adapter.setSubreddit(subreddit);
        if (eventListener != null) {
            eventListener.onSubredditDiscovery(subreddit);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        adapter.setSelectedPosition(position);
        if (listener != null) {
            listener.onThingSelected(adapter.getThingBundle(getActivity(), position));
        }
        if (adapter.isNew(position)) {
            Provider.readMessageAsync(getActivity(), adapter.getAccountName(),
                    adapter.getThingId(position), true);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (visibleItemCount <= 0 || scrollLoading) {
            return;
        }
        if (firstVisibleItem + visibleItemCount * 2 >= totalItemCount) {
            if (getLoaderManager().getLoader(0) != null) {
                if (!adapter.isEmpty()) {
                    String more = adapter.getNextMore();
                    if (!TextUtils.isEmpty(more)) {
                        scrollLoading = true;
                        Bundle b = new Bundle(1);
                        b.putString(ARG_MORE, more);
                        getLoaderManager().restartLoader(0, b, this);
                    }
                }
            }
        }
    }

    public void onVote(String thingId, int likes) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLike id: " + thingId + " likes: " + likes);
        }
        String accountName = adapter.getAccountName();
        if (!TextUtils.isEmpty(accountName)) {
            Provider.voteAsync(getActivity(), accountName, thingId, likes);
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
        boolean redundant = menu.findItem(R.id.menu_about_thing_subreddit) != null;
        String subreddit = adapter.getSubreddit();
        menu.findItem(R.id.menu_about_subreddit).setVisible(!redundant
                && subreddit != null
                && !Subreddits.isFrontPage(subreddit)
                && !Subreddits.isAll(subreddit));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about_subreddit:
                handleAboutSubreddit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleAboutSubreddit() {
        Intent intent = new Intent(getActivity(), SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, adapter.getSubreddit());
        startActivity(intent);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thing_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        boolean hasAccount = AccountUtils.isAccount(adapter.getAccountName());
        mode.setTitle(getResources().getQuantityString(R.plurals.things, count, count));

        menu.findItem(R.id.menu_new_message).setVisible(hasAccount && count == 1);
        menu.findItem(R.id.menu_view_profile).setVisible(count == 1);
        menu.findItem(R.id.menu_copy_url).setVisible(count == 1);

        boolean saveable = false;
        boolean saved = false;
        if (hasAccount && count == 1) {
            int position = getFirstCheckedPosition();
            saveable = adapter.getKind(position) != Kinds.KIND_MESSAGE;
            saved = adapter.isSaved(position);
        }
        menu.findItem(R.id.menu_saved).setVisible(saveable && saved);
        menu.findItem(R.id.menu_unsaved).setVisible(saveable && !saved);

        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_message:
                return handleNewMessage(mode);

            case R.id.menu_saved:
                return handleSaved(mode, SaveActions.ACTION_UNSAVE);

            case R.id.menu_unsaved:
                return handleSaved(mode, SaveActions.ACTION_SAVE);

            case R.id.menu_view_profile:
                return handleViewProfile(mode);

            case R.id.menu_copy_url:
                return handleCopyUrl(mode);

            default:
                return false;
        }
    }

    private boolean handleNewMessage(ActionMode mode) {
        String user = adapter.getAuthor(getFirstCheckedPosition());
        MenuHelper.startComposeActivity(getActivity(),
                ComposeActivity.COMPOSITION_MESSAGE, user, null);
        mode.finish();
        return true;
    }

    private boolean handleSaved(ActionMode mode, int action) {
        String accountName = adapter.getAccountName();
        String thingId = adapter.getThingId(getFirstCheckedPosition());
        Provider.saveAsync(getActivity(), accountName, thingId, action);
        mode.finish();
        return true;
    }

    private boolean handleViewProfile(ActionMode mode) {
        String user = adapter.getAuthor(getFirstCheckedPosition());
        MenuHelper.startProfileActivity(getActivity(), user, FilterAdapter.PROFILE_OVERVIEW);
        mode.finish();
        return true;
    }

    private boolean handleCopyUrl(ActionMode mode) {
        int position = getFirstCheckedPosition();
        String title = adapter.getTitle(position);
        CharSequence url = adapter.getUrl(position);
        MenuHelper.setClipAndToast(getActivity(), title, url);
        mode.finish();
        return true;
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SESSION_ID, adapter.getSessionId());
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

    public void setSubreddit(String subreddit) {
        if (!Objects.equalsIgnoreCase(subreddit, adapter.getSubreddit())) {
            adapter.setSubreddit(subreddit);
        }
    }

    public String getQuery() {
        return adapter.getQuery();
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
}
