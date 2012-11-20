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

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ClipHelper;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.widget.ThingPagerAdapter;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_THING_BUNDLE = "tb";

    public interface ThingPagerHolder {
        ViewPager getPager();
    }

    private Bundle thingBundle;

    public static ThingMenuFragment newInstance(Bundle thingBundle) {
        Bundle args = new Bundle(1);
        args.putBundle(ARG_THING_BUNDLE, thingBundle);
        ThingMenuFragment f = new ThingMenuFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thingBundle = getArguments().getBundle(ARG_THING_BUNDLE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu, menu);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean onlyComments = Things.isSelf(thingBundle)
                || Things.getKind(thingBundle) == Things.KIND_COMMENT;
        boolean showingLink = isShowingLink();
        boolean showLink = !onlyComments && !showingLink;
        boolean showComments = !onlyComments && showingLink;

        menu.findItem(R.id.menu_link).setVisible(showLink);
        menu.findItem(R.id.menu_comments).setVisible(showComments);

        updateShareProvider(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_thing_sidebar:
                handleViewSidebar();
                return true;

            case R.id.menu_link:
                handleLink();
                return true;

            case R.id.menu_comments:
                handleComments();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl();
                return true;

            case R.id.menu_open:
                handleOpen();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateShareProvider(Menu menu) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, Things.getTitle(thingBundle));
        intent.putExtra(Intent.EXTRA_TEXT, getLink());

        ShareActionProvider shareActionProvider =
                (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
        shareActionProvider.setShareIntent(intent);
    }

    private void handleViewSidebar() {
        Intent intent = new Intent(getActivity(), SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, Things.getSubreddit(thingBundle));
        startActivity(intent);
    }

    private void handleLink() {
        getHolder().getPager().setCurrentItem(0);
    }

    private void handleComments() {
        getHolder().getPager().setCurrentItem(1);
    }

    private void handleCopyUrl() {
        ClipHelper.setClipToast(getActivity(), Things.getTitle(thingBundle), getLink());
    }

    private void handleOpen() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getLink().toString()));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_open)));
    }

    private boolean isShowingLink() {
        int position = getHolder().getPager().getCurrentItem();
        return ThingPagerAdapter.getType(thingBundle, position) == ThingPagerAdapter.TYPE_LINK;
    }

    private CharSequence getLink() {
        if (isShowingLink()) {
            return Things.getUrl(thingBundle);
        } else {
            return Urls.permaUrl(Things.getPermaLink(thingBundle), null).toExternalForm();
        }
    }

    private ThingPagerHolder getHolder() {
        return (ThingPagerHolder) getActivity();
    }
}
