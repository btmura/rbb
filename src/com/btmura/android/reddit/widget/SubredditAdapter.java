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

package com.btmura.android.reddit.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;

// TODO: Split this into an interface and 2 adapters.
public class SubredditAdapter extends LoaderAdapter {

    public static final String TAG = "SubredditAdapter";

    private static final String[] PROJECTION_SUBREDDITS = {
            Subreddits._ID,
            Subreddits.COLUMN_NAME
    };

    private static final String[] PROJECTION_SEARCH = {
            SubredditResults._ID,
            SubredditResults.COLUMN_NAME,
            SubredditResults.COLUMN_SUBSCRIBERS,
            SubredditResults.COLUMN_OVER_18,
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_NAME = 1;
    private static final int INDEX_SUBSCRIBERS = 2;
    private static final int INDEX_OVER_18 = 3;

    private static final MatrixCursor PRESETS_CURSOR = new MatrixCursor(PROJECTION_SUBREDDITS, 3);
    static {
        // Use negative IDs for presets. See isDeletable.
        PRESETS_CURSOR.newRow().add(-1).add(Subreddits.NAME_FRONT_PAGE);
        PRESETS_CURSOR.newRow().add(-2).add(Subreddits.NAME_ALL);
        PRESETS_CURSOR.newRow().add(-3).add(Subreddits.NAME_RANDOM);
    }

    private final boolean showPresets;
    private final boolean singleChoice;

    private long sessionId = -1;
    private String accountName;
    private String selectedSubreddit;
    private String query;

    /** Creates an adapter for showing the user's subreddits with presets. */
    public static SubredditAdapter newSubredditsInstance(Context context, boolean singleChoice) {
        return new SubredditAdapter(context, true, singleChoice);
    }

    /** Creates an adapter for searching for subreddits. */
    public static SubredditAdapter newSearchInstance(Context context, String query,
            boolean singleChoice) {
        // Don't show presets when searching. Single choice depends on layout.
        SubredditAdapter adapter = new SubredditAdapter(context, false, singleChoice);
        adapter.setQuery(query);
        return adapter;
    }

    /** Creates an adapter for use with AutoCompleteTextView. */
    public static SubredditAdapter newAutoCompleteInstance(Context context) {
        // Don't show presets or make it single choice for AutoCompleteTextView.
        final SubredditAdapter adapter = new SubredditAdapter(context, false, false);

        // Attach FilterQueryProvider that executes a query as the user types.
        final Context appContext = context.getApplicationContext();
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return adapter.getFilterCursor(appContext, constraint);
            }
        });
        return adapter;
    }

    private SubredditAdapter(Context context, boolean showPresets, boolean singleChoice) {
        super(context, null, 0);
        this.showPresets = showPresets;
        this.singleChoice = singleChoice;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (showPresets) {
            newCursor = new MergeCursor(new Cursor[] {PRESETS_CURSOR, newCursor});
        }
        return super.swapCursor(newCursor);
    }

    @Override
    public boolean isLoadable() {
        return accountName != null;
    }

    @Override
    protected Uri getLoaderUri() {
        if (isQuery()) {
            return ThingProvider.subredditSearchUri(sessionId, accountName, query);
        } else {
            return SubredditProvider.SUBREDDITS_URI;
        }
    }

    @Override
    protected String[] getProjection() {
        return isQuery() ? PROJECTION_SEARCH : PROJECTION_SUBREDDITS;
    }

    @Override
    protected String getSelection() {
        return isQuery() ? null : Subreddits.SELECT_BY_ACCOUNT_NOT_DELETED;
    }

    @Override
    protected String[] getSelectionArgs() {
        return isQuery() ? null : Array.of(accountName);
    }

    @Override
    protected String getSortOrder() {
        return isQuery() ? SubredditResults.SORT_BY_NAME : Subreddits.SORT_BY_NAME;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new SubredditView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String name = cursor.getString(INDEX_NAME);
        int subscribers = query != null ? cursor.getInt(INDEX_SUBSCRIBERS) : -1;
        boolean over18 = query != null && cursor.getInt(INDEX_OVER_18) == 1;
        SubredditView v = (SubredditView) view;
        v.setData(name, over18, subscribers);
        v.setChosen(singleChoice && Objects.equalsIgnoreCase(selectedSubreddit, name));
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        if (!Objects.equals(this.accountName, accountName)) {
            this.accountName = accountName;
            notifyDataSetChanged();
        }
    }

    public String getSelectedSubreddit() {
        return selectedSubreddit;
    }

    public void setSelectedSubreddit(String subreddit) {
        if (!Objects.equals(selectedSubreddit, subreddit)) {
            selectedSubreddit = subreddit;
            notifyDataSetChanged();
        }
    }

    public String setSelectedPosition(int position) {
        String subreddit = getString(position, INDEX_NAME);
        setSelectedSubreddit(subreddit);
        return subreddit;
    }

    public String getQuery() {
        return query;
    }

    void setQuery(String query) {
        this.query = query;
    }

    public boolean isQuery() {
        return !TextUtils.isEmpty(query);
    }

    public String getName(int position) {
        return getString(position, INDEX_NAME);
    }

    public boolean isDeletable(int position) {
        // Non-presets are deletable. Presents have negative ids.
        return getLong(position, INDEX_ID) >= 0;
    }

    /** Returns a {@link Cursor} using the constraint or null if not ready. */
    Cursor getFilterCursor(Context context, CharSequence constraint) {
        if (AccountUtils.isAccount(accountName) && !TextUtils.isEmpty(constraint)) {
            String namePattern = new StringBuilder(constraint).append("%").toString();
            ContentResolver cr = context.getApplicationContext().getContentResolver();
            return cr.query(SubredditProvider.SUBREDDITS_URI, PROJECTION_SUBREDDITS,
                    Subreddits.SELECT_NOT_DELETED_BY_ACCOUNT_AND_LIKE_NAME,
                    Array.of(accountName, namePattern),
                    Subreddits.SORT_BY_NAME);
        }
        return null;
    }
}
