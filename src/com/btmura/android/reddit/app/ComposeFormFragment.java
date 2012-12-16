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

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.widget.AccountNameAdapter;

/**
 * {@link Fragment} that displays a form for composing submissions and messages.
 */
public class ComposeFormFragment extends Fragment implements LoaderCallbacks<AccountResult> {

    // This fragment only reports back the user's input and doesn't handle
    // modifying the database. The caller of this fragment should handle that.

    /** Integer argument indicating the type of composition. */
    public static final String ARG_COMPOSITION = "composition";

    /** Type of composition when submitting a link or text. */
    public static final int COMPOSITION_SUBMISSION = 0;

    /** Type of composition when replying to some comment. */
    public static final int COMPOSITION_COMMENT = 1;

    /** Type of composition when crafting a message. */
    public static final int COMPOSITION_MESSAGE = 2;

    /** Flag for initially setting account spinner once. */
    private boolean restoringState;

    /** Adapter of account names to select who will be composing. */
    private AccountNameAdapter adapter;

    /** {@link Spinner} containing all the acounts who can compose. */
    private Spinner accountSpinner;

    /** {@link EditText} for either subreddit or username. */
    private EditText destinationText;

    /** {@link EditText} for either title or subject. */
    private EditText titleText;

    public static ComposeFormFragment newInstance(int composition) {
        Bundle args = new Bundle(1);
        args.putInt(ARG_COMPOSITION, composition);
        ComposeFormFragment frag = new ComposeFormFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        restoringState = savedInstanceState != null;

        // Fill the adapter with loading to avoid jank.
        adapter = new AccountNameAdapter(getActivity());
        adapter.add(getString(R.string.loading));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.compose_form, container, false);

        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setEnabled(false);
        accountSpinner.setAdapter(adapter);

        destinationText = (EditText) v.findViewById(R.id.destination_text);
        titleText = (EditText) v.findViewById(R.id.title_text);

        switch (getArguments().getInt(ARG_COMPOSITION)) {
            case COMPOSITION_SUBMISSION:
                destinationText.setHint(R.string.hint_subreddit);
                titleText.setHint(R.string.hint_title);
                break;

            case COMPOSITION_COMMENT:
                destinationText.setVisibility(View.GONE);
                titleText.setVisibility(View.GONE);
                break;

            case COMPOSITION_MESSAGE:
                destinationText.setHint(R.string.hint_username);
                titleText.setHint(R.string.hint_subject);
                break;
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        // Create loader that doesn't show the app storage account.
        return new AccountLoader(getActivity(), false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountSpinner.setEnabled(true);
        adapter.clear();
        adapter.addAll(result.accountNames);

        // Only setup spinner when not changing configs. Widget will handle
        // selecting the last account on config changes on its own.
        if (!restoringState) {
            accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
        }

        getActivity().invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
    }
}
