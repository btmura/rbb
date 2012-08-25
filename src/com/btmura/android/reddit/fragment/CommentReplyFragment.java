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

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.btmura.android.reddit.R;

/**
 * {@link DialogFragment} that displays a text box for the user to fill in to
 * respond to a thing.
 */
public class CommentReplyFragment extends DialogFragment implements OnClickListener {

    // This fragment only reports back the user's input and doesn't handle
    // modifying the database. The caller of this fragment should handle that.

    public static final String TAG = "CommentReplyFragment";

    /** Thing id of the thing you are replying to. */
    public static final String ARG_THING_ID = "thingId";

    /** Author of the thing you are replying to. */
    public static final String ARG_AUTHOR = "author";

    /** Listener fired when the user presses the OK button. */
    interface OnCommentReplyListener {
        /**
         * @param thingId of the thing you are replying to
         * @param text of your reply
         */
        void onCommentReply(String thingId, String text);
    }

    private OnCommentReplyListener listener;
    private EditText bodyText;
    private View ok;

    /**
     * @param thingId of the thing the user is replying to
     * @param author of the thing the user is replying to
     */
    public static CommentReplyFragment newInstance(String thingId, String author) {
        Bundle args = new Bundle(2);
        args.putString(ARG_THING_ID, thingId);
        args.putString(ARG_AUTHOR, author);
        CommentReplyFragment frag = new CommentReplyFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnCommentReplyListener) {
            this.listener = (OnCommentReplyListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String author = getArguments().getString(ARG_AUTHOR);
        getDialog().setTitle(getString(R.string.comment_reply_title, author));

        View v = inflater.inflate(R.layout.comment_reply, container, false);
        bodyText = (EditText) v.findViewById(R.id.body_text);
        ok = v.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        v.findViewById(R.id.cancel).setOnClickListener(this);
        return v;
    }

    public void onClick(View view) {
        if (view == ok && listener != null) {
            String thingId = getArguments().getString(ARG_THING_ID);
            listener.onCommentReply(thingId, bodyText.getText().toString());
        }
        dismiss();
    }
}
