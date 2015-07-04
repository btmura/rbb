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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.util.Array;

import java.util.Arrays;
import java.util.Comparator;

public class AccountLoader extends BaseAsyncTaskLoader<AccountResult>
    implements OnAccountsUpdateListener {

  public static final String TAG = "AccountLoader";

  public static class AccountResult {

    public String[] accountNames;
    public int[] linkKarma;
    public int[] commentKarma;
    public boolean[] hasMail;

    private AccountResult(
        String[] accountNames,
        int[] linkKarma,
        int[] commentKarma,
        boolean[] hasMail) {
      this.accountNames = accountNames;
      this.linkKarma = linkKarma;
      this.commentKarma = commentKarma;
      this.hasMail = hasMail;
    }

    public String getLastAccount(Context ctx) {
      String accountName =
          AccountPrefs.getLastAccount(ctx, Subreddits.ACCOUNT_NONE);
      int numAccounts = accountNames.length;
      for (int i = 0; i < numAccounts; i++) {
        if (accountNames[i].equals(accountName)) {
          return accountName;
        }
      }
      return numAccounts > 0 ? accountNames[0] : null;
    }
  }

  private static Comparator<Account> ACCOUNT_COMPARATOR = new Comparator<Account>() {
    public int compare(Account lhs, Account rhs) {
      return lhs.name.compareToIgnoreCase(rhs.name);
    }
  };

  private static final String[] PROJECTION = {
      Accounts._ID,
      Accounts.COLUMN_ACCOUNT,
      Accounts.COLUMN_LINK_KARMA,
      Accounts.COLUMN_COMMENT_KARMA,
      Accounts.COLUMN_HAS_MAIL,
  };

  private static final int INDEX_ACCOUNT = 1;
  private static final int INDEX_LINK_KARMA = 2;
  private static final int INDEX_COMMENT_KARMA = 3;
  private static final int INDEX_HAS_MAIL = 4;

  private final AccountManager manager;
  private final boolean includeNoAccount;
  private final boolean includeAccountInfo;
  private final ContentObserver observer;

  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context ctx, Intent intent) {
      String accountName =
          intent.getStringExtra(SelectAccountBroadcast.EXTRA_ACCOUNT);
      AccountPrefs.setLastAccount(ctx, accountName);
      onContentChanged();
    }
  };

  public AccountLoader(
      Context ctx,
      boolean includeNoAccount,
      boolean includeAccountInfo) {
    super(ctx);
    this.manager = AccountManager.get(getContext());
    this.includeNoAccount = includeNoAccount;
    this.includeAccountInfo = includeAccountInfo;
    this.observer = !includeAccountInfo ? null : new ContentObserver(null) {
      @Override
      public void onChange(boolean selfChange) {
        onContentChanged();
      }
    };
  }

  @Override
  public AccountResult loadInBackground() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "loadInBackground");
    }

    Context ctx = getContext();

    // Get the accounts and sort them.
    Account[] accounts = manager
        .getAccountsByType(AccountAuthenticator.getAccountType(ctx));
    Arrays.sort(accounts, ACCOUNT_COMPARATOR);

    // Convert to strings and prepend the no account at the top.
    int offset = includeNoAccount ? 1 : 0;
    int length = includeNoAccount ? accounts.length + 1 : accounts.length;
    String[] accountNames = new String[length];
    if (includeNoAccount) {
      accountNames[0] = Subreddits.ACCOUNT_NONE;
    }
    for (int i = 0; i < accounts.length; i++) {
      accountNames[i + offset] = accounts[i].name;
    }

    int[] linkKarma = null;
    int[] commentKarma = null;
    boolean[] hasMail = null;
    if (includeAccountInfo) {
      ContentResolver cr = getContext().getContentResolver();
      Cursor c = cr.query(AccountProvider.ACCOUNTS_URI, PROJECTION, null, null,
          null);
      linkKarma = Array.newIntArray(length, -1);
      commentKarma = Array.newIntArray(length, -1);
      hasMail = Array.newBooleanArray(length, false);
      for (int i = 0; i < accounts.length; i++) {
        String accountName = accountNames[i + offset];
        for (c.moveToPosition(-1); c.moveToNext(); ) {
          if (accountName.equals(c.getString(INDEX_ACCOUNT))) {
            linkKarma[i + offset] = c.getInt(INDEX_LINK_KARMA);
            commentKarma[i + offset] = c.getInt(INDEX_COMMENT_KARMA);
            hasMail[i + offset] = c.getInt(INDEX_HAS_MAIL) != 0;
            break;
          }
        }
      }
      c.close();
    }

    return new AccountResult(accountNames, linkKarma, commentKarma, hasMail);
  }

  @Override
  protected void onStartListening() {
    manager.addOnAccountsUpdatedListener(this, null, false);
    SelectAccountBroadcast.registerReceiver(getContext(), receiver);
    if (includeAccountInfo) {
      getContext().getContentResolver().registerContentObserver(
          AccountProvider.ACCOUNTS_URI,
          true, observer);
    }
  }

  @Override
  protected void onStopListening() {
    manager.removeOnAccountsUpdatedListener(this);
    SelectAccountBroadcast.unregisterReceiver(getContext(), receiver);
    if (includeAccountInfo) {
      getContext().getContentResolver().unregisterContentObserver(observer);
    }
  }

  @Override
  public void onAccountsUpdated(Account[] accounts) {
    onContentChanged();
  }
}
