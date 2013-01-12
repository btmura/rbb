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

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.Result;

/**
 * A head-less {@link Fragment} that composes submissions and messages.
 */
public class ComposeFragment extends Fragment {

    public static final String TAG = "ComposeFragment";

    private static final String ARG_TYPE = "type";
    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_DESTINATION = "destination";
    private static final String ARG_TITLE = "title";
    private static final String ARG_TEXT = "text";
    private static final String ARG_IS_LINK = "isLink";
    private static final String ARG_CAPTCHA_ID = "captchaId";
    private static final String ARG_CAPTCHA_GUESS = "captchaGuess";

    public interface OnComposeListener {
        void onComposeSuccess(int type, String name, String url);

        void onComposeCancelled();
    }

    private OnComposeListener listener;
    private ComposeTask task;

    public static ComposeFragment newInstance(int type, String accountName, String destination,
            String title, String text, boolean isLink, String captchaId, String captchaGuess) {
        Bundle args = new Bundle(9);
        args.putInt(ARG_TYPE, type);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_DESTINATION, destination);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        args.putBoolean(ARG_IS_LINK, isLink);
        args.putString(ARG_CAPTCHA_ID, captchaId);
        args.putString(ARG_CAPTCHA_GUESS, captchaGuess);

        ComposeFragment frag = new ComposeFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnComposeListener) {
            listener = (OnComposeListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain instance to avoid cancelling ongoing RPC.
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (task == null) {
            int type = getArguments().getInt(ARG_TYPE);
            String accountName = getArguments().getString(ARG_ACCOUNT_NAME);
            String destination = getArguments().getString(ARG_DESTINATION);
            String title = getArguments().getString(ARG_TITLE);
            String text = getArguments().getString(ARG_TEXT);
            boolean isLink = getArguments().getBoolean(ARG_IS_LINK);
            String captchaId = getArguments().getString(ARG_CAPTCHA_ID);
            String captchaGuess = getArguments().getString(ARG_CAPTCHA_GUESS);
            task = new ComposeTask(getActivity(), type, accountName, destination, title, text, isLink,
                    captchaId, captchaGuess);
            task.execute();
        }
    }

    @Override
    public void onDestroy() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        super.onDestroy();
    }

    class ComposeTask extends AsyncTask<Void, Void, Result> {

        private final Context context;
        private final int type;
        private final String accountName;
        private final String destination;
        private final String title;
        private final String text;
        private final boolean isLink;
        private final String captchaId;
        private final String captchaGuess;

        ComposeTask(Context context, int type, String accountName, String destination,
                String title, String text, boolean isLink, String captchaId, String captchaGuess) {
            this.context = context.getApplicationContext();
            this.type = type;
            this.accountName = accountName;
            this.destination = destination;
            this.title = title;
            this.text = text;
            this.isLink = isLink;
            this.captchaId = captchaId;
            this.captchaGuess = captchaGuess;
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getFragmentManager(),
                    getString(R.string.submit_link_submitting));
        }

        @Override
        protected Result doInBackground(Void... voidRay) {
            try {
                AccountManager manager = AccountManager.get(context);
                Account account = AccountUtils.getAccount(context, accountName);

                // Get account cookie or bail out.
                String cookie = AccountUtils.getCookie(manager, account);
                if (cookie == null) {
                    return null;
                }

                // Get account modhash or bail out.
                String modhash = AccountUtils.getModhash(manager, account);
                if (modhash == null) {
                    return null;
                }

                switch (type) {
                    case ComposeActivity.TYPE_POST:
                        return RedditApi.submit(destination, title, text, isLink,
                                captchaId, captchaGuess, cookie, modhash);

                    case ComposeActivity.TYPE_MESSAGE:
                        return RedditApi.compose(destination, title, text,
                                captchaId, captchaGuess, cookie, modhash);

                    default:
                        throw new IllegalArgumentException();
                }

            } catch (OperationCanceledException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (AuthenticatorException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Result result) {
            ProgressDialogFragment.dismissDialog(getFragmentManager());
            if (result == null) {
                MessageDialogFragment.showMessage(getFragmentManager(),
                        getString(R.string.error));
            } else if (result.errors != null) {
                MessageDialogFragment.showMessage(getFragmentManager(),
                        result.getErrorMessage(context));
            } else if (listener != null) {
                listener.onComposeSuccess(type, result.name, result.url);
            }
        }
    }
}
