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
import com.btmura.android.reddit.app.ComposeFragment.OnComposeListener;

/**
 * {@link Activity} that displays a form for composing submissions and messages
 * and subsequently processing them.
 */
public class ComposeActivity extends Activity implements OnComposeFormListener,
        OnCaptchaGuessListener, OnComposeListener {

    /** Charsequence extra for the activity's title */
    static final String EXTRA_DESTINATION = "destination";

    /** Integer extra indicating the type of composition. */
    static final String EXTRA_COMPOSITION = "composition";

    /** Type of composition when submitting a link or text. */
    static final int COMPOSITION_SUBMISSION = ComposeFormFragment.COMPOSITION_SUBMISSION;

    /** Type of composition when replying to some comment. */
    static final int COMPOSITION_COMMENT = ComposeFormFragment.COMPOSITION_COMMENT;

    /** Type of composition when crafting a message. */
    static final int COMPOSITION_MESSAGE = ComposeFormFragment.COMPOSITION_MESSAGE;

    /** String extra with account name from compose dialog. */
    private static final String EXTRA_COMPOSE_ACCOUNT_NAME = "accountName";

    /** String extra with destination from compose dialog. */
    private static final String EXTRA_COMPOSE_DESTINATION = "destination";

    /** String extra with title from compose dialog. */
    private static final String EXTRA_COMPOSE_TITLE = "title";

    /** String extra with text from compose dialog. */
    private static final String EXTRA_COMPOSE_TEXT = "text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        setTitle(getComposeTitle());

        // No action bar will be available on large devices.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private String getComposeTitle() {
        switch (getIntent().getIntExtra(EXTRA_COMPOSITION, -1)) {
            case COMPOSITION_SUBMISSION:
                return getString(R.string.submit_link_label);

            case COMPOSITION_COMMENT:
                return getString(R.string.comment_reply_title,
                        getIntent().getStringExtra(EXTRA_COMPOSE_DESTINATION));

            case COMPOSITION_MESSAGE:
                return getString(R.string.compose_message_title);

            default:
                throw new IllegalArgumentException();
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        // Fragments will be restored on config changes.
        if (savedInstanceState != null) {
            return;
        }

        int composition = getIntent().getIntExtra(EXTRA_COMPOSITION, -1);
        String destination = getIntent().getStringExtra(EXTRA_COMPOSE_DESTINATION);
        ComposeFormFragment frag = ComposeFormFragment.newInstance(composition, destination);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.compose_form_container, frag);
        ft.commit();
    }

    public void onComposeForm(String accountName, String destination, String title, String text) {
        // Bundle up extras from the form to pass through the captcha fragment
        // and then back to callbacks in this activity.
        Bundle extras = new Bundle(4);
        extras.putString(EXTRA_COMPOSE_ACCOUNT_NAME, accountName);
        extras.putString(EXTRA_COMPOSE_DESTINATION, destination);
        extras.putString(EXTRA_COMPOSE_TITLE, title);
        extras.putString(EXTRA_COMPOSE_TEXT, text);
        CaptchaFragment.newInstance(extras).show(getFragmentManager(), CaptchaFragment.TAG);
    }

    public void onComposeFormCancelled() {
        finish();
    }

    public void onCaptchaGuess(String id, String guess, Bundle extras) {
        switch (getIntent().getIntExtra(EXTRA_COMPOSITION, -1)) {
            case COMPOSITION_SUBMISSION:
                SubmitLinkFragment f = SubmitLinkFragment.newInstance(id, guess, extras);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(f, SubmitLinkFragment.TAG);
                ft.commit();
                break;

            case COMPOSITION_COMMENT:
                break;

            case COMPOSITION_MESSAGE:
                String accountName = extras.getString(EXTRA_COMPOSE_ACCOUNT_NAME);
                String destination = extras.getString(EXTRA_COMPOSE_DESTINATION);
                String title = extras.getString(EXTRA_COMPOSE_TITLE);
                String text = extras.getString(EXTRA_COMPOSE_TEXT);
                ComposeFragment frag = ComposeFragment.newInstance(accountName, destination, title,
                        text, id, guess);
                ft = getFragmentManager().beginTransaction();
                ft.add(frag, ComposeFragment.TAG);
                ft.commit();
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public void onCaptchaCancelled() {
    }

    public void onCompose() {
        finish();
    }

    public void onComposeCancelled() {
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
