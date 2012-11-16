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
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.CommentReplyFormFragment.OnCommentReplyFormListener;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.provider.CommentProvider;

public class CommentReplyActivity extends Activity implements OnCommentReplyFormListener {

    public static final String TAG = "CommentReplyActivity";

    /** String extra with the thing ID. */
    public static final String EXTRA_THING_ID = "thingId";

    /** String extra with the author. */
    public static final String EXTRA_AUTHOR = "author";

    /** Bundle extra with pass-through arguments. */
    public static final String EXTRA_PASS_THROUGH = "passThrough";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comment_reply);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        String author = getIntent().getStringExtra(EXTRA_AUTHOR);
        setTitle(getString(R.string.comment_reply_title, author));

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            String thingId = getIntent().getStringExtra(EXTRA_THING_ID);
            String author = getIntent().getStringExtra(EXTRA_AUTHOR);
            Bundle passThrough = getIntent().getBundleExtra(EXTRA_PASS_THROUGH);

            Fragment frag = CommentReplyFormFragment.newInstance(thingId, author, passThrough);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.comment_reply_container, frag);
            ft.commit();
        }
    }

    public void onCommentReply(String accountName, String thingId, String text, Bundle extras) {
        int nesting = extras.getInt(Comments.COLUMN_NESTING);
        int sequence = extras.getInt(Comments.COLUMN_SEQUENCE);
        String sessionId = extras.getString(Comments.COLUMN_SESSION_ID);
        long sessionCreationTime = extras.getLong(Comments.COLUMN_SESSION_TIMESTAMP);
        CommentProvider.insertInBackground(this, accountName, text, nesting,
                thingId, sequence, sessionId, sessionCreationTime, thingId);
        finish();
    }

    public void onCommentReplyCancelled() {
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
}
