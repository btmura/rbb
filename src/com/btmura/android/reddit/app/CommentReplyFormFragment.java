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
    interface OnCommentReplyFormListener {
        /**
         * @param accountName of the account selected
         * @param thingId of the thing you are replying to
         * @param text of your reply
         * @param extras passed to the fragment
         */
        void onCommentReply(String accountName, String thingId, String text, Bundle extras);

        void onCommentReplyCancelled();
    }

    private OnCommentReplyFormListener listener;
    private AccountNameAdapter adapter;
    private boolean restoringState;
    private Spinner accountSpinner;
    private EditText bodyText;
    private View ok;
    private View cancel;

    /**
     * @param thingId of the thing the user is replying to
     * @param author of the thing the user is replying to
     */
    public static CommentReplyFormFragment newInstance(String thingId, String author,
            Bundle extras) {
        Bundle args = new Bundle(3);
        args.putString(ARG_THING_ID, thingId);
        args.putString(ARG_AUTHOR, author);
        args.putBundle(ARG_EXTRAS, extras);
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
        restoringState = savedInstanceState != null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.comment_reply_form, container, false);
        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(adapter);

        bodyText = (EditText) v.findViewById(R.id.body_text);

        if (getActivity().getActionBar() == null) {
            ViewStub vs = (ViewStub) v.findViewById(R.id.button_bar_stub);
            View buttonBar = vs.inflate();
            ok = buttonBar.findViewById(R.id.ok);
            ok.setOnClickListener(this);
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
        adapter.setAccountNames(result.accountNames);
        if (!restoringState) {
            accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
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
            String accountName = adapter.getItem(accountSpinner.getSelectedItemPosition());
            String thingId = getArguments().getString(ARG_THING_ID);
            String body = bodyText.getText().toString();
            Bundle extras = getArguments().getBundle(ARG_EXTRAS);
            listener.onCommentReply(accountName, thingId, body, extras);
        }
        return true;
    }
}
