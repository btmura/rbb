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

package com.btmura.android.reddit.activity;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.entity.Comment;
import com.btmura.android.reddit.provider.NetApi;
import com.btmura.android.reddit.widget.AccountSpinnerAdapter;

public class CommentReplyActivity extends Activity implements LoaderCallbacks<AccountResult>,
        OnItemSelectedListener {

    public static final String TAG = "CommentReplyActivity";

    public static final String EXTRA_COMMENT = "c";

    private AccountSpinnerAdapter adapter;

    private EditText text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comment_reply);
        setupActionBar();
        setupAccountSpinner();
        setupViews();
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupAccountSpinner() {
        adapter = new AccountSpinnerAdapter(this, false);
        Spinner accountSpinner = (Spinner) findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(adapter);
        accountSpinner.setOnItemSelectedListener(this);
        getLoaderManager().initLoader(0, null, this);
    }

    private void setupViews() {
        text = (EditText) findViewById(R.id.text);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        adapter.setAccountNames(result.accountNames);
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
    }

    public void onItemSelected(AdapterView<?> av, View v, int position, long id) {
        adapter.updateState(position);
    }

    public void onNothingSelected(AdapterView<?> av) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.comment_reply_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                handleReply();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void handleReply() {
        if (text.getText().length() <= 0) {
            text.setError(getString(R.string.error_blank_field));
            return;
        }

        Comment c = getIntent().getParcelableExtra(EXTRA_COMMENT);
        new ReplyTask(adapter.getAccountName(), c.name, text.getText().toString()).execute();
    }

    class ReplyTask extends AsyncTask<Void, Void, Void> {

        private final String accountName;
        private final String thingId;
        private final String text;

        ReplyTask(String accountName, String thingId, String text) {
            this.accountName = accountName;
            this.thingId = thingId;
            this.text = text;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                AccountManager manager = AccountManager.get(CommentReplyActivity.this);
                Account account = new Account(accountName, getString(R.string.account_type));
                String cookie = manager.blockingGetAuthToken(account,
                        AccountAuthenticator.AUTH_TOKEN_COOKIE,
                        true);
                String modhash = manager.blockingGetAuthToken(account,
                        AccountAuthenticator.AUTH_TOKEN_MODHASH,
                        true);
                NetApi.comment(thingId, text, cookie, modhash);
            } catch (IOException e) {
                Log.e(TAG, "doInBackground", e);
            } catch (OperationCanceledException e) {
                Log.e(TAG, "doInBackground", e);
            } catch (AuthenticatorException e) {
                Log.e(TAG, "doInBackground", e);
            }

            return null;
        }
    }
}
