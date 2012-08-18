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

import com.btmura.android.reddit.R;

public class CommentReplyFragment extends DialogFragment implements OnClickListener {

    public static final String TAG = "CommentReplyFragment";

    public static final String ARG_AUTHOR = "author";
    public static final String ARG_SEQUENCE = "sequence";
    public static final String ARG_THING_ID = "thingId";

    private String author;
    private View cancel;
    private View ok;

    public static CommentReplyFragment newInstance(String author, int sequence, String thingId) {
        Bundle args = new Bundle(3);
        args.putString(ARG_AUTHOR, author);
        args.putInt(ARG_SEQUENCE, sequence);
        args.putString(ARG_THING_ID, thingId);

        CommentReplyFragment frag = new CommentReplyFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        author = getArguments().getString(ARG_AUTHOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.comment_reply_title, author));

        View v = inflater.inflate(R.layout.comment_reply, container, false);
        cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        ok = v.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        return v;
    }

    public void onClick(View v) {
        if (v == cancel) {
            dismiss();
        } else if (v == ok) {
            dismiss();
        }
    }
}
