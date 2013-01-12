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

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.CaptchaFragment.OnCaptchaGuessListener;
import com.btmura.android.reddit.app.ComposeFormFragment.OnComposeFormListener;
import com.btmura.android.reddit.app.ComposeFragment.OnComposeListener;
import com.btmura.android.reddit.provider.Provider;

public class ComposeActivity extends Activity implements OnPageChangeListener,
        OnClickListener,
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

    public static final String EXTRA_COMMENT_PARENT_ID = "parentId";
    public static final String EXTRA_COMMENT_PARENT_NUM_COMMENTS = "parentNumComments";
    public static final String EXTRA_COMMENT_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_COMMENT_AUTHOR = "author";
    public static final String EXTRA_COMMENT_THING_ID = "thingId";
    public static final String EXTRA_COMMENT_NESTING = "nesting";
    public static final String EXTRA_COMMENT_SEQUENCE = "sequence";
    public static final String EXTRA_COMMENT_SESSION_ID = "sessionId";

    // The following extras should be passed for MESSAGE_REPLY.

    public static final String EXTRA_MESSAGE_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_MESSAGE_SESSION_ID = "sessionId";
    public static final String EXTRA_MESSAGE_THING_ID = "thingId";

    // Internal extras used by the activity to pass extras to the captcha
    // fragment and back without storing member data in this activity.

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

    interface OnComposeActivityListener {
        void onOkClicked(int id);
    }

    private final ArrayList<OnComposeActivityListener> listeners =
            new ArrayList<OnComposeActivityListener>(2);

    /** ViewPager that holds the pages to compose different things. */
    private ViewPager pager;

    /** Adapter of the ViewPager used to swipe between screens. */
    private ComposePagerAdapter adapter;

    /** Ok button visible when this form is a dialog. */
    private View ok;

    /** Cancel button visible when this form is a dialog. */
    private View cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        adapter = new ComposePagerAdapter(this, getFragmentManager(), types,
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

        if (getActionBar() == null) {
            ViewStub vs = (ViewStub) findViewById(R.id.button_bar_stub);
            View buttonBar = vs.inflate();
            ok = buttonBar.findViewById(R.id.ok);
            ok.setOnClickListener(this);
            cancel = buttonBar.findViewById(R.id.cancel);
            cancel.setOnClickListener(this);
        }
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

            default:
                throw new IllegalArgumentException();
        }
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    public void onClick(View v) {
        if (v == ok) {
            int size = listeners.size();
            for (int i = 0; i < size; i++) {
                listeners.get(i).onOkClicked(pager.getCurrentItem());
            }
        } else if (v == cancel) {
            finish();
        }
    }

    public void addOnComposeActivityListener(OnComposeActivityListener listener) {
        listeners.add(listener);
    }

    public void onComposeForm(String accountName, String destination, String title, String text,
            boolean isLink) {
        // TODO: Don't reply on the current page.
        switch (getCurrentType()) {
            case TYPE_POST:
            case TYPE_MESSAGE:
                // Bundle up extras from the form to pass through the captcha
                // fragment and then back to callbacks in this activity.
                Bundle extras = new Bundle(5);
                extras.putString(EXTRA_COMPOSE_ACCOUNT_NAME, accountName);
                extras.putString(EXTRA_COMPOSE_DESTINATION, destination);
                extras.putString(EXTRA_COMPOSE_TITLE, title);
                extras.putString(EXTRA_COMPOSE_TEXT, text);
                extras.putBoolean(EXTRA_COMPOSE_IS_LINK, isLink);
                CaptchaFragment.newInstance(extras).show(getFragmentManager(), CaptchaFragment.TAG);
                break;

            case TYPE_COMMENT_REPLY:
                handleCommentReply(accountName, text);
                break;

            case TYPE_MESSAGE_REPLY:
                handleMessageReply(accountName, text);
                break;
        }
    }

    public void onCaptchaGuess(String id, String guess, Bundle extras) {
        String accountName = extras.getString(EXTRA_COMPOSE_ACCOUNT_NAME);
        String destination = extras.getString(EXTRA_COMPOSE_DESTINATION);
        String title = extras.getString(EXTRA_COMPOSE_TITLE);
        String text = extras.getString(EXTRA_COMPOSE_TEXT);
        boolean isLink = extras.getBoolean(EXTRA_COMPOSE_IS_LINK);

        // TODO: Don't reply on the current page.
        int type = getCurrentType();
        switch (type) {
            case TYPE_POST:
            case TYPE_MESSAGE:
                Fragment frag = ComposeFragment.newInstance(type, accountName, destination, title,
                        text, isLink, id, guess);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
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

    // TODO: Do we need these?
    public void onComposeCancelled() {
    }

    private void handleCommentReply(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        long parentId = extras.getLong(EXTRA_COMMENT_PARENT_ID);
        int parentNumComments = extras.getInt(EXTRA_COMMENT_PARENT_NUM_COMMENTS);
        String parentThingId = extras.getString(EXTRA_COMMENT_PARENT_THING_ID);
        String replyThingId = extras.getString(EXTRA_COMMENT_THING_ID);
        int nesting = extras.getInt(EXTRA_COMMENT_NESTING);
        int sequence = extras.getInt(EXTRA_COMMENT_SEQUENCE);
        long sessionId = extras.getLong(EXTRA_COMMENT_SESSION_ID, -1);
        Provider.commentReplyAsync(this, parentId, parentNumComments, parentThingId, replyThingId,
                accountName, body, nesting, sequence, sessionId);
        finish();
    }

    private void handleMessageReply(String accountName, String body) {
        Bundle extras = getIntent().getBundleExtra(EXTRA_EXTRAS);
        String parentThingId = extras.getString(EXTRA_MESSAGE_PARENT_THING_ID);
        long sessionId = extras.getLong(EXTRA_MESSAGE_SESSION_ID);
        String thingId = extras.getString(EXTRA_MESSAGE_THING_ID);
        Provider.insertMessageReplyAsync(this, accountName, body, parentThingId, sessionId, thingId);
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
