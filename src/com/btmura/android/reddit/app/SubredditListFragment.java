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
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.view.SwipeDismissTouchListener;
import com.btmura.android.reddit.view.SwipeDismissTouchListener.OnSwipeDismissListener;
import com.btmura.android.reddit.widget.SubredditView;

abstract class SubredditListFragment<C extends SubredditListController<?>, AC extends SearchSubredditListActionModeController>
        extends ThingProviderListFragment
        implements OnSwipeDismissListener, MultiChoiceModeListener {

    public static final String TAG = "SubredditListFragment";

    public interface OnSubredditSelectedListener {
        /**
         * Notifies the listener of the first subreddit in the loaded list. If there are no
         * subreddits, then subreddit is null. If there was an error, then subreddit is null but
         * error is true. Otherwise, subreddit is non-null with error set to false.
         */
        void onInitialSubredditSelected(String subreddit, boolean error);

        void onSubredditSelected(View view, String subreddit);
    }

    protected C controller;
    protected AC actionModeController;

    private OnSubredditSelectedListener listener;

    protected abstract C createController();

    protected abstract AC createActionModeController(C controller);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSubredditSelectedListener) {
            listener = (OnSubredditSelectedListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = createController();
        actionModeController = createActionModeController(controller);
        if (savedInstanceState != null) {
            controller.restoreInstanceState(savedInstanceState);
            actionModeController.restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);

        SwipeDismissTouchListener touchListener = new SwipeDismissTouchListener(listView, this);
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(controller.getAdapter());
        // Only show the spinner if this is a single pane display since showing
        // two spinners can be annoying.
        setListShown(controller.isSingleChoice());
        loadIfPossible();
    }

    public void loadIfPossible() {
        if (controller.isLoadable()) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return controller.createLoader();
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (controller.swapCursor(cursor)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onLoadFinished");
            }

            // TODO: Remove dependency on ThingProviderListFragment.
            super.onLoadFinished(loader, cursor);

            setEmptyText(getEmptyText(cursor == null));
            setListShown(true);

            actionModeController.invalidateActionMode();
            if (listener != null) {
                if (cursor == null) {
                    listener.onInitialSubredditSelected(null, true);
                } else if (cursor.getCount() == 0) {
                    listener.onInitialSubredditSelected(null, false);
                } else {
                    listener.onInitialSubredditSelected(getSubreddit(0), false);
                }
            }
        }
    }

    private String getEmptyText(boolean error) {
        if (controller.isSingleChoice()) {
            return ""; // Don't show duplicate message in multipane layout.
        }
        return getString(error ? R.string.error : R.string.empty_list);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        throw new IllegalStateException();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        controller.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        String selectedSubreddit = controller.setSelectedPosition(position);
        if (controller.isSingleChoice() && view instanceof SubredditView) {
            ((SubredditView) view).setChosen(true);
        }
        if (listener != null) {
            listener.onSubredditSelected(view, selectedSubreddit);
        }
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return controller.isSwipeDismissable(position);
    }

    @Override
    public void onSwipeDismiss(ListView listView, View view, int position) {
        Provider.removeSubredditAsync(getActivity(), getAccountName(), getSubreddit(position));
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return actionModeController.onCreateActionMode(mode, menu, getListView());
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return actionModeController.onPrepareActionMode(mode, menu, getListView());
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        actionModeController.onItemCheckedStateChanged(mode, position, id, checked);
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return actionModeController.onActionItemClicked(mode, item, getListView());
    }

    public void onDestroyActionMode(ActionMode mode) {
        actionModeController.onDestroyActionMode(mode);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveInstanceState(outState);
        actionModeController.saveInstanceState(outState);
    }

    public String getAccountName() {
        return controller.getAccountName();
    }

    public void setAccountName(String accountName) {
        controller.setAccountName(accountName);
    }

    public String getSelectedSubreddit() {
        return controller.getSelectedSubreddit();
    }

    public void setSelectedSubreddit(String subreddit) {
        controller.setSelectedSubreddit(subreddit);
    }

    private String getSubreddit(int position) {
        return controller.getAdapter().getName(position);
    }
}
