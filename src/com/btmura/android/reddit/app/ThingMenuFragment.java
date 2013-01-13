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
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_AUTHOR = "author";

    interface ThingMenuEventListenerHolder {
        void setOnThingMenuEventListener(OnThingMenuEventListener listener);
    }

    /**
     * Interface that activities should implement to be aware of when the user
     * selects menu items that ThingMenuFragment cannot handle on its own.
     */
    interface OnThingMenuEventListener {

        /** Listener method fired when the user clicks the new item. */
        void onNewItemSelected();

        /** Listener method fired when the user clicks the saved item. */
        void onSavedItemSelected();

        /** Listener method fired when the user clicks the unsaved item. */
        void onUnsavedItemSelected();
    }

    private OnThingMenuEventListener listener;

    private boolean showNewCommentItem;
    private boolean showSaveItems;
    private boolean saved;

    private MenuItem savedItem;
    private MenuItem unsavedItem;
    private MenuItem newCommentItem;
    private MenuItem authorItem;
    private MenuItem subredditItem;
    private MenuItem aboutSubredditItem;
    private MenuItem addSubredditItem;
    private MenuItem viewSubredditItem;

    public static ThingMenuFragment newInstance(String subreddit, String author) {
        Bundle args = new Bundle(3);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_AUTHOR, author);
        ThingMenuFragment frag = new ThingMenuFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingMenuEventListener) {
            listener = (OnThingMenuEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void setMenuItems(boolean showNewCommentItem, boolean showSaveItems, boolean saved) {
        this.showNewCommentItem = showNewCommentItem;
        this.showSaveItems = showSaveItems;
        this.saved = saved;
        refreshMenuItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu_menu, menu);
        savedItem = menu.findItem(R.id.menu_saved);
        unsavedItem = menu.findItem(R.id.menu_unsaved);
        newCommentItem = menu.findItem(R.id.menu_new_comment);
        authorItem = menu.findItem(R.id.menu_author);
        subredditItem = menu.findItem(R.id.menu_subreddit);
        aboutSubredditItem = menu.findItem(R.id.menu_about_thing_subreddit);
        addSubredditItem = menu.findItem(R.id.menu_add_thing_subreddit);
        viewSubredditItem = menu.findItem(R.id.menu_view_subreddit);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        refreshMenuItems();
    }

    private void refreshMenuItems() {
        // If one item is found, then all the others have been found too.
        if (savedItem != null) {
            savedItem.setVisible(showSaveItems && saved);
            unsavedItem.setVisible(showSaveItems && !saved);
            newCommentItem.setVisible(showNewCommentItem);
            authorItem.setVisible(!TextUtils.isEmpty(getAuthor()));

            boolean hasSubreddit = Subreddits.hasSidebar(getSubreddit());
            subredditItem.setVisible(hasSubreddit);
            aboutSubredditItem.setVisible(hasSubreddit);
            addSubredditItem.setVisible(hasSubreddit);
            viewSubredditItem.setVisible(hasSubreddit);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_saved:
                handleSaved();
                return true;

            case R.id.menu_unsaved:
                handleUnsaved();
                return true;

            case R.id.menu_new_comment:
                handleNewComment();
                return true;

            case R.id.menu_author:
                handleAuthor();
                return true;

            case R.id.menu_about_thing_subreddit:
                handleAboutSubreddit();
                return true;

            case R.id.menu_add_thing_subreddit:
                handleAddSubreddit();
                return true;

            case R.id.menu_view_subreddit:
                handleViewSubreddit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleSaved() {
        if (listener != null) {
            listener.onSavedItemSelected();
        }
    }

    private void handleUnsaved() {
        if (listener != null) {
            listener.onUnsavedItemSelected();
        }
    }

    private void handleNewComment() {
        if (listener != null) {
            listener.onNewItemSelected();
        }
    }

    private void handleAuthor() {
        MenuHelper.startProfileActivity(getActivity(), getAuthor(), -1);
    }

    private void handleAboutSubreddit() {
        MenuHelper.startSidebarActivity(getActivity(), getSubreddit());
    }

    private void handleAddSubreddit() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(), getSubreddit());
    }

    private void handleViewSubreddit() {
        MenuHelper.startSubredditActivity(getActivity(), getSubreddit());
    }

    private String getSubreddit() {
        return getArguments().getString(ARG_SUBREDDIT);
    }

    private String getAuthor() {
        return getArguments().getString(ARG_AUTHOR);
    }
}
