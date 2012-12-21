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
package com.btmura.android.reddit.database;

import android.database.Cursor;
import android.test.ProviderTestCase2;

import com.btmura.android.reddit.provider.AccountProvider;

/**
 * Test for {@link AccountProvider}.
 */
public class AccountProviderTest extends ProviderTestCase2<AccountProvider> {

    public AccountProviderTest() {
        super(AccountProvider.class, AccountProvider.AUTHORITY);
    }

    public void testQuery() {
        AccountProvider provider = getProvider();
        Cursor c = provider.query(AccountProvider.ACCOUNTS_URI, null, null, null, null);
        assertNotNull(c);
        c.close();
    }
}
