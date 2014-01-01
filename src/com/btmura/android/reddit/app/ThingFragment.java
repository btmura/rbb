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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
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
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ComparableFragments;
import com.btmura.android.reddit.util.Strings;

public class ThingFragment extends Fragment implements
        ComparableFragment,
        LoaderCallbacks<ThingData>,
        OnPageChangeListener {

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_BUNDLE = "thingBundle";

    private OnThingEventListener listener;
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
    private MenuItem addSubredditItem;
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
    public boolean equalFragments(ComparableFragment o) {
        return ComparableFragments.equalClasses(this, o)
                && ComparableFragments.equalStrings(this, o, ARG_ACCOUNT_NAME);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingEventListener) {
            listener = (OnThingEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
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
        thingData = data;
        if (pagerAdapter == null) {
            pagerAdapter = new ThingPagerAdapter(getChildFragmentManager(),
                    getAccountName(),
                    data);
            pager.setAdapter(pagerAdapter);
            pager.setOnPageChangeListener(this);

            if (listener != null) {
                listener.onThingTitleDiscovery(thingData.parent.getDisplayTitle());
            }
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<ThingData> loader) {
        thingData = null;
    }

    @Override
    public void onPageSelected(int page) {
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
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
        addSubredditItem = menu.findItem(R.id.menu_thing_add_subreddit);
        subredditItem = menu.findItem(R.id.menu_thing_subreddit);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (linkItem == null || thingData == null) {
            return; // Bail out if the menu hasn't been created.
        }

        boolean hasLinkAndComments = thingData.parent.hasLinkUrl()
                && thingData.parent.hasCommentsUrl();
        linkItem.setVisible(hasLinkAndComments
                && !isCurrentPageType(ThingPagerAdapter.TYPE_LINK));
        commentsItem.setVisible(hasLinkAndComments
                && !isCurrentPageType(ThingPagerAdapter.TYPE_COMMENTS));

        savedItem.setVisible(thingData.isParentSaveable() && thingData.isParentSaved());
        unsavedItem.setVisible(thingData.isParentSaveable() && !thingData.isParentSaved());

        boolean hasAccount = AccountUtils.isAccount(getAccountName());
        newCommentItem.setVisible(hasAccount);

        String title = thingData.parent.getTitle();
        CharSequence url = getUrl();
        boolean hasTitleAndUrl = !TextUtils.isEmpty(title) && !TextUtils.isEmpty(url);
        openItem.setVisible(hasTitleAndUrl);
        copyUrlItem.setVisible(hasTitleAndUrl);
        shareItem.setVisible(hasTitleAndUrl);
        if (shareItem.isVisible()) {
            MenuHelper.setShareProvider(shareItem, title, url);
        }

        String user = thingData.parent.getAuthor();
        userItem.setVisible(!TextUtils.isEmpty(user));
        if (userItem.isVisible()) {
            userItem.setTitle(getString(R.string.menu_user, user));
        }

        String subreddit = thingData.parent.getSubreddit();
        boolean hasSidebar = Subreddits.hasSidebar(subreddit);
        addSubredditItem.setVisible(hasSidebar);
        subredditItem.setVisible(hasSidebar);
        if (subredditItem.isVisible()) {
            subredditItem.setTitle(getString(R.string.menu_subreddit, subreddit));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_link:
                handleLinkItem();
                return true;

            case R.id.menu_comments:
                handleComments();
                return true;

            case R.id.menu_saved:
                handleSaved();
                return true;

            case R.id.menu_unsaved:
                handleUnsaved();
                return true;

            case R.id.menu_new_comment:
                handleNewComment();
                return true;

            case R.id.menu_open:
                handleOpen();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl();
                return true;

            case R.id.menu_thing_add_subreddit:
                handleAddSubreddit();
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

    private void handleLinkItem() {
        setCurrentPageType(ThingPagerAdapter.PAGE_LINK, true);
    }

    private void handleComments() {
        setCurrentPageType(ThingPagerAdapter.PAGE_COMMENTS, true);
    }

    private void handleSaved() {
        Provider.saveAsync(getActivity(),
                getAccountName(),
                thingData.parent.getThingId(),
                null,
                false);
    }

    private void handleUnsaved() {
        Provider.saveAsync(getActivity(),
                getAccountName(),
                thingData.parent.getThingId(),
                thingData.parent,
                true);
    }

    private void handleNewComment() {
        switch (thingData.parent.getKind()) {
            case Kinds.KIND_LINK:
                handleNewLinkComment();
                break;

            case Kinds.KIND_MESSAGE:
                handleNewMessageComment();
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private void handleNewLinkComment() {
        String author = thingData.parent.getAuthor();
        String title = getFormattedTitle();
        String thingId = thingData.parent.getThingId();
        MenuHelper.startCommentReplyComposer(getActivity(),
                getAccountName(),
                author,
                title,
                thingId,
                thingId);
    }

    private void handleNewMessageComment() {
        String author = thingData.parent.getAuthor();
        String title = getFormattedTitle();
        String thingId = thingData.parent.getThingId();
        MenuHelper.startMessageReplyComposer(getActivity(),
                getAccountName(),
                author,
                title,
                thingId,
                thingId,
                true);
    }

    // TODO: Put the code to format title in a common class and remove duplication.
    private String getFormattedTitle() {
        return Strings.toString(Strings.ellipsize(thingData.parent.getTitle(), 50));
    }

    private void handleOpen() {
        MenuHelper.openUrl(getActivity(), getUrl());
    }

    private void handleCopyUrl() {
        MenuHelper.copyUrl(getActivity(), thingData.parent.getTitle(), getUrl());
    }

    private void handleAddSubreddit() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(), thingData.parent.getSubreddit());
    }

    private void handleUser() {
        MenuHelper.startProfileActivity(getActivity(), thingData.parent.getAuthor(), -1);
    }

    private void handleSubreddit() {
        MenuHelper.startSidebarActivity(getActivity(), thingData.parent.getSubreddit());
    }

    private CharSequence getUrl() {
        switch (getCurrentPageType()) {
            case ThingPagerAdapter.TYPE_LINK:
                return thingData.parent.getLinkUrl();

            case ThingPagerAdapter.TYPE_COMMENTS:
                return thingData.parent.getCommentsUrl();

            default:
                return null;
        }
    }

    private void setCurrentPageType(int pageType, boolean smoothScroll) {
        int position = pagerAdapter.findPageType(pageType);
        pager.setCurrentItem(position, smoothScroll);
        getActivity().invalidateOptionsMenu();
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
