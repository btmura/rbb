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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.CaptchaFragment.OnCaptchaGuessListener;
import com.btmura.android.reddit.app.ComposeFormFragment.OnComposeFormListener;
import com.btmura.android.reddit.app.ComposeFragment.OnComposeListener;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.provider.Provider;

public class ComposeActivity extends FragmentActivity implements OnPageChangeListener,
        OnComposeFormListener,
        OnCaptchaGuessListener,
        OnComposeListener {

    /** Type of composition when submitting a link or text. */
    public static final int TYPE_POST = 0;

    /** Type of composition when crafting a new message. */
    public static final int TYPE_MESSAGE = 1;

    /** Type when replying to some comment. */
    public static final int TYPE_COMMENT_REPLY = 2;

    /** Type of composition when replying to some message. */
    public static final int TYPE_MESSAGE_REPLY = 3;

    /** Type to use when editing a self post. */
    public static final int TYPE_EDIT_POST = 4;

    /** Type to use when editing a comment. */
    public static final int TYPE_EDIT_COMMENT = 5;

    /** Default set of types supported when sharing something to the app. */
    public static final int[] DEFAULT_TYPE_SET = {
            TYPE_POST,
            TYPE_MESSAGE,
    };

    /** Set of types when sending a message to somebody. */
    public static final int[] MESSAGE_TYPE_SET = {
            TYPE_MESSAGE,
    };

    /** Set of types when replying to some comment. */
    public static final int[] COMMENT_REPLY_TYPE_SET = {
            TYPE_COMMENT_REPLY,
            TYPE_MESSAGE,
    };

    /** Set of types when replying in a message thread. */
    public static final int[] MESSAGE_REPLY_TYPE_SET = {
            TYPE_MESSAGE_REPLY,
            TYPE_MESSAGE,
    };

    /** Set of types when editing a self post. */
    public static final int[] EDIT_POST_TYPE_SET = {
            TYPE_EDIT_POST,
    };

    /** Set of types when editing a comment. */
    public static final int[] EDIT_COMMENT_TYPE_SET = {
            TYPE_EDIT_COMMENT,
    };

    /** Array of ints specifying what types to show we can compose. */
    public static final String EXTRA_TYPES = "types";

    /** Optional string extra to specify the subreddit of a post. */
    public static final String EXTRA_SUBREDDIT_DESTINATION = "subredditDestination";

    /** Optional string extra to specify the destination of a message. */
    public static final String EXTRA_MESSAGE_DESTINATION = "messageDestination";

    /** Optional string extra to specify the title of a post or message. */
    public static final String EXTRA_TITLE = Intent.EXTRA_SUBJECT;

    /** Optional string extra to specify the text of a post or message. */
    public static final String EXTRA_TEXT = Intent.EXTRA_TEXT;

    /** Optional boolean indicating whether this is a reply to something. */
    public static final String EXTRA_IS_REPLY = "isReply";

    /** Bundle of extras to pass through. */
    public static final String EXTRA_EXTRAS = "extras";

    // The following extras should be passed for COMMENT_REPLY.

    public static final String EXTRA_COMMENT_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_COMMENT_THING_ID = "thingId";

    // The following extras should be passed for MESSAGE_REPLY.

    public static final String EXTRA_MESSAGE_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_MESSAGE_THING_ID = "thingId";

    // The following extras should be passed for EDIT.

    public static final String EXTRA_EDIT_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_EDIT_SESSION_ID = "sessionId";
    public static final String EXTRA_EDIT_THING_ID = "thingId";

    /** ViewPager that holds the pages to compose different things. */
    private ViewPager pager;

    /** Adapter of the ViewPager used to swipe between screens. */
    private ComposePagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.compose);
        setupViews();
    }

    private void setupViews() {
        // No action bar will be available on large devices.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        int[] types = getIntent().getIntArrayExtra(EXTRA_TYPES);
        if (types == null) {
            types = DEFAULT_TYPE_SET;
        }

        adapter = new ComposePagerAdapter(this, getSupportFragmentManager(), types,
                getIntent().getStringExtra(EXTRA_SUBREDDIT_DESTINATION),
                getIntent().getStringExtra(EXTRA_MESSAGE_DESTINATION),
                getIntent().getStringExtra(EXTRA_TITLE),
                getIntent().getStringExtra(EXTRA_TEXT),
                getIntent().getBooleanExtra(EXTRA_IS_REPLY, false));

        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(this);
        onPageSelected(0);

        PagerTabStrip pagerStrip = (PagerTabStrip) findViewById(R.id.pager_strip);
        pagerStrip.setTabIndicatorColorResource(android.R.color.holo_blue_light);
        pagerStrip.setVisibility(types.length > 1 ? View.VISIBLE : View.GONE);
    }

    public void onPageSelected(int position) {
        setTitle(getTitle(position));
    }

    private String getTitle(int position) {
        switch (adapter.getType(position)) {
            case ComposeActivity.TYPE_POST:
                return getString(R.string.compose_title_post);

            case ComposeActivity.TYPE_MESSAGE:
                return getString(R.string.compose_title_message);

            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_MESSAGE_REPLY:
                return getString(R.string.compose_title_reply,
                        getIntent().getStringExtra(EXTRA_MESSAGE_DESTINATION));

            case ComposeActivity.TYPE_EDIT_POST:
                return getString(R.string.compose_title_edit_post);

            case ComposeActivity.TYPE_EDIT_COMMENT:
                return getString(R.string.compose_title_edit_comment);

            default:
                throw new IllegalArgumentException();
        }
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    // TODO: Use bundle for onComposeForm.
    public void onComposeForm(String accountName, String destination, String title, String text,
            boolean isLink) {
        // TODO: Don't rely on the current page.
        int type = getCurrentType();
        switch (type) {
            case TYPE_POST:
            case TYPE_MESSAGE:
                Bundle extras = ComposeFragment.newExtras(accountName, destination, title, text,
                        isLink);
                Fragment frag = ComposeFragment.newInstance(type, extras, null, null);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(frag, ComposeFragment.TAG);
                ft.commit();
                break;

            case TYPE_COMMENT_REPLY:
                handleCommentReply(accountName, text);
                break;

            case TYPE_MESSAGE_REPLY:
                handleMessageReply(accountName, text);
                break;

            case TYPE_EDIT_POST:
            case TYPE_EDIT_COMMENT:
                handleEdit(accountName, text);
                break;
        }
    }

    public void onComposeSuccess(int type, String name, String url) {
        switch (type) {
            case TYPE_POST:
                Toast.makeText(getApplicationContext(), url, Toast.LENGTH_SHORT).show();
                finish();
                break;

            case TYPE_MESSAGE:
                Toast.makeText(getApplicationContext(), R.string.compose_message_sent,
                        Toast.LENGTH_SHORT).show();
                finish();
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public void onComposeCaptchaFailure(String captchaId, Bundle extras) {
        CaptchaFragment.newInstance(captchaId, extras)
                .show(getSupportFragmentManager(), CaptchaFragment.TAG);
    }

    // TODO: Do we need these?
    public void onComposeCancelled() {
    }

    public void onCaptchaGuess(String id, String guess, Bundle extras) {
        // TODO: Don't reply on the current page.
        int type = getCurrentType();
        switch (type) {
            case TYPE_POST:
            case TYPE_MESSAGE:
                Fragment frag = ComposeFragment.newInstance(type, extras, id, guess);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(frag, ComposeFragment.TAG);
                ft.commit();
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    // TODO: Do we need these?
    public void onCaptchaCancelled() {
    }

    private void handleCommentReply(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        String parentThingId = extras.getString(EXTRA_COMMENT_PARENT_THING_ID);
        String replyThingId = extras.getString(EXTRA_COMMENT_THING_ID);
        Provider.commentReplyAsync(this, accountName, body, parentThingId, replyThingId);
        finish();
    }

    private void handleMessageReply(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        String parentThingId = extras.getString(EXTRA_MESSAGE_PARENT_THING_ID);
        String thingId = extras.getString(EXTRA_MESSAGE_THING_ID);
        Provider.messageReplyAsync(this, accountName, body, parentThingId, thingId);
        finish();
    }

    private void handleEdit(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        String parentThingId = extras.getString(EXTRA_EDIT_PARENT_THING_ID);
        // TODO: Fix this to not require session like comment and message replies.
        long sessionId = extras.getLong(EXTRA_EDIT_SESSION_ID);
        String thingId = extras.getString(EXTRA_EDIT_THING_ID);
        Provider.editAsync(this, accountName, parentThingId, thingId, body, sessionId);
        finish();
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

    private int getCurrentType() {
        return adapter.getType(pager.getCurrentItem());
    }
}
