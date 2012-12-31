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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.CaptchaFragment.OnCaptchaGuessListener;
import com.btmura.android.reddit.app.ComposeFormFragment.OnComposeFormListener;
import com.btmura.android.reddit.app.ComposeFragment.OnComposeListener;
import com.btmura.android.reddit.app.SubmitLinkFragment.OnSubmitLinkListener;
import com.btmura.android.reddit.provider.Provider;

/**
 * {@link Activity} that displays a form for composing submissions and messages
 * and subsequently processing them.
 */
public class ComposeActivity extends Activity implements OnComposeFormListener,
        OnCaptchaGuessListener, OnComposeListener, OnSubmitLinkListener {

    /** Charsequence extra for the activity's title */
    static final String EXTRA_DESTINATION = "destination";

    /** Integer extra indicating the type of composition. */
    static final String EXTRA_COMPOSITION = "composition";

    /** Bundle of extras to pass through. */
    static final String EXTRA_EXTRAS = "extras";

    /** Type of composition when submitting a link or text. */
    static final int COMPOSITION_SUBMISSION = ComposeFormFragment.COMPOSITION_SUBMISSION;

    /** Type of composition when crafting a new message. */
    static final int COMPOSITION_MESSAGE = ComposeFormFragment.COMPOSITION_MESSAGE;

    /** Type of composition when replying to some comment. */
    static final int COMPOSITION_COMMENT_REPLY = ComposeFormFragment.COMPOSITION_COMMENT_REPLY;

    /** Type of composition when replying to some message. */
    static final int COMPOSITION_MESSAGE_REPLY = ComposeFormFragment.COMPOSITION_MESSAGE_REPLY;

    /** String extra with account name from compose dialog. */
    private static final String EXTRA_COMPOSE_ACCOUNT_NAME = "accountName";

    /** String extra with destination from compose dialog. */
    private static final String EXTRA_COMPOSE_DESTINATION = "destination";

    /** String extra with title from compose dialog. */
    private static final String EXTRA_COMPOSE_TITLE = "title";

    /** String extra with text from compose dialog. */
    private static final String EXTRA_COMPOSE_TEXT = "text";

    /** Boolean extra indicating whether the text is a link. */
    private static final String EXTRA_COMPOSE_IS_LINK = "isLink";

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
            case COMPOSITION_MESSAGE:
                return getString(R.string.label_new_message);

            case COMPOSITION_COMMENT_REPLY:
            case COMPOSITION_MESSAGE_REPLY:
                return getString(R.string.label_reply,
                        getIntent().getStringExtra(EXTRA_COMPOSE_DESTINATION));

            default:
                return getString(R.string.label_new_post);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        // Fragments will be restored on config changes.
        if (savedInstanceState != null) {
            return;
        }

        int composition = getIntent().getIntExtra(EXTRA_COMPOSITION, -1);
        String destination = getIntent().getStringExtra(EXTRA_COMPOSE_DESTINATION);
        String title = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        Fragment frag = ComposeFormFragment.newInstance(composition, destination, title, text);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.compose_form_container, frag);
        ft.commit();
    }

    public void onComposeForm(String accountName, String destination, String title, String text,
            boolean isLink) {
        switch (getComposition()) {
            case COMPOSITION_COMMENT_REPLY:
                handleCommentReply(accountName, text);
                return;

            case COMPOSITION_MESSAGE_REPLY:
                handleMessageReply(accountName, text);
                return;

            default:
                // Bundle up extras from the form to pass through the captcha
                // fragment and then back to callbacks in this activity.
                Bundle extras = new Bundle(4);
                extras.putString(EXTRA_COMPOSE_ACCOUNT_NAME, accountName);
                extras.putString(EXTRA_COMPOSE_DESTINATION, destination);
                extras.putString(EXTRA_COMPOSE_TITLE, title);
                extras.putString(EXTRA_COMPOSE_TEXT, text);
                extras.putBoolean(EXTRA_COMPOSE_IS_LINK, isLink);
                CaptchaFragment.newInstance(extras).show(getFragmentManager(), CaptchaFragment.TAG);
        }
    }

    public void onComposeFormCancelled() {
        finish();
    }

    private void handleCommentReply(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        long parentId = extras.getLong(CommentListFragment.EXTRA_PARENT_ID);
        int parentNumComments = extras.getInt(CommentListFragment.EXTRA_PARENT_NUM_COMMENTS);
        String parentThingId = extras.getString(CommentListFragment.EXTRA_PARENT_THING_ID);
        String replyThingId = extras.getString(CommentListFragment.EXTRA_REPLY_THING_ID);
        int nesting = extras.getInt(CommentListFragment.EXTRA_NESTING);
        int sequence = extras.getInt(CommentListFragment.EXTRA_SEQUENCE);
        long sessionId = extras.getLong(CommentListFragment.EXTRA_SESSION_ID, -1);
        Provider.insertCommentAsync(this, parentId, parentNumComments, parentThingId, replyThingId,
                accountName, body, nesting, sequence, sessionId);
        finish();
    }

    private void handleMessageReply(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        String parentThingId = extras.getString(MessageThreadListFragment.EXTRA_PARENT_THING_ID);
        long sessionId = extras.getLong(MessageThreadListFragment.EXTRA_SESSION_ID);
        String thingId = extras.getString(MessageThreadListFragment.EXTRA_THING_ID);
        Provider.insertMessageReplyAsync(this, accountName, body, parentThingId, sessionId, thingId);
        finish();
    }

    public void onCaptchaGuess(String id, String guess, Bundle extras) {
        String accountName = extras.getString(EXTRA_COMPOSE_ACCOUNT_NAME);
        String destination = extras.getString(EXTRA_COMPOSE_DESTINATION);
        String title = extras.getString(EXTRA_COMPOSE_TITLE);
        String text = extras.getString(EXTRA_COMPOSE_TEXT);
        boolean isLink = extras.getBoolean(EXTRA_COMPOSE_IS_LINK);

        switch (getComposition()) {
            case COMPOSITION_SUBMISSION:
                Fragment frag = SubmitLinkFragment.newInstance(accountName, destination, title,
                        text, isLink, id, guess);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(frag, SubmitLinkFragment.TAG);
                ft.commit();
                break;

            case COMPOSITION_MESSAGE:
                frag = ComposeFragment.newInstance(accountName, destination, title, text, id, guess);
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

    public void onSubmitLink(String name, String url) {
        Toast.makeText(getApplicationContext(), url, Toast.LENGTH_SHORT).show();
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

    private int getComposition() {
        return getIntent().getIntExtra(EXTRA_COMPOSITION, -1);
    }
}
