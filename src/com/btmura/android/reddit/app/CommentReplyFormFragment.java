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
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
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
public class CommentReplyFormFragment extends Fragment implements
        LoaderCallbacks<AccountResult>, OnClickListener {

    // This fragment only reports back the user's input and doesn't handle
    // modifying the database. The caller of this fragment should handle that.

    public static final String TAG = "CommentReplyFormFragment";

    public static final String ARG_REPLY_ARGS = "replyArgs";

    /**
     * Listener fired when the user presses the OK button and submits a
     * non-empty comment.
     */
    interface OnCommentReplyFormListener {
        /**
         * @param replyArgs passed to the fragment
         * @param accountName of the account selected to make the comment
         * @param comment text to reply with
         */
        void onCommentReply(Bundle replyArgs, String accountName, String comment);

        void onCommentReplyCancelled();
    }

    private OnCommentReplyFormListener listener;
    private AccountNameAdapter adapter;
    private boolean restoringState;
    private Spinner accountSpinner;
    private EditText bodyText;
    private View ok;
    private View cancel;

    public static CommentReplyFormFragment newInstance(Bundle replyArgs) {
        Bundle args = new Bundle(1);
        args.putBundle(ARG_REPLY_ARGS, replyArgs);
        CommentReplyFormFragment frag = new CommentReplyFormFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnCommentReplyFormListener) {
            this.listener = (OnCommentReplyFormListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new AccountNameAdapter(getActivity());
        adapter.add(getString(R.string.loading));
        restoringState = savedInstanceState != null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.comment_reply_form, container, false);

        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setEnabled(false);
        accountSpinner.setAdapter(adapter);

        bodyText = (EditText) v.findViewById(R.id.body_text);

        if (getActivity().getActionBar() == null) {
            ViewStub vs = (ViewStub) v.findViewById(R.id.button_bar_stub);
            View buttonBar = vs.inflate();
            ok = buttonBar.findViewById(R.id.ok);
            ok.setOnClickListener(this);
            ok.setEnabled(false);
            cancel = buttonBar.findViewById(R.id.cancel);
            cancel.setOnClickListener(this);
        }

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
        if (ok != null) {
            ok.setEnabled(true);
        }
        accountSpinner.setEnabled(true);
        adapter.clear();
        adapter.addAll(result.accountNames);
        if (!restoringState) {
            accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
        }
        getActivity().invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.clear();
        if (listener != null) {
            listener.onCommentReplyCancelled();
        }
    }

    public void onClick(View v) {
        if (v == ok) {
            handleSubmit();
        } else if (v == cancel && listener != null) {
            listener.onCommentReplyCancelled();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.comment_reply_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_submit).setEnabled(accountSpinner.isEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_submit:
                return handleSubmit();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean handleSubmit() {
        if (TextUtils.isEmpty(bodyText.getText())) {
            bodyText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (listener != null) {
            Bundle replyArgs = getArguments().getBundle(ARG_REPLY_ARGS);
            String accountName = adapter.getItem(accountSpinner.getSelectedItemPosition());
            String body = bodyText.getText().toString();
            listener.onCommentReply(replyArgs, accountName, body);
        }
        return true;
    }
}
