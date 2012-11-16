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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.CaptchaFragment.OnCaptchaGuessListener;
import com.btmura.android.reddit.app.SubmitLinkFragment.OnSubmitLinkListener;
import com.btmura.android.reddit.app.SubmitLinkFormFragment.OnSubmitFormListener;

public class SubmitLinkActivity extends Activity implements OnSubmitFormListener,
        OnCaptchaGuessListener, OnSubmitLinkListener {

    public static final String TAG = "SubmitLinkActivity";

    public static final String EXTRA_SUBREDDIT = "subreddit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_link);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            String subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.submit_link_container, SubmitLinkFormFragment.newInstance(subreddit));
            ft.commit();
        }
    }

    public void onSubmitForm(Bundle submitExtras) {
        CaptchaFragment.newInstance(submitExtras).show(getFragmentManager(), CaptchaFragment.TAG);
    }

    public void onSubmitFormCancelled() {
        finish();
    }

    public void onCaptchaGuess(String id, String guess, Bundle submitExtras) {
        SubmitLinkFragment f = SubmitLinkFragment.newInstance(submitExtras, id, guess);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(f, SubmitLinkFragment.TAG);
        ft.commit();
    }

    public void onCaptchaCancelled() {
    }

    public void onSubmitLink(String name, String url) {
        Toast.makeText(getApplicationContext(), url, Toast.LENGTH_LONG).show();
        finish();
    }

    public void onSubmitLinkCancelled() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
