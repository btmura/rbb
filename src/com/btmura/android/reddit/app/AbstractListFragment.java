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
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.btmura.android.reddit.R;

abstract class AbstractListFragment<C extends Controller<A>, MC extends MenuController, AC extends ActionModeController, A extends ListAdapter>
        extends ListFragment
        implements LoaderCallbacks<Cursor>, MultiChoiceModeListener {

    protected C controller;
    protected MC menuController;
    protected AC actionModeController;

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
    }

    protected abstract C createController();

    protected abstract MC createMenuController(C controller);

    protected abstract AC createActionModeController(C controller);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(controller.getAdapter());
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return controller.createLoader();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        controller.swapCursor(cursor);
        setEmptyText(getEmptyText(cursor));
        setListShown(true);
        if (cursor != null) {
            actionModeController.invalidateActionMode();
        }
    }

    private String getEmptyText(Cursor cursor) {
        return getString(cursor != null ? R.string.empty_list : R.string.error);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        controller.swapCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
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
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
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
}
