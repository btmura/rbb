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

    private MenuItem openItem;
    private MenuItem copyUrlItem;

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
        openItem = menu.findItem(R.id.menu_open);
        copyUrlItem = menu.findItem(R.id.menu_copy_url);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        prepareOpen();
        prepareCopyUrl();
    }

    private void prepareOpen() {
        if (openItem != null) {
            openItem.setVisible(getUrl() != null);
        }
    }

    private void prepareCopyUrl() {
        if (copyUrlItem != null) {
            copyUrlItem.setVisible(getTitle() != null && getUrl() != null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open:
                handleOpen();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleOpen() {
        MenuHelper.startIntentChooser(getActivity(), getUrl());
    }

    private void handleCopyUrl() {
        MenuHelper.setClipAndToast(getActivity(), getTitle(), getUrl());
    }

    private CharSequence getTitle() {
        return ThingBundle.getTitle(thingBundle);
    }

    private CharSequence getUrl() {
        int pageType = adapter.getPageType(thingPager.getCurrentItem());
        switch (pageType) {
            case ThingPagerAdapter.TYPE_LINK:
                return ThingBundle.getLinkUrl(thingBundle);

            case ThingPagerAdapter.TYPE_COMMENTS:
                return ThingBundle.getCommentUrl(thingBundle);

            default:
                throw new IllegalStateException();
        }
    }

    private String getAccountName() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }
}
