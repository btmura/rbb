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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.btmura.android.reddit.util.ComparableFragments;
import com.btmura.android.reddit.widget.SearchSubredditAdapter;
import com.btmura.android.reddit.widget.SubredditAdapter;

public class SearchSubredditListFragment
        extends SubredditListFragment<SearchSubredditListController,
        SearchSubredditActionModeController,
        SearchSubredditAdapter>
        implements ComparableFragment {

    public interface OnSubredditSelectedListener {
        void onSubredditSelected(View view, String subreddit, boolean onLoad);
    }

    private OnSubredditSelectedListener listener;

    private AccountResultHolder accountResultHolder;

    public static SearchSubredditListFragment newInstance(String accountName, String query,
            boolean singleChoice) {
        Bundle args = new Bundle(3);
        args.putString(SearchSubredditListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(SearchSubredditListController.EXTRA_QUERY, query);
        args.putBoolean(SearchSubredditListController.EXTRA_SINGLE_CHOICE, singleChoice);

        SearchSubredditListFragment frag = new SearchSubredditListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public boolean fragmentEquals(ComparableFragment o) {
        return ComparableFragments.baseEquals(this, o)
                && ComparableFragments.equalStrings(this, o,
                        SearchSubredditListController.EXTRA_ACCOUNT_NAME)
                && ComparableFragments.equalStrings(this, o,
                        SearchSubredditListController.EXTRA_QUERY);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSubredditSelectedListener) {
            listener = (OnSubredditSelectedListener) activity;
        }
        if (activity instanceof AccountResultHolder) {
            accountResultHolder = (AccountResultHolder) activity;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        super.onLoadFinished(loader, cursor);
        SubredditAdapter adapter = controller.getAdapter();
        if (adapter.getCursor() != null && adapter.getCount() > 0
                && TextUtils.isEmpty(controller.getSelectedSubreddit())) {
            String subreddit = adapter.getName(0);
            controller.setSelectedSubreddit(subreddit);
            if (listener != null) {
                listener.onSubredditSelected(null, subreddit, true);
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        super.onListItemClick(l, view, position, id);
        if (listener != null) {
            listener.onSubredditSelected(view, controller.getSelectedSubreddit(), false);
        }
    }

    @Override
    protected SearchSubredditListController createController() {
        return new SearchSubredditListController(getActivity(), getArguments());
    }

    @Override
    protected SearchSubredditActionModeController createActionModeController(
            SearchSubredditListController controller) {
        return new SearchSubredditActionModeController(getActivity(), getFragmentManager(),
                controller.getAdapter(), accountResultHolder);
    }
}
