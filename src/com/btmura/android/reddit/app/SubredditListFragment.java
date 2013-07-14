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
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.view.SwipeDismissTouchListener;
import com.btmura.android.reddit.view.SwipeDismissTouchListener.OnSwipeDismissListener;
import com.btmura.android.reddit.widget.SubredditAdapter;
import com.btmura.android.reddit.widget.SubredditView;

abstract class SubredditListFragment<C extends SubredditListController<A>, AC extends ActionModeController, A extends SubredditAdapter>
        extends AbstractListFragment<C, AC, A>
        implements OnSwipeDismissListener {

    public static final String TAG = "SubredditListFragment";

    public interface OnSubredditSelectedListener {
        /**
         * Notifies the listener of the first subreddit in the loaded list. If there are no
         * subreddits, then subreddit is null. If there was an error, then subreddit is null but
         * error is true. Otherwise, subreddit is non-null with error set to false.
         */
        void onInitialSubredditSelected(String subreddit, boolean error);

        void onSubredditSelected(View view, String subreddit);
    }

    private OnSubredditSelectedListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSubredditSelectedListener) {
            listener = (OnSubredditSelectedListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);

        SwipeDismissTouchListener touchListener = new SwipeDismissTouchListener(listView, this);
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        return view;
    }

    @Override
    protected boolean showInitialLoadingSpinner() {
        // Only show the spinner if this is a single pane since showing two can be annoying.
        return controller.isSingleChoice();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        super.onLoadFinished(loader, cursor);
        if (listener != null) {
            if (cursor == null) {
                listener.onInitialSubredditSelected(null, true);
            } else if (cursor.getCount() == 0) {
                listener.onInitialSubredditSelected(null, false);
            } else {
                listener.onInitialSubredditSelected(controller.getSelectedSubreddit(), false);
            }
        }
    }

    @Override
    protected String getEmptyText(Cursor cursor) {
        if (controller.isSingleChoice()) {
            return ""; // Don't show duplicate message in multipane layout.
        }
        return super.getEmptyText(cursor);
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        String selectedSubreddit = controller.setSelectedPosition(position);
        if (controller.isSingleChoice() && view instanceof SubredditView) {
            ((SubredditView) view).setChosen(true);
        }
        if (listener != null) {
            listener.onSubredditSelected(view, selectedSubreddit);
        }
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return controller.isSwipeDismissable(position);
    }

    @Override
    public void onSwipeDismiss(ListView listView, View view, int position) {
        Provider.removeSubredditAsync(getActivity(), getAccountName(), getSubreddit(position));
    }

    public String getAccountName() {
        return controller.getAccountName();
    }

    public void setAccountName(String accountName) {
        controller.setAccountName(accountName);
    }

    public String getSelectedSubreddit() {
        return controller.getSelectedSubreddit();
    }

    public void setSelectedSubreddit(String subreddit) {
        controller.setSelectedSubreddit(subreddit);
    }

    private String getSubreddit(int position) {
        return controller.getAdapter().getName(position);
    }
}
