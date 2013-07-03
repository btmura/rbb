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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.ThingDataLoader;
import com.btmura.android.reddit.content.ThingDataLoader.ThingData;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingFragment extends Fragment implements LoaderCallbacks<ThingData> {

    static final String TAG = "ThingFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_BUNDLE = "thingBundle";

    private ThingData thingData;
    private ThingPagerAdapter pagerAdapter;
    private ViewPager pager;

    private MenuItem linkItem;
    private MenuItem commentsItem;
    private MenuItem savedItem;
    private MenuItem unsavedItem;
    private MenuItem newCommentItem;
    private MenuItem shareItem;
    private MenuItem openItem;
    private MenuItem copyUrlItem;
    private MenuItem userItem;
    private MenuItem subredditItem;

    public static ThingFragment newInstance(String accountName, ThingBundle thingBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putParcelable(ARG_THING_BUNDLE, thingBundle);

        ThingFragment frag = new ThingFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.thing_frag, container, false);
        pager = (ViewPager) view.findViewById(R.id.thing_pager);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ThingData> onCreateLoader(int id, Bundle args) {
        return new ThingDataLoader(getActivity(),
                getAccountName(),
                getThingBundleArgument());
    }

    @Override
    public void onLoadFinished(Loader<ThingData> loader, ThingData data) {
        // Handle the fact that onLoadFinished is called twice after orientation changes.
        if (thingData != data) {
            thingData = data;
            getActivity().invalidateOptionsMenu();
        }

        if (pagerAdapter == null) {
            pagerAdapter = new ThingPagerAdapter(getChildFragmentManager(),
                    getAccountName(), data);
            pager.setAdapter(pagerAdapter);
        }
    }

    @Override
    public void onLoaderReset(Loader<ThingData> loader) {
        thingData = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_frag_menu, menu);
        linkItem = menu.findItem(R.id.menu_link);
        commentsItem = menu.findItem(R.id.menu_comments);
        savedItem = menu.findItem(R.id.menu_saved);
        unsavedItem = menu.findItem(R.id.menu_unsaved);
        newCommentItem = menu.findItem(R.id.menu_new_comment);
        openItem = menu.findItem(R.id.menu_open);
        shareItem = menu.findItem(R.id.menu_share);
        copyUrlItem = menu.findItem(R.id.menu_copy_url);
        userItem = menu.findItem(R.id.menu_user);
        subredditItem = menu.findItem(R.id.menu_thing_subreddit);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (linkItem == null || thingData == null) {
            return; // Bail out if the menu hasn't been created.
        }

        linkItem.setVisible(thingData.hasLinkUrl()
                && isCurrentPageType(ThingPagerAdapter.TYPE_LINK));
        commentsItem.setVisible(thingData.hasLinkUrl() && thingData.hasCommentsUrl()
                && isCurrentPageType(ThingPagerAdapter.TYPE_COMMENTS));

        savedItem.setVisible(thingData.isSaveable() && thingData.isSaved());
        unsavedItem.setVisible(thingData.isSaveable() && !thingData.isSaved());

        boolean hasAccount = AccountUtils.isAccount(getAccountName());
        newCommentItem.setVisible(hasAccount);

        String title = thingData.getDisplayTitle(getActivity());
        CharSequence url = getUrl();
        boolean hasTitleAndUrl = !TextUtils.isEmpty(title) && !TextUtils.isEmpty(url);
        openItem.setVisible(hasTitleAndUrl);
        copyUrlItem.setVisible(hasTitleAndUrl);
        shareItem.setVisible(hasTitleAndUrl);
        if (shareItem.isVisible()) {
            MenuHelper.setShareProvider(shareItem, title, url);
        }

        userItem.setTitle(getString(R.string.menu_user, thingData.getAuthor()));
        subredditItem.setTitle(getString(R.string.menu_subreddit, thingData.getSubreddit()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_link:
                handleLinkItem();
                return true;

            case R.id.menu_comments:
                handleCommentsItem();
                return true;

            case R.id.menu_saved:
                handleSaved();
                return true;

            case R.id.menu_unsaved:
                handleUnsaved();
                return true;

            case R.id.menu_open:
                handleOpenItem();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrlItem();
                return true;

            case R.id.menu_thing_add_subreddit:
                handleAddSubredditItem();
                return true;

            case R.id.menu_user:
                handleUserItem();
                return true;

            case R.id.menu_thing_subreddit:
                handleSubredditItem();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleLinkItem() {
        setCurrentPageType(ThingPagerAdapter.PAGE_LINK, true);
    }

    private void handleCommentsItem() {
        setCurrentPageType(ThingPagerAdapter.PAGE_COMMENTS, true);
    }

    private void handleSaved() {
        Provider.unsaveAsync(getActivity(), getAccountName(),
                thingData.getThingId());
    }

    private void handleUnsaved() {
        Provider.saveAsync(getActivity(), getAccountName(),
                thingData.getAuthor(),
                thingData.getCreatedUtc(),
                thingData.getDomain(),
                thingData.getDowns(),
                thingData.getLikes(),
                thingData.getNumComments(),
                thingData.isOver18(),
                thingData.getPermaLink(),
                thingData.getScore(),
                thingData.isSelf(),
                thingData.getSubreddit(),
                thingData.getThingId(),
                thingData.getThumbnailUrl(),
                thingData.getTitle(),
                thingData.getUps(),
                thingData.getUrl());
    }

    private void handleOpenItem() {
        MenuHelper.startIntentChooser(getActivity(), getUrl());
    }

    private void handleCopyUrlItem() {
        MenuHelper.setClipAndToast(getActivity(),
                thingData.getDisplayTitle(getActivity()), getUrl());
    }

    private void handleAddSubredditItem() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(), thingData.getSubreddit());
    }

    private void handleUserItem() {
        MenuHelper.startProfileActivity(getActivity(), thingData.getAuthor(), -1);
    }

    private void handleSubredditItem() {
        MenuHelper.startSidebarActivity(getActivity(), thingData.getSubreddit());
    }

    private CharSequence getUrl() {
        switch (getCurrentPageType()) {
            case ThingPagerAdapter.TYPE_LINK:
                return thingData.getLinkUrl();

            case ThingPagerAdapter.TYPE_COMMENTS:
                return thingData.getCommentsUrl();

            default:
                return null;
        }
    }

    private void setCurrentPageType(int pageType, boolean smoothScroll) {
        int position = pagerAdapter.findPageType(pageType);
        pager.setCurrentItem(position, smoothScroll);
    }

    private int getCurrentPageType() {
        if (pagerAdapter != null && pager != null) {
            return pagerAdapter.getPageType(pager.getCurrentItem());
        }
        return -1;
    }

    private boolean isCurrentPageType(int pageType) {
        return pageType == getCurrentPageType();
    }

    // Getters for arguments

    private String getAccountName() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }

    private ThingBundle getThingBundleArgument() {
        return getArguments().getParcelable(ARG_THING_BUNDLE);
    }
}
