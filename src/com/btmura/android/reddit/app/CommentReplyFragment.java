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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.widget.AccountNameAdapter;

/**
 * {@link DialogFragment} that displays a text box for the user to fill in to
 * respond to a thing.
 */
public class CommentReplyFragment extends DialogFragment implements LoaderCallbacks<AccountResult>,
        OnClickListener {

    // This fragment only reports back the user's input and doesn't handle
    // modifying the database. The caller of this fragment should handle that.

    public static final String TAG = "CommentReplyFragment";

    /** String extra with thing id you are replying to. */
    public static final String ARG_THING_ID = "thingId";

    /** String extra specifying author of the thing you are replying to. */
    public static final String ARG_AUTHOR = "author";

    /** Bundle that will be passed back via listener callbacks. */
    public static final String ARG_EXTRAS = "extras";

    /**
     * Listener fired when the user presses the OK button and submits a
     * non-empty comment.
     */
    interface OnCommentReplyListener {
        /**
         * @param accountName of the account selected
         * @param thingId of the thing you are replying to
         * @param text of your reply
         * @param extras passed to the fragment
         */
        void onCommentReply(String accountName, String thingId, String text, Bundle extras);
    }

    private OnCommentReplyListener listener;
    private AccountNameAdapter adapter;
    private boolean restoringState;
    private Spinner accountSpinner;
    private EditText bodyText;
    private View ok;

    /**
     * @param thingId of the thing the user is replying to
     * @param author of the thing the user is replying to
     */
    public static CommentReplyFragment newInstance(String thingId, String author, Bundle extras) {
        Bundle args = new Bundle(3);
        args.putString(ARG_THING_ID, thingId);
        args.putString(ARG_AUTHOR, author);
        args.putBundle(ARG_EXTRAS, extras);
        CommentReplyFragment frag = new CommentReplyFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getTargetFragment() instanceof OnCommentReplyListener) {
            this.listener = (OnCommentReplyListener) getTargetFragment();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AccountNameAdapter(getActivity());
        restoringState = savedInstanceState != null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        String author = getArguments().getString(ARG_AUTHOR);
        getDialog().setTitle(getString(R.string.comment_reply_title, author));

        View v = inflater.inflate(R.layout.comment_reply, container, false);
        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(adapter);

        bodyText = (EditText) v.findViewById(R.id.body_text);
        ok = v.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        v.findViewById(R.id.cancel).setOnClickListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity(), false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        adapter.setAccountNames(result.accountNames);
        if (!restoringState) {
            accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
        dismiss();
    }

    public void onClick(View view) {
        if (view == ok) {
            if (TextUtils.isEmpty(bodyText.getText())) {
                bodyText.setError(getString(R.string.error_blank_field));
                return;
            }
            if (listener != null) {
                String accountName = adapter.getItem(accountSpinner.getSelectedItemPosition());
                String thingId = getArguments().getString(ARG_THING_ID);
                String body = bodyText.getText().toString();
                Bundle extras = getArguments().getBundle(ARG_EXTRAS);
                listener.onCommentReply(accountName, thingId, body, extras);
            }
        }
        dismiss();
    }
}
