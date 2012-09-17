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

package com.btmura.android.reddit.content;

import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.Provider;

public class AccountInitializer {

    public static void initializeAccount(Context context, String login, String cookie)
            throws RemoteException, OperationApplicationException, IOException {
        ArrayList<String> subreddits = RedditApi.getSubreddits(cookie);
        int count = subreddits.size();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                count + 2);
        ops.add(ContentProviderOperation.newDelete(Provider.SUBREDDITS_URI)
                .withSelection(Subreddits.SELECTION_ACCOUNT, new String[] {login})
                .build());
        ops.add(ContentProviderOperation.newInsert(Provider.SUBREDDITS_URI)
                .withValue(Subreddits.COLUMN_ACCOUNT, login)
                .withValue(Subreddits.COLUMN_NAME, Subreddits.NAME_FRONT_PAGE)
                .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_INSERTING)
                .build());
        for (int i = 0; i < count; i++) {
            ops.add(ContentProviderOperation.newInsert(Provider.SUBREDDITS_URI)
                    .withValue(Subreddits.COLUMN_ACCOUNT, login)
                    .withValue(Subreddits.COLUMN_NAME, subreddits.get(i))
                    .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_NORMAL)
                    .build());
        }
        ContentResolver cr = context.getContentResolver();
        cr.applyBatch(Provider.AUTHORITY, ops);
    }
}
