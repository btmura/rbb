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
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingFragment extends Fragment {

    static final String TAG = "ThingFragment";

    private static final String ARG_ACCOUNT_NAME = "an";
    private static final String ARG_THING_BUNDLE = "tb";

    private Bundle thingBundle;
    private ThingPagerAdapter adapter;
    private ViewPager thingPager;

    private MenuItem linkItem;
    private MenuItem commentsItem;
    private MenuItem shareItem;
    private MenuItem openItem;
    private MenuItem copyUrlItem;
    private MenuItem userItem;
    private MenuItem subredditItem;

    public static ThingFragment newInstance(String accountName, Bundle thingBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putBundle(ARG_THING_BUNDLE, thingBundle);

        ThingFragment frag = new ThingFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thingBundle = getArguments().getBundle(ARG_THING_BUNDLE);
        adapter = new ThingPagerAdapter(getChildFragmentManager(), getAccountName(), thingBundle);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.thing_frag, container, false);
        thingPager = (ViewPager) view.findViewById(R.id.thing_pager);
        thingPager.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_frag_menu, menu);
        linkItem = menu.findItem(R.id.menu_link);
        commentsItem = menu.findItem(R.id.menu_comments);
        shareItem = menu.findItem(R.id.menu_share);
        openItem = menu.findItem(R.id.menu_open);
        copyUrlItem = menu.findItem(R.id.menu_copy_url);
        userItem = menu.findItem(R.id.menu_user);
        subredditItem = menu.findItem(R.id.menu_thing_subreddit);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        String title = getTitle();
        CharSequence url = getUrl();
        prepareLinkItem();
        prepareCommentsItem();
        prepareShareItem(title, url);
        prepareOpenItem(url);
        prepareCopyUrlItem(title, url);
        prepareUserItem();
        prepareSubredditItem();
    }

    private void prepareLinkItem() {
        if (linkItem != null) {
            linkItem.setVisible(ThingBundle.hasLinkUrl(thingBundle)
                    && getCurrentPageType() != ThingPagerAdapter.TYPE_LINK);
        }
    }

    private void prepareCommentsItem() {
        if (commentsItem != null) {
            commentsItem.setVisible(ThingBundle.hasCommentUrl(thingBundle)
                    && getCurrentPageType() != ThingPagerAdapter.TYPE_COMMENTS);
        }
    }

    private void prepareShareItem(String title, CharSequence url) {
        if (shareItem != null) {
            shareItem.setVisible(title != null && url != null);
            if (shareItem.isVisible()) {
                MenuHelper.setShareProvider(shareItem, title, url);
            }
        }
    }

    private void prepareOpenItem(CharSequence url) {
        if (openItem != null) {
            openItem.setVisible(url != null);
        }
    }

    private void prepareCopyUrlItem(String title, CharSequence url) {
        if (copyUrlItem != null) {
            copyUrlItem.setVisible(title != null && url != null);
        }
    }

    private void prepareUserItem() {
        if (userItem != null) {
            userItem.setTitle(getString(R.string.menu_user,
                    ThingBundle.getAuthor(thingBundle)));
        }
    }

    private void prepareSubredditItem() {
        if (subredditItem != null) {
            subredditItem.setTitle(getString(R.string.menu_subreddit,
                    ThingBundle.getSubreddit(thingBundle)));
        }
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

    private void handleOpenItem() {
        MenuHelper.startIntentChooser(getActivity(), getUrl());
    }

    private void handleCopyUrlItem() {
        MenuHelper.setClipAndToast(getActivity(), getTitle(), getUrl());
    }

    private void handleAddSubredditItem() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(), getSubreddit());
    }

    private void handleUserItem() {
        MenuHelper.startProfileActivity(getActivity(), getAuthor(), -1);
    }

    private void handleSubredditItem() {
        MenuHelper.startSidebarActivity(getActivity(), getSubreddit());
    }

    private CharSequence getUrl() {
        switch (getCurrentPageType()) {
            case ThingPagerAdapter.TYPE_LINK:
                return ThingBundle.getLinkUrl(thingBundle);

            case ThingPagerAdapter.TYPE_COMMENTS:
                return ThingBundle.getCommentUrl(thingBundle);

            default:
                return null;
        }
    }

    private void setCurrentPageType(int pageType, boolean smoothScroll) {
        int position = adapter.findPageType(pageType);
        thingPager.setCurrentItem(position, smoothScroll);
    }

    private int getCurrentPageType() {
        if (adapter != null && thingPager != null) {
            return adapter.getPageType(thingPager.getCurrentItem());
        }
        return -1;
    }

    private String getAuthor() {
        return ThingBundle.getAuthor(thingBundle);
    }

    private String getSubreddit() {
        return ThingBundle.getSubreddit(thingBundle);
    }

    private String getTitle() {
        return ThingBundle.getTitle(thingBundle);
    }

    private String getAccountName() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }
}
