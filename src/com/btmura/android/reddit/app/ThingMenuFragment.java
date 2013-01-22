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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_BUNDLE = "thingBundle";

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

    private boolean newCommentVisible;
    private boolean newCommentEnabled;
    private boolean savable;
    private boolean saved;

    private MenuItem savedItem;
    private MenuItem unsavedItem;
    private MenuItem newCommentItem;
    private MenuItem userItem;
    private MenuItem subredditItem;

    public static ThingMenuFragment newInstance(String accountName, Bundle thingBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putBundle(ARG_THING_BUNDLE, thingBundle);
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
        newCommentVisible = isNewCommentVisible();
        savable = isSavable();
        saved = isInitiallySaved();
        setHasOptionsMenu(true);
    }

    public void setNewCommentItemEnabled(boolean enabled) {
        this.newCommentEnabled = enabled;
        refreshNewCommentItem();
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
        refreshSaveItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu_menu, menu);
        savedItem = menu.findItem(R.id.menu_saved);
        unsavedItem = menu.findItem(R.id.menu_unsaved);
        newCommentItem = menu.findItem(R.id.menu_new_comment);
        userItem = menu.findItem(R.id.menu_user);
        subredditItem = menu.findItem(R.id.menu_thing_subreddit);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        refreshMenuItems();
    }

    private void refreshMenuItems() {
        refreshNewCommentItem();
        refreshSaveItems();
        refreshUserItems();
        refreshSubredditItem();
    }

    private void refreshNewCommentItem() {
        if (newCommentItem != null) {
            newCommentItem.setVisible(newCommentVisible);
            newCommentItem.setEnabled(newCommentEnabled);
        }
    }

    private void refreshSaveItems() {
        if (savedItem != null && unsavedItem != null) {
            savedItem.setVisible(savable && saved);
            unsavedItem.setVisible(savable && !saved);
        }
    }

    private void refreshUserItems() {
        if (userItem != null) {
            userItem.setTitle(getString(R.string.menu_user, getUser()));
        }
    }

    private void refreshSubredditItem() {
        if (subredditItem != null) {
            boolean visible = hasSubreddit();
            subredditItem.setVisible(visible);
            if (visible) {
                subredditItem.setTitle(getString(R.string.menu_subreddit, getSubreddit()));
            }
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

            case R.id.menu_user:
                handleUser();
                return true;

            case R.id.menu_thing_subreddit:
                handleSubreddit();
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

    private void handleUser() {
        MenuHelper.startProfileActivity(getActivity(), getUser(), -1);
    }

    private void handleSubreddit() {
        MenuHelper.startSidebarActivity(getActivity(), getSubreddit());
    }

    private String getAccountName() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }

    private boolean hasAccountName() {
        return AccountUtils.isAccount(getAccountName());
    }

    private boolean hasSubreddit() {
        return Subreddits.hasSidebar(getSubreddit());
    }

    private String getSubreddit() {
        return ThingBundle.getSubreddit(getThingBundle());
    }

    private String getUser() {
        return ThingBundle.getAuthor(getThingBundle());
    }

    private Bundle getThingBundle() {
        return getArguments().getBundle(ARG_THING_BUNDLE);
    }

    private boolean isNewCommentVisible() {
        return hasAccountName();
    }

    private boolean isSavable() {
        return hasAccountName() && ThingBundle.isSavable(getThingBundle());
    }

    private boolean isInitiallySaved() {
        return ThingBundle.isSaved(getThingBundle());
    }
}
