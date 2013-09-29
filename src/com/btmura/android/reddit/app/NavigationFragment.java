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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
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
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.AbstractBrowserActivity.LeftFragment;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.AccountSubredditListLoader;
import com.btmura.android.reddit.content.RandomSubredditLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ComparableFragments;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.util.Views;
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
        MultiChoiceModeListener {

    public static final String TAG = "NavigationFragment";

    private static final String ARG_REQUESTED_SUBREDDIT = "requestedSubreddit";
    private static final String ARG_REQUESTED_THING_BUNDLE = "requestedThingBundle";

    private static final String STATE_REQUESTED_SUBREDDIT = ARG_REQUESTED_SUBREDDIT;
    private static final String STATE_REQUESTED_THING_BUNDLE = ARG_REQUESTED_THING_BUNDLE;

    private static final String STATE_ACCOUNT_NAME = "accountName";
    private static final String STATE_PLACE = "place";
    private static final String STATE_SUBREDDIT = "subreddit";
    private static final String STATE_IS_RANDOM = "isRandom";
    private static final String STATE_FILTER = "filter";

    private static final int ADAPTER_ACCOUNTS = 0;
    private static final int ADAPTER_PLACES = 1;
    private static final int ADAPTER_SUBREDDITS = 2;

    private static final int LOADER_ACCOUNTS = 0;
    private static final int LOADER_SUBREDDITS = 1;
    private static final int LOADER_RANDOM_SUBREDDIT = 2;

    private static final String LOADER_ARG_ACCOUNT_NAME = "accountName";

    public interface OnNavigationEventListener {
        void onNavigationSubredditSelected(String accountName,
                String subreddit,
                boolean isRandom,
                int filter,
                ThingBundle thingBundle,
                boolean force);

        void onNavigationProfileSelected(String accountName, int filter, boolean force);

        void onNavigationSavedSelected(String accountName, int filter, boolean force);

        void onNavigationMessagesSelected(String accountName, int filter, boolean force);
    }

    private final SubredditLoaderCallbacks subredditLoaderCallbacks =
            new SubredditLoaderCallbacks();
    private final RandomSubredditLoaderCallbacks randomLoaderCallbacks =
            new RandomSubredditLoaderCallbacks();

    private OnNavigationEventListener listener;
    private AccountResultAdapter accountAdapter;
    private AccountPlaceAdapter placesAdapter;
    private AccountSubredditAdapter subredditAdapter;
    private MergeAdapter mergeAdapter;

    private String requestedSubreddit;
    private ThingBundle requestedThingBundle;

    private String accountName;
    private int place;
    private String subreddit;
    private boolean isRandom;
    private int filter;

    public static NavigationFragment newInstance(String requestedSubreddit,
            ThingBundle requestedThingBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_REQUESTED_SUBREDDIT, requestedSubreddit);
        args.putParcelable(ARG_REQUESTED_THING_BUNDLE, requestedThingBundle);

        NavigationFragment frag = new NavigationFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public boolean equalFragments(ComparableFragment o) {
        return ComparableFragments.equalClasses(this, o);
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
        if (savedInstanceState == null) {
            requestedSubreddit = getArguments().getString(ARG_REQUESTED_SUBREDDIT);
            requestedThingBundle = getArguments().getParcelable(ARG_REQUESTED_THING_BUNDLE);
        } else {
            requestedSubreddit = savedInstanceState.getString(STATE_REQUESTED_SUBREDDIT);
            requestedThingBundle = savedInstanceState.getParcelable(STATE_REQUESTED_THING_BUNDLE);

            accountName = savedInstanceState.getString(STATE_ACCOUNT_NAME);
            place = savedInstanceState.getInt(STATE_PLACE);
            subreddit = savedInstanceState.getString(STATE_SUBREDDIT);
            isRandom = savedInstanceState.getBoolean(STATE_IS_RANDOM);
            filter = savedInstanceState.getInt(STATE_FILTER);
        }

        accountAdapter = new AccountResultAdapter(getActivity(), this);
        placesAdapter = new AccountPlaceAdapter(getActivity(), this);
        subredditAdapter = AccountSubredditAdapter.newAccountInstance(getActivity());
        mergeAdapter = new MergeAdapter(accountAdapter, placesAdapter, subredditAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
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

        String accountName = result.getLastAccount(getActivity());
        refreshAccount(accountName);
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    private void refreshAccount(String accountName) {
        boolean accountChanged = selectAccount(accountName);
        refreshPlace(accountChanged);
    }

    private boolean selectAccount(String accountName) {
        boolean accountChanged = !Objects.equals(this.accountName, accountName);
        this.accountName = accountName;
        accountAdapter.setSelectedAccountName(accountName);
        AccountPrefs.setLastAccount(getActivity(), accountName);
        placesAdapter.setAccountPlaces(accountAdapter.getCount() > 1,
                AccountUtils.isAccount(accountName));
        refreshSubredditLoader(accountChanged);
        return accountChanged;
    }

    private void refreshSubredditLoader(boolean restartLoader) {
        Bundle args = newLoaderArgs(accountName);
        if (restartLoader) {
            getLoaderManager().restartLoader(LOADER_SUBREDDITS, args, subredditLoaderCallbacks);
        } else {
            getLoaderManager().initLoader(LOADER_SUBREDDITS, args, subredditLoaderCallbacks);
        }
    }

    private void refreshPlace(boolean accountChanged) {
        if (accountChanged) {
            if (requestedSubreddit != null) {
                selectRequestedPlace();
            } else {
                selectLastPlace();
            }
        } else {
            selectCurrentPlace();
        }
    }

    private void selectRequestedPlace() {
        selectPlace(PLACE_SUBREDDIT,
                requestedSubreddit,
                Subreddits.isRandom(requestedSubreddit),
                AccountPrefs.getLastSubredditFilter(getActivity(), FilterAdapter.SUBREDDIT_HOT),
                requestedThingBundle,
                false,
                false);
        requestedSubreddit = null;
        requestedThingBundle = null;
    }

    private void selectLastPlace() {
        int place = AccountUtils.isAccount(accountName)
                ? AccountPrefs.getLastPlace(getActivity(), PLACE_SUBREDDIT)
                : PLACE_SUBREDDIT;
        selectPlaceWithDefaults(place, false);
    }

    private void selectCurrentPlace() {
        selectPlace(place, subreddit, isRandom, filter, null, true, false);
    }

    private void selectPlaceWithDefaults(int place, boolean force) {
        switch (place) {
            case PLACE_SUBREDDIT:
                String subreddit = AccountPrefs.getLastSubreddit(getActivity(),
                        accountName,
                        Subreddits.NAME_FRONT_PAGE);
                boolean isRandom = AccountPrefs.getLastIsRandom(getActivity(),
                        accountName,
                        false);
                int filter = AccountPrefs.getLastSubredditFilter(getActivity(),
                        FilterAdapter.SUBREDDIT_HOT);
                selectPlace(place, subreddit, isRandom, filter, null, true, force);
                break;

            case PLACE_PROFILE:
                selectPlaceWithNoSubreddit(place, FilterAdapter.PROFILE_OVERVIEW, force);
                break;

            case PLACE_SAVED:
                selectPlaceWithNoSubreddit(place, FilterAdapter.PROFILE_SAVED, force);
                break;

            case PLACE_MESSAGES:
                selectPlaceWithNoSubreddit(place, FilterAdapter.MESSAGE_INBOX, force);
                break;
        }
    }

    private void selectPlaceWithNoSubreddit(int place, int filter, boolean force) {
        selectPlace(place, null, false, filter, null, true, force);
    }

    private void selectPlaceWithFilter(int newFilter, boolean force) {
        selectPlace(place, subreddit, isRandom, newFilter, null, true, force);
    }

    private void selectPlace(int place,
            String subreddit,
            boolean isRandom,
            int filter,
            ThingBundle thingBundle,
            boolean savePrefs,
            boolean force) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "selectPlace accountName: " + accountName
                    + " place: " + place
                    + " subreddit: " + subreddit
                    + " isRandom: " + isRandom
                    + " filter: " + filter
                    + " thingBundle: " + thingBundle
                    + " savePrefs: " + savePrefs
                    + " force: " + force);
        }

        this.place = place;
        this.subreddit = subreddit;
        this.isRandom = isRandom;
        this.filter = filter;

        placesAdapter.setSelectedPlace(place);
        if (savePrefs) {
            AccountPrefs.setLastPlace(getActivity(), place);
        }

        switch (place) {
            case PLACE_SUBREDDIT:
                subredditAdapter.setSelectedSubreddit(isRandom
                        ? Subreddits.NAME_RANDOM
                        : subreddit);
                if (savePrefs) {
                    AccountPrefs.setLastSubreddit(getActivity(), accountName, subreddit);
                    AccountPrefs.setLastIsRandom(getActivity(), accountName, isRandom);
                    AccountPrefs.setLastSubredditFilter(getActivity(), filter);
                }

                if (Subreddits.isRandom(subreddit)) {
                    getLoaderManager().restartLoader(LOADER_RANDOM_SUBREDDIT,
                            newLoaderArgs(accountName),
                            randomLoaderCallbacks);
                } else if (listener != null) {
                    listener.onNavigationSubredditSelected(accountName,
                            subreddit,
                            isRandom,
                            filter,
                            thingBundle,
                            force);
                }
                break;

            case PLACE_PROFILE:
                getLoaderManager().destroyLoader(LOADER_RANDOM_SUBREDDIT);
                subredditAdapter.setSelectedSubreddit(null);
                if (listener != null) {
                    listener.onNavigationProfileSelected(accountName, filter, force);
                }
                break;

            case PLACE_SAVED:
                getLoaderManager().destroyLoader(LOADER_RANDOM_SUBREDDIT);
                subredditAdapter.setSelectedSubreddit(null);
                if (listener != null) {
                    listener.onNavigationSavedSelected(accountName, filter, force);
                }
                break;

            case PLACE_MESSAGES:
                getLoaderManager().destroyLoader(LOADER_RANDOM_SUBREDDIT);
                subredditAdapter.setSelectedSubreddit(null);
                if (listener != null) {
                    listener.onNavigationMessagesSelected(accountName, filter, force);
                }
                break;

            default:
                throw new IllegalArgumentException("place: " + place);
        }
    }

    @Override
    public void onAccountMessagesSelected(String accountName) {
        selectAccount(accountName);
        selectPlaceWithNoSubreddit(PLACE_MESSAGES, FilterAdapter.MESSAGE_UNREAD, true);
    }

    @Override
    public void onPlaceSelected(int place) {
        selectPlaceWithDefaults(place, false);
    }

    public void setFilter(int newFilter) {
        if (this.filter != newFilter) {
            selectPlaceWithFilter(newFilter, false);
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

            case ADAPTER_PLACES:
                throw new IllegalStateException();

            case ADAPTER_SUBREDDITS:
                handleSubredditClick(adapterPosition);
                break;
        }
    }

    private void handleAccountClick(int position) {
        Item item = accountAdapter.getItem(position);
        refreshAccount(item.getAccountName());
    }

    private void handleSubredditClick(int position) {
        String subreddit = subredditAdapter.getName(position);
        boolean isRandom = Subreddits.isRandom(subreddit);
        selectPlace(PLACE_SUBREDDIT, subreddit, isRandom, filter, null, true, true);
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
    public void onItemCheckedStateChanged(ActionMode mode,
            int position,
            long id,
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
        int position = Views.getCheckedPosition(getListView());
        int adapterPosition = mergeAdapter.getAdapterPosition(position);
        return subredditAdapter.getName(adapterPosition);
    }

    private void handleDelete() {
        String[] subreddits = getCheckedSubreddits();
        Provider.removeSubredditsAsync(getActivity(), accountName, subreddits);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REQUESTED_SUBREDDIT, requestedSubreddit);
        outState.putParcelable(STATE_REQUESTED_THING_BUNDLE, requestedThingBundle);

        outState.putString(STATE_ACCOUNT_NAME, accountName);
        outState.putInt(STATE_PLACE, place);
        outState.putString(STATE_SUBREDDIT, subreddit);
        outState.putBoolean(STATE_IS_RANDOM, isRandom);
        outState.putInt(STATE_FILTER, filter);
    }

    private static Bundle newLoaderArgs(String accountName) {
        Bundle args = new Bundle(1);
        args.putString(LOADER_ARG_ACCOUNT_NAME, accountName);
        return args;
    }

    class SubredditLoaderCallbacks implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String accountName = args.getString(LOADER_ARG_ACCOUNT_NAME);
            return new AccountSubredditListLoader(getActivity(), accountName);
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

    class RandomSubredditLoaderCallbacks implements LoaderCallbacks<String> {
        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            String accountName = args.getString(LOADER_ARG_ACCOUNT_NAME);
            return new RandomSubredditLoader(getActivity(), accountName);
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String resolvedSubreddit) {
            if (!TextUtils.isEmpty(resolvedSubreddit)) {
                selectPlace(place, resolvedSubreddit, true, filter, null, true, false);
            }
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
        }
    }
}
