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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.CaptchaFragment.OnCaptchaGuessListener;
import com.btmura.android.reddit.app.ComposeFormFragment.OnComposeFormListener;

/**
 * {@link Activity} that displays a form for composing submissions and messages
 * and subsequently processing them.
 */
public class ComposeActivity extends Activity implements OnComposeFormListener,
        OnCaptchaGuessListener {

    /** Charsequence extra for the activity's title */
    public static final String EXTRA_TITLE = "title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getCharSequenceExtra(EXTRA_TITLE));
        setContentView(R.layout.compose);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        // No action bar will be available on large devices.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        // Fragments will be restored on config changes.
        if (savedInstanceState != null) {
            return;
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.compose_form_container,
                ComposeFormFragment.newInstance(ComposeFormFragment.COMPOSITION_MESSAGE));
        ft.commit();
    }

    public void onComposeForm(String accountName, String destination, String title, String text) {
        Bundle extras = new Bundle();
        CaptchaFragment.newInstance(extras).show(getFragmentManager(), CaptchaFragment.TAG);
    }

    public void onComposeFormCancelled() {
        finish();
    }

    public void onCaptchaGuess(String id, String guess, Bundle extras) {
    }

    public void onCaptchaCancelled() {
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
