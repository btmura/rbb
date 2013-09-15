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
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.ComposeFormFragment.OnComposeFormListener;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.util.Objects;

public class ComposeActivity extends FragmentActivity implements
        TabListener,
        OnComposeFormListener {

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

    /** String extra of the account name selected when starting the composer. */
    public static final String EXTRA_ACCOUNT_NAME = "accountName";

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
    public static final String EXTRA_EDIT_THING_ID = "thingId";

    private int[] types;
    private ActionBar bar;
    private TabController tabController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.compose);
        setup(savedInstanceState);
    }

    private void setup(Bundle savedInstanceState) {
        types = getIntent().getIntArrayExtra(EXTRA_TYPES);
        if (types == null) {
            types = DEFAULT_TYPE_SET;
        }
        setupTabs(savedInstanceState);
    }

    private void setupTabs(Bundle savedInstanceState) {
        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        tabController = new TabController(bar, savedInstanceState);

        for (int i = 0; i < types.length; i++) {
            switch (types[i]) {
                case ComposeActivity.TYPE_POST:
                case ComposeActivity.TYPE_EDIT_POST:
                    addTab(R.string.compose_tab_post);
                    break;

                case ComposeActivity.TYPE_MESSAGE:
                    addTab(R.string.compose_tab_message);
                    break;

                case ComposeActivity.TYPE_COMMENT_REPLY:
                case ComposeActivity.TYPE_MESSAGE_REPLY:
                case ComposeActivity.TYPE_EDIT_COMMENT:
                    addTab(R.string.compose_tab_comment);
                    break;
            }
        }
        tabController.setupTabs();
    }

    private void addTab(int titleResId) {
        tabController.addTab(bar.newTab().setText(titleResId).setTabListener(this));
    }

    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction trans) {
        if (tabController.selectTab(tab)) {
            ComposeFormFragment frag = ComposeFormFragment.newInstance(types[tab.getPosition()],
                    getIntent().getStringExtra(EXTRA_SUBREDDIT_DESTINATION),
                    getIntent().getStringExtra(EXTRA_MESSAGE_DESTINATION),
                    getIntent().getStringExtra(EXTRA_TITLE),
                    getIntent().getStringExtra(EXTRA_TEXT),
                    getIntent().getBooleanExtra(EXTRA_IS_REPLY, false),
                    getIntent().getBundleExtra(EXTRA_EXTRAS));
            bar.setTitle(frag.getTitle(this));

            if (!Objects.fragmentEquals(frag, getComposeFormFragment())) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, frag, ComposeFormFragment.TAG);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                ft.commit();
            }
        }
    }

    @Override
    public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
    }

    @Override
    public void onComposeFinished() {
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        tabController.saveInstanceState(outState);
    }

    private ComparableFragment getComposeFormFragment() {
        return (ComparableFragment) getSupportFragmentManager()
                .findFragmentByTag(ComposeFormFragment.TAG);
    }
}
