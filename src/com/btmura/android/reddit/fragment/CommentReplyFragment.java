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

package com.btmura.android.reddit.fragment;

import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.provider.CommentProvider;

public class CommentReplyFragment extends DialogFragment implements OnClickListener {

    public static final String TAG = "CommentReplyFragment";

    public static final String ARG_ACCOUNT_NAME = "accountName";
    public static final String ARG_COMMENT_BUNDLE = "commentBundle";

    private String accountName;
    private Bundle commentBundle;
    private EditText bodyText;
    private View cancel;
    private View ok;

    public static CommentReplyFragment newInstance(String accountName, Bundle commentBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putBundle(ARG_COMMENT_BUNDLE, commentBundle);
        CommentReplyFragment frag = new CommentReplyFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountName = getArguments().getString(ARG_ACCOUNT_NAME);
        commentBundle = getArguments().getBundle(ARG_COMMENT_BUNDLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String author = Comments.getAuthor(commentBundle);
        getDialog().setTitle(getString(R.string.comment_reply_title, author));

        View v = inflater.inflate(R.layout.comment_reply, container, false);
        bodyText = (EditText) v.findViewById(R.id.body_text);
        cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        ok = v.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        return v;
    }

    public void onClick(View view) {
        if (view == ok) {
            dismiss();
            AsyncTask.execute(new Runnable() {
                public void run() {
                    int sequence = Comments.getSequence(commentBundle);
                    int nesting = Comments.getNesting(commentBundle);
                    if (sequence != 0) {
                        nesting++;
                    }

                    ContentValues v = new ContentValues(8);
                    v.put(Comments.COLUMN_ACCOUNT, accountName);
                    v.put(Comments.COLUMN_AUTHOR, accountName);
                    v.put(Comments.COLUMN_BODY, bodyText.getText().toString());
                    v.put(Comments.COLUMN_CREATED_UTC, System.currentTimeMillis() / 1000);
                    v.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
                    v.put(Comments.COLUMN_NESTING, nesting);
                    v.put(Comments.COLUMN_SEQUENCE, sequence);
                    v.put(Comments.COLUMN_SESSION_ID, Comments.getSessionId(commentBundle));

                    ContentResolver cr = getActivity().getContentResolver();
                    cr.insert(CommentProvider.CONTENT_URI, v);
                }
            });
        } else if (view == cancel) {
            dismiss();
        }
    }
}
