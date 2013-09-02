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

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.AbstractBrowserActivity.LeftFragment;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.AccountSubredditListLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ComparableFragments;
import com.btmura.android.reddit.util.ListViews;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.AccountPlaceAdapter;
import com.btmura.android.reddit.widget.AccountPlaceAdapter.OnPlaceSelectedListener;
import com.btmura.android.reddit.widget.AccountResultAdapter;
import com.btmura.android.reddit.widget.AccountResultAdapter.Item;
import com.btmura.android.reddit.widget.AccountResultAdapter.OnAccountMessagesSelectedListener;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.MergeAdapter;

public class NavigationFragment extends ListFragment implements
        LeftFragment,
        LoaderCallbacks<AccountResult>,
        OnAccountMessagesSelectedListener,
        OnPlaceSelectedListener,
        OnSubredditEventListener,
        MultiChoiceModeListener,
        ComponentCallbacks2 {

    public static final String TAG = "NavigationFragment";

    private static final String STATE_STATE = "state";

    private static final int ADAPTER_ACCOUNTS = 0;
    private static final int ADAPTER_PLACES = 1;
    private static final int ADAPTER_SUBREDDITS = 2;

    private static final int LOADER_ACCOUNTS = 0;
    private static final int LOADER_SUBREDDITS = 1;

    public interface OnNavigationEventListener {
        void onNavigationSubredditSelected(String accountName,
                String subreddit,
                boolean isRandom,
                int filter,
                boolean force);

        void onNavigationProfileSelected(String accountName, int filter, boolean force);

        void onNavigationSavedSelected(String accountName, int filter, boolean force);

        void onNavigationMessagesSelected(String accountName, int filter, boolean force);
    }

    private final AccountSubredditLoaderCallbacks subredditLoaderCallbacks =
            new AccountSubredditLoaderCallbacks();

    private OnNavigationEventListener listener;
    private AccountResultAdapter accountAdapter;
    private AccountPlaceAdapter placesAdapter;
    private AccountSubredditAdapter subredditAdapter;
    private MergeAdapter mergeAdapter;

    private State restoreState;
    private State oldState;
    private State state;

    public static NavigationFragment newInstance() {
        return new NavigationFragment();
    }

    @Override
    public boolean fragmentEquals(ComparableFragment o) {
        return ComparableFragments.baseEquals(this, o);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnNavigationEventListener) {
            listener = (OnNavigationEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            restoreState = savedInstanceState.getParcelable(STATE_STATE);
        }
        accountAdapter = new AccountResultAdapter(getActivity(), this);
        placesAdapter = new AccountPlaceAdapter(getActivity(), this);
        subredditAdapter = AccountSubredditAdapter.newAccountInstance(getActivity());
        mergeAdapter = new MergeAdapter(accountAdapter, placesAdapter, subredditAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) v.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(mergeAdapter);
        setListShown(false);
        getLoaderManager().initLoader(LOADER_ACCOUNTS, null, this);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity(), true, true);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountAdapter.setAccountResult(result);
        setListShown(true);
        if (state == null || !result.hasAccount(state.accountName)) {
            if (restoreState == null || !result.hasAccount(restoreState.accountName)) {
                String accountName = result.getLastAccount(getActivity());
                refresh(newAccountState(accountName), false);
            } else {
                refresh(restoreState, false);
            }
            restoreState = null;
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    private void refresh(State newState, boolean force) {
        oldState = state;
        state = newState;
        refreshAccounts(oldState, state);
        refreshPlace(oldState, state, force);
    }

    private void refreshAccounts(State oldState, State newState) {
        if (oldState == null || !Objects.equals(oldState.accountName, newState.accountName)) {
            accountAdapter.setSelectedAccountName(newState.accountName);
            AccountPrefs.setLastAccount(getActivity(), newState.accountName);
            placesAdapter.setAccountPlaces(accountAdapter.getCount() > 1,
                    AccountUtils.isAccount(newState.accountName));
            loadSubreddits(true);
        } else {
            loadSubreddits(false);
        }
    }

    private void loadSubreddits(boolean restart) {
        if (restart) {
            getLoaderManager().restartLoader(LOADER_SUBREDDITS, null, subredditLoaderCallbacks);
        } else {
            getLoaderManager().initLoader(LOADER_SUBREDDITS, null, subredditLoaderCallbacks);
        }
    }

    private void refreshPlace(State oldState, State newState, boolean force) {
        if (force
                || oldState == null
                || !Objects.equals(oldState.accountName, newState.accountName)
                || !Objects.equals(oldState.place, newState.place)
                || !Objects.equals(oldState.subreddit, newState.subreddit)
                || !Objects.equals(oldState.isRandom, newState.isRandom)
                || !Objects.equals(oldState.filter, newState.filter)) {

            String accountName = newState.accountName;
            int place = newState.place;
            String subreddit = newState.subreddit;
            boolean isRandom = newState.isRandom;
            int filter = newState.filter;

            placesAdapter.setSelectedPlace(place);
            AccountPrefs.setLastPlace(getActivity(), place);

            switch (place) {
                case PLACE_SUBREDDIT:
                    AccountPrefs.setLastSubreddit(getActivity(), accountName, subreddit);
                    AccountPrefs.setLastIsRandom(getActivity(), accountName, isRandom);
                    AccountPrefs.setLastSubredditFilter(getActivity(), filter);

                    String selectedSubreddit = isRandom ? Subreddits.NAME_RANDOM : subreddit;
                    subredditAdapter.setSelectedSubreddit(selectedSubreddit);

                    if (listener != null) {
                        listener.onNavigationSubredditSelected(accountName,
                                subreddit,
                                isRandom,
                                filter,
                                force);
                    }
                    break;

                case PLACE_PROFILE:
                    subredditAdapter.setSelectedSubreddit(null);
                    if (listener != null) {
                        listener.onNavigationProfileSelected(accountName, filter, force);
                    }
                    break;

                case PLACE_SAVED:
                    subredditAdapter.setSelectedSubreddit(null);
                    if (listener != null) {
                        listener.onNavigationSavedSelected(accountName, filter, force);
                    }
                    break;

                case PLACE_MESSAGES:
                    subredditAdapter.setSelectedSubreddit(null);
                    if (listener != null) {
                        listener.onNavigationMessagesSelected(accountName, filter, force);
                    }
                    break;
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        int adapterIndex = mergeAdapter.getAdapterIndex(position);
        int adapterPosition = mergeAdapter.getAdapterPosition(position);
        switch (adapterIndex) {
            case ADAPTER_ACCOUNTS:
                handleAccountClick(adapterPosition);
                break;

            case ADAPTER_SUBREDDITS:
                handleSubredditClick(adapterPosition);
                break;

            default:
                throw new IllegalArgumentException("adapterIndex: " + adapterIndex);
        }
    }

    private void handleAccountClick(int position) {
        Item item = accountAdapter.getItem(position);
        refresh(newAccountState(item.getAccountName()), true);
    }

    private void handleSubredditClick(int position) {
        String subreddit = subredditAdapter.getName(position);
        refresh(newSubredditState(subreddit, Subreddits.isRandom(subreddit)), true);
    }

    @Override
    public void onAccountMessagesSelected(String accountName) {
        refresh(newPlaceState(accountName, PLACE_MESSAGES), true);
    }

    @Override
    public void onPlaceSelected(int place) {
        refresh(newPlaceState(state.accountName, place), true);
    }

    public void setFilter(int filter) {
        refresh(newFilterState(state.accountName, filter), false);
    }

    @Override
    public void onSubredditDiscovery(String subreddit) {
        state = newSubredditState(subreddit, true);
        AccountPrefs.setLastSubreddit(getActivity(), state.accountName, subreddit);
    }

    private State newAccountState(String accountName) {
        int place = getLastPlace(accountName);
        String subreddit = getLastSubreddit(accountName);
        boolean isRandom = getLastIsRandom(accountName);
        int filter = getPlaceFilter(accountName, place);
        return new State(accountName, place, subreddit, isRandom, filter);
    }

    private State newSubredditState(String subreddit, boolean isRandom) {
        String accountName = state.accountName;
        int place = PLACE_SUBREDDIT;
        int filter = getPlaceFilter(accountName, place);
        return new State(accountName, place, subreddit, isRandom, filter);
    }

    private State newPlaceState(String accountName, int place) {
        int filter = getPlaceFilter(accountName, place);
        return new State(state.accountName,
                place,
                state.subreddit,
                state.isRandom,
                filter);
    }

    private State newFilterState(String accountName, int filter) {
        return new State(state.accountName,
                state.place,
                state.subreddit,
                state.isRandom,
                filter);
    }

    private int getLastPlace(String accountName) {
        return AccountUtils.isAccount(accountName)
                ? AccountPrefs.getLastPlace(getActivity(), PLACE_SUBREDDIT)
                : PLACE_SUBREDDIT;
    }

    private String getLastSubreddit(String accountName) {
        return AccountPrefs.getLastSubreddit(getActivity(),
                accountName,
                Subreddits.NAME_FRONT_PAGE);
    }

    private boolean getLastIsRandom(String accountName) {
        return AccountPrefs.getLastIsRandom(getActivity(), accountName, false);
    }

    private int getPlaceFilter(String accountName, int place) {
        switch (place) {
            case PLACE_SUBREDDIT:
                return AccountPrefs.getLastSubredditFilter(getActivity(),
                        FilterAdapter.SUBREDDIT_HOT);

            case PLACE_PROFILE:
                return FilterAdapter.PROFILE_OVERVIEW;

            case PLACE_SAVED:
                return FilterAdapter.PROFILE_SAVED;

            case PLACE_MESSAGES:
                return FilterAdapter.MESSAGE_INBOX;

            default:
                throw new IllegalArgumentException("place: " + place);
        }
    }

    public boolean isRandom() {
        return state.isRandom;
    }

    public int getFilter() {
        return state.filter;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (subredditAdapter.getCursor() == null) {
            getListView().clearChoices();
            return false;
        }

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.subreddit_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        boolean aboutItemVisible = count == 1;
        boolean shareItemsVisible = count == 1;
        boolean deleteItemVisible = true;

        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int size = checked.size();
        for (int i = 0; i < size; i++) {
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                int adapterIndex = mergeAdapter.getAdapterIndex(position);
                switch (adapterIndex) {
                    case ADAPTER_ACCOUNTS:
                    case ADAPTER_PLACES:
                        aboutItemVisible = shareItemsVisible = deleteItemVisible = false;
                        break;

                    case ADAPTER_SUBREDDITS:
                        int adapterPosition = mergeAdapter.getAdapterPosition(position);
                        String subreddit = subredditAdapter.getName(adapterPosition);
                        boolean hasSidebar = Subreddits.hasSidebar(subreddit);
                        aboutItemVisible &= hasSidebar;
                        shareItemsVisible &= hasSidebar;
                        deleteItemVisible &= hasSidebar;
                        break;
                }
            }
        }

        if (aboutItemVisible || shareItemsVisible || deleteItemVisible) {
            prepareMode(mode, count);
            prepareAddItem(menu);
            prepareAboutItem(menu, aboutItemVisible);
            prepareDeleteItem(menu, deleteItemVisible);
            prepareShareItems(menu, shareItemsVisible);
        } else {
            mode.finish();
        }

        return true;
    }

    private void prepareMode(ActionMode mode, int checkedCount) {
        mode.setTitle(getResources().getQuantityString(R.plurals.subreddits,
                checkedCount, checkedCount));
    }

    private void prepareAddItem(Menu menu) {
        MenuItem addItem = menu.findItem(R.id.menu_add_subreddit);
        addItem.setVisible(accountAdapter.getCount() > 1);
    }

    private void prepareAboutItem(Menu menu, boolean visible) {
        MenuItem aboutItem = menu.findItem(R.id.menu_subreddit);
        aboutItem.setVisible(visible);
        if (visible) {
            aboutItem.setTitle(getString(R.string.menu_subreddit, getFirstCheckedSubreddit()));
        }
    }

    private void prepareDeleteItem(Menu menu, boolean visible) {
        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        deleteItem.setVisible(visible);
    }

    private void prepareShareItems(Menu menu, boolean visible) {
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        shareItem.setVisible(visible);
        if (visible) {
            MenuHelper.setShareProvider(shareItem, getClipLabel(), getClipText());
        }

        MenuItem copyUrlItem = menu.findItem(R.id.menu_copy_url);
        copyUrlItem.setVisible(visible);
    }

    private String getClipLabel() {
        return getFirstCheckedSubreddit();
    }

    private CharSequence getClipText() {
        return Urls.subreddit(getFirstCheckedSubreddit(), -1, null, Urls.TYPE_HTML);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
            boolean checked) {
        mode.invalidate();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_subreddit:
                handleAddSubreddit();
                mode.finish();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit();
                mode.finish();
                return true;

            case R.id.menu_delete:
                handleDelete();
                mode.finish();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl();
                mode.finish();
                return true;
        }
        return false;
    }

    private void handleAddSubreddit() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(), getCheckedSubreddits());
    }

    private void handleSubreddit() {
        MenuHelper.startSidebarActivity(getActivity(), getFirstCheckedSubreddit());
    }

    private String getFirstCheckedSubreddit() {
        int position = ListViews.getFirstCheckedPosition(getListView());
        int adapterPosition = mergeAdapter.getAdapterPosition(position);
        return subredditAdapter.getName(adapterPosition);
    }

    private void handleDelete() {
        String[] subreddits = getCheckedSubreddits();
        Provider.removeSubredditAsync(getActivity(), state.accountName, subreddits);
    }

    private String[] getCheckedSubreddits() {
        ListView listView = getListView();
        int checkedCount = listView.getCheckedItemCount();
        String[] subreddits = new String[checkedCount];

        SparseBooleanArray checked = listView.getCheckedItemPositions();
        int size = checked.size();
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                int adapterPosition = mergeAdapter.getAdapterPosition(position);
                subreddits[j++] = subredditAdapter.getName(adapterPosition);
            }
        }
        return subreddits;
    }

    private void handleCopyUrl() {
        MenuHelper.setClipAndToast(getActivity(), getClipLabel(), getClipText());
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onTrimMemory(int level) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_STATE, state);
    }

    class AccountSubredditLoaderCallbacks implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new AccountSubredditListLoader(getActivity(), state.accountName);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            subredditAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            subredditAdapter.swapCursor(null);
        }
    }

    static class State implements Parcelable {

        public static final Parcelable.Creator<State> CREATOR =
                new Parcelable.Creator<State>() {
                    @Override
                    public State createFromParcel(Parcel source) {
                        return new State(source);
                    }

                    @Override
                    public State[] newArray(int size) {
                        return new State[size];
                    }
                };

        String accountName;
        int place;
        String subreddit;
        boolean isRandom;
        int filter;

        State(String accountName,
                int place,
                String subreddit,
                boolean isRandom,
                int filter) {
            this.accountName = accountName;
            this.place = place;
            this.subreddit = subreddit;
            this.isRandom = isRandom;
            this.filter = filter;
        }

        State(Parcel in) {
            this.accountName = in.readString();
            this.place = in.readInt();
            this.subreddit = in.readString();
            this.isRandom = in.readInt() == 1;
            this.filter = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(accountName);
            dest.writeInt(place);
            dest.writeString(subreddit);
            dest.writeInt(isRandom ? 1 : 0);
            dest.writeInt(filter);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public State withPlaceFilter(int place, int filter) {
            return new State(accountName, place, subreddit, isRandom, filter);
        }
    }
}
