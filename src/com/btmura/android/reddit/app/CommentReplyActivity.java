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
import com.btmura.android.reddit.app.CommentReplyFormFragment.OnCommentReplyFormListener;
import com.btmura.android.reddit.provider.Provider;

public class CommentReplyActivity extends Activity implements OnCommentReplyFormListener {

    public static final String TAG = "CommentReplyActivity";

    /** Bundle extra containing arguments needed to create the reply. */
    public static final String EXTRA_ARGS = "args";

    public static final String ARG_PARENT_ID = "parentId";
    public static final String ARG_PARENT_NUM_COMMENTS = "parentNumComments";
    public static final String ARG_PARENT_THING_ID = "parentThingId";
    public static final String ARG_REPLY_AUTHOR = "replyAuthor";
    public static final String ARG_REPLY_THING_ID = "replyThingId";
    public static final String ARG_NESTING = "nesting";
    public static final String ARG_SEQUENCE = "sequence";
    public static final String ARG_SESSION_ID = "sessionId";
    public static final String ARG_SESSION_TIMESTAMP = "sessionTimestamp";

    public static Bundle newArgs(long parentId,
            int parentNumComments,
            String parentThingId,
            String replyAuthor,
            String replyThingId,
            int nesting,
            int sequence,
            String sessionId,
            long sessionTimestamp) {
        Bundle args = new Bundle(9);
        args.putLong(ARG_PARENT_ID, parentId);
        args.putInt(ARG_PARENT_NUM_COMMENTS, parentNumComments);
        args.putString(ARG_PARENT_THING_ID, parentThingId);
        args.putString(ARG_REPLY_AUTHOR, replyAuthor);
        args.putString(ARG_REPLY_THING_ID, replyThingId);
        args.putInt(ARG_NESTING, nesting);
        args.putInt(ARG_SEQUENCE, sequence);
        args.putString(ARG_SESSION_ID, sessionId);
        args.putLong(ARG_SESSION_TIMESTAMP, sessionTimestamp);
        return args;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comment_reply);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        Bundle args = getIntent().getBundleExtra(EXTRA_ARGS);
        String author = args.getString(ARG_REPLY_AUTHOR);
        setTitle(getString(R.string.comment_reply_title, author));

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Bundle args = getIntent().getBundleExtra(EXTRA_ARGS);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.comment_reply_container, CommentReplyFormFragment.newInstance(args));
            ft.commit();
        }
    }

    public void onCommentReply(Bundle replyArgs, String accountName, String comment) {
        long parentId = replyArgs.getLong(ARG_PARENT_ID);
        int parentNumComments = replyArgs.getInt(ARG_PARENT_NUM_COMMENTS);
        String parentThingId = replyArgs.getString(ARG_PARENT_THING_ID);
        String replyThingId = replyArgs.getString(ARG_REPLY_THING_ID);
        int nesting = replyArgs.getInt(ARG_NESTING);
        int sequence = replyArgs.getInt(ARG_SEQUENCE);
        String sessionId = replyArgs.getString(ARG_SESSION_ID);
        long sessionTimestamp = replyArgs.getLong(ARG_SESSION_TIMESTAMP);
        Provider.insertCommentAsync(this, parentId, parentNumComments, parentThingId, replyThingId,
                accountName, comment, nesting, sequence, sessionId, sessionTimestamp);
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
