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
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
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
import com.btmura.android.reddit.app.AbstractBrowserActivity.RightFragment;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.view.SwipeDismissTouchListener;
import com.btmura.android.reddit.view.SwipeDismissTouchListener.OnSwipeDismissListener;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingView;
import com.btmura.android.reddit.widget.ThumbnailLoader;

abstract class ThingListFragment<C extends ThingListController<?>, MC extends MenuController, AC extends ThingActionModeController>
        extends ListFragment
        implements LoaderCallbacks<Cursor>,
        RightFragment,
        Refreshable,
        OnScrollListener,
        OnSwipeDismissListener,
        OnVoteListener,
        MultiChoiceModeListener {

    /** String argument that is used to paginate things. */
    private static final String LOADER_MORE_ID = "moreId";

    public interface OnThingSelectedListener {
        void onThingSelected(View view, ThingBundle thingBundle, int pageType);

        int onThingBodyMeasure();
    }

    protected C controller;
    protected MC menuController;
    protected AC actionModeController;

    private OnThingSelectedListener listener;
    private boolean scrollLoading;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingSelectedListener) {
            listener = (OnThingSelectedListener) activity;
        }
    }

    protected abstract C createController();

    protected abstract MC createMenuController(C controller);

    protected abstract AC createActionModeController(C controller);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = createController();
        menuController = createMenuController(controller);
        actionModeController = createActionModeController(controller);
        if (savedInstanceState != null) {
            controller.restoreInstanceState(savedInstanceState);
            menuController.restoreInstanceState(savedInstanceState);
            actionModeController.restoreInstanceState(savedInstanceState);
        }
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

        ThumbnailLoader.lock(false);
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
        controller.setThingBodyWidth(getThingBodyWidth());
        setListAdapter(controller.getAdapter());
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void refresh() {
        controller.swapCursor(null);
        setListAdapter(controller.getAdapter());
        setListShown(false);
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        args = Objects.nullToEmpty(args);
        controller.setMoreId(args.getString(LOADER_MORE_ID));
        return controller.createLoader();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        controller.swapCursor(cursor);
        scrollLoading = false;
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        controller.swapCursor(null);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        ThumbnailLoader.lock(scrollState != SCROLL_STATE_IDLE);
    }

    @Override
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
        if (controller.isSingleChoice() && v instanceof ThingView) {
            ((ThingView) v).setChosen(true);
        }
    }

    private void selectThing(View v, int position, int pageType) {
        controller.setSelectedPosition(position);
        if (listener != null) {
            listener.onThingSelected(v, controller.getThingBundle(position), pageType);
        }
        controller.onThingSelected(position);
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return actionModeController.isSwipeable(position);
    }

    @Override
    public void onSwipeDismiss(ListView listView, View view, int position) {
        actionModeController.swipe(position);
    }

    @Override
    public void onStatusClick(View view) {
        int position = getListView().getPositionForView(view);
        actionModeController.clickStatus(position);
    }

    @Override
    public void onVoteClick(View v, int action) {
        int position = getListView().getPositionForView(v);
        actionModeController.vote(position, action);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menuController.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return menuController.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return actionModeController.onCreateActionMode(mode, menu, getListView());
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return actionModeController.onPrepareActionMode(mode, menu, getListView());
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
            boolean checked) {
        actionModeController.onItemCheckedStateChanged(mode, position, id, checked);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return actionModeController.onActionItemClicked(mode, item, getListView());
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionModeController.onDestroyActionMode(mode);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveInstanceState(outState);
        menuController.saveInstanceState(outState);
        actionModeController.saveInstanceState(outState);
    }

    private int getThingBodyWidth() {
        return listener != null ? listener.onThingBodyMeasure() : 0;
    }

    @Override
    public void setSelectedThing(String thingId, String linkId) {
        controller.setSelectedThing(thingId, linkId);
    }
}
