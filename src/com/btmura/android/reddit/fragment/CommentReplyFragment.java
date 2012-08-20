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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.provider.ReplyProvider;

public class CommentReplyFragment extends DialogFragment implements OnClickListener {

    public static final String TAG = "CommentReplyFragment";

    public static final String ARG_ACCOUNT_NAME = "accountName";
    public static final String ARG_PARENT_THING_ID = "parentThingId";
    public static final String ARG_THING_ID = "thingId";
    public static final String ARG_AUTHOR = "author";

    private String accountName;
    private String parentThingId;
    private String thingId;
    private String author;
    private EditText bodyText;
    private View cancel;
    private View ok;

    public static CommentReplyFragment newInstance(String accountName, String parentThingId,
            String thingId, String author) {
        Bundle args = new Bundle(4);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_PARENT_THING_ID, parentThingId);
        args.putString(ARG_THING_ID, thingId);
        args.putString(ARG_AUTHOR, author);
        CommentReplyFragment frag = new CommentReplyFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountName = getArguments().getString(ARG_ACCOUNT_NAME);
        parentThingId = getArguments().getString(ARG_PARENT_THING_ID);
        thingId = getArguments().getString(ARG_THING_ID);
        author = getArguments().getString(ARG_AUTHOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
            ReplyProvider.replyInBackground(getActivity(), accountName, parentThingId, thingId,
                    bodyText.getText().toString());
            dismiss();
        } else if (view == cancel) {
            dismiss();
        }

    }
}
