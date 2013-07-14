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
import android.text.TextUtils;
import android.view.View;
import android.widget.FilterQueryProvider;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;

/** {@link SubredditAdapter} that lists a user's subreddits. */
public class AccountSubredditAdapter extends SubredditAdapter {

    private static final String[] ACCOUNT_PROJECTION = {
            Accounts._ID,
            Accounts.COLUMN_ACCOUNT,
            Accounts.COLUMN_LINK_KARMA,
            Accounts.COLUMN_COMMENT_KARMA,
            Accounts.COLUMN_HAS_MAIL,
    };

    private static final String[] SUBREDDIT_PROJECTION = {
            Subreddits._ID,
            Subreddits.COLUMN_NAME
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_NAME = 1;

    private static final int ID_ACCOUNT = -1;
    private static final int ID_PRESET = -2;

    private static final MatrixCursor PRESETS_CURSOR = new MatrixCursor(SUBREDDIT_PROJECTION, 3);
    static {
        // Use negative IDs for presets. See isDeletable.
        PRESETS_CURSOR.newRow().add(ID_PRESET).add(Subreddits.NAME_FRONT_PAGE);
        PRESETS_CURSOR.newRow().add(ID_PRESET).add(Subreddits.NAME_ALL);
        PRESETS_CURSOR.newRow().add(ID_PRESET).add(Subreddits.NAME_RANDOM);
    }

    private final MatrixCursor accountCursor;
    private final boolean showPresets;
    private Cursor originalCursor;

    /** Creates an adapter for use with AutoCompleteTextView. */
    public static AccountSubredditAdapter newAutoCompleteInstance(Context context) {
        // Don't make it single choice for AutoCompleteTextView.
        return new AccountSubredditAdapter(context, false, true, null, false);
    }

    /** Creates an adapter that displays accounts and the selected account's subreddits. */
    public static AccountSubredditAdapter newAccountInstance(Context context,
            AccountResult accountResult, boolean singleChoice) {
        return new AccountSubredditAdapter(context, true, false, accountResult, singleChoice);
    }

    private AccountSubredditAdapter(Context context, boolean showPresets, boolean addFilter,
            AccountResult accountResult, boolean singleChoice) {
        super(context, singleChoice);
        this.accountCursor = createAccountCursor(accountResult);
        this.showPresets = showPresets;
        if (addFilter) {
            // Attach filter that executes a query as the user types.
            final Context appContext = context.getApplicationContext();
            setFilterQueryProvider(new FilterQueryProvider() {
                public Cursor runQuery(CharSequence constraint) {
                    return getFilterCursor(appContext, constraint);
                }
            });
        }
    }

    private MatrixCursor createAccountCursor(AccountResult result) {
        if (result != null && result.accountNames.length > 1) {
            int count = result.accountNames.length;
            MatrixCursor cursor = new MatrixCursor(ACCOUNT_PROJECTION, count);
            for (int i = 0; i < count; i++) {
                cursor.newRow().add(ID_ACCOUNT)
                        .add(result.accountNames[i])
                        .add(result.linkKarma[i])
                        .add(result.commentKarma[i])
                        .add(result.hasMail[i]);
            }
            return cursor;
        }
        return null;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (originalCursor != newCursor) {
            originalCursor = newCursor;
            if (showPresets) {
                Cursor[] cursors = accountCursor != null
                        ? new Cursor[] {accountCursor, PRESETS_CURSOR, newCursor}
                        : new Cursor[] {PRESETS_CURSOR, newCursor};
                newCursor = new MergeCursor(cursors);
            }
            return super.swapCursor(newCursor);
        }
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        SubredditView v = (SubredditView) view;
        long id = cursor.getLong(INDEX_ID);
        String name = cursor.getString(INDEX_NAME);
        if (id == ID_ACCOUNT) {
            v.setAccountData(name);
            v.setChosen(false);
        } else {
            v.setSubredditData(name, false, -1);
            v.setChosen(singleChoice && Objects.equalsIgnoreCase(selectedSubreddit, name));
        }
    }

    @Override
    public String getName(int position) {
        return getString(position, INDEX_NAME);
    }

    public boolean isDeletable(int position) {
        // Non-presets are deletable. Presets have negative ids.
        return getLong(position, INDEX_ID) >= 0;
    }

    /** Returns a {@link Cursor} using the constraint or null if not ready. */
    Cursor getFilterCursor(Context context, CharSequence constraint) {
        if (AccountUtils.isAccount(accountName) && !TextUtils.isEmpty(constraint)) {
            String namePattern = new StringBuilder(constraint).append("%").toString();
            ContentResolver cr = context.getApplicationContext().getContentResolver();
            return cr.query(SubredditProvider.SUBREDDITS_URI, SUBREDDIT_PROJECTION,
                    Subreddits.SELECT_NOT_DELETED_BY_ACCOUNT_AND_LIKE_NAME,
                    Array.of(accountName, namePattern),
                    Subreddits.SORT_BY_NAME);
        }
        return null;
    }
}
