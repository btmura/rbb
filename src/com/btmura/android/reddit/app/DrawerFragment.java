/*
 * Copyright (C) 2013 Brian Muramatsu
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
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.widget.DrawerAdapter;
import com.btmura.android.reddit.widget.DrawerAdapter.Item;

public class DrawerFragment extends ListFragment implements LoaderCallbacks<AccountResult> {

    private static final int[] ATTRIBUTES = {
            android.R.attr.windowBackground,
    };

    private static final int PLACE_PROFILE = 0;
    private static final int PLACE_SAVED = 1;
    private static final int PLACE_MESSAGES = 2;

    public interface OnDrawerEventListener {

        void onDrawerAccountSelected(View view, String accountName);

        void onDrawerProfileSelected(View view);

        void onDrawerSavedSelected(View view);

        void onDrawerMessagesSelected(View view);
    }

    private OnDrawerEventListener listener;
    private DrawerAdapter adapter;
    private AccountResult accountResult;
    private String accountName;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnDrawerEventListener) {
            listener = (OnDrawerEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DrawerAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundResource(getBackgroundResource());
        return view;
    }

    // TODO(btmura): Replace this with a proper resource instead of window background.
    private int getBackgroundResource() {
        TypedArray array = getActivity().getTheme().obtainStyledAttributes(ATTRIBUTES);
        int backgroundResId = array.getResourceId(0, 0);
        array.recycle();
        return backgroundResId;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle arg1) {
        return new AccountLoader(getActivity(), true, true);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult accountResult) {
        this.accountResult = accountResult;
        this.accountName = accountResult.getLastAccount(getActivity());
        updateAdapter();

        int index = adapter.findAccount(accountName);
        selectItem(index);

        setListShown(true);
    }

    private void updateAdapter() {
        adapter.clear();
        addAccountNames(accountResult);
        if (AccountUtils.isAccount(accountName)) {
            addAccountPlaces();
        }
    }

    private void addAccountNames(AccountResult result) {
        String[] accountNames = result.accountNames;
        int[] linkKarma = result.linkKarma;
        int[] commentKarma = result.commentKarma;
        boolean[] hasMail = result.hasMail;
        if (accountNames != null) {
            int count = accountNames.length;
            for (int i = 0; i < count; i++) {
                String linkKarmaText = getKarmaCount(linkKarma, i);
                String commentKarmaText = getKarmaCount(commentKarma, i);
                int hasMailValue = hasMail != null && hasMail[i] ? 1 : 0;
                adapter.addItem(Item.newAccount(getActivity(), accountNames[i], linkKarmaText,
                        commentKarmaText, hasMailValue));
            }
        }
    }

    private String getKarmaCount(int[] karmaCounts, int index) {
        if (karmaCounts != null && karmaCounts[index] != -1) {
            return getString(R.string.karma_count, karmaCounts[index]);
        }
        return null;
    }

    private void addAccountPlaces() {
        Context context = getActivity();
        adapter.addItem(Item.newCategory(context, R.string.place_category));
        adapter.addItem(Item.newPlace(context, R.string.place_profile,
                ThemePrefs.getProfileIcon(context), PLACE_PROFILE));
        adapter.addItem(Item.newPlace(context, R.string.place_saved,
                ThemePrefs.getSavedIcon(context), PLACE_SAVED));
        adapter.addItem(Item.newPlace(context, R.string.place_messages,
                ThemePrefs.getMessagesIcon(context), PLACE_MESSAGES));
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        selectItem(position);
    }

    private void selectItem(int position) {
        Item item = adapter.getItem(position);
        switch (item.getType()) {
            case Item.TYPE_ACCOUNT_NAME:
                accountName = item.getAccountName();
                updateAdapter();
                if (listener != null) {
                    listener.onDrawerAccountSelected(getView(), item.getAccountName());
                }
                break;

            case Item.TYPE_PLACE:
                switch (item.getPlace()) {
                    case PLACE_PROFILE:
                        if (listener != null) {
                            listener.onDrawerProfileSelected(getView());
                        }
                        break;

                    case PLACE_SAVED:
                        if (listener != null) {
                            listener.onDrawerSavedSelected(getView());
                        }
                        break;

                    case PLACE_MESSAGES:
                        if (listener != null) {
                            listener.onDrawerMessagesSelected(getView());
                        }
                        break;
                }
                break;
        }
    }
}
