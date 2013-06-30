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

public class AccountSubredditListFragment
        extends SubredditListFragment<AccountSubredditListController,
        AccountSubredditListActionModeController> {

    public static AccountSubredditListFragment newInstance(String accountName,
            String selectedSubreddit, boolean singleChoice) {
        Bundle args = new Bundle(3);
        args.putString(AccountSubredditListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(AccountSubredditListController.EXTRA_SELECTED_SUBREDDIT, selectedSubreddit);
        args.putBoolean(AccountSubredditListController.EXTRA_SINGLE_CHOICE, singleChoice);

        AccountSubredditListFragment frag = new AccountSubredditListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    protected AccountSubredditListController createController() {
        return new AccountSubredditListController(getActivity(), getArguments());
    }

    @Override
    protected AccountSubredditListActionModeController createActionModeController(
            AccountSubredditListController controller) {
        return new AccountSubredditListActionModeController(getActivity(),
                controller.getAccountName(), controller.getAdapter());
    }
}
