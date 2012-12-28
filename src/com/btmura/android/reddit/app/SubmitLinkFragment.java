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

public class SubmitLinkFragment extends Fragment {

    public static final String TAG = "SubmitLinkFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_TITLE = "title";
    private static final String ARG_TEXT = "text";
    private static final String ARG_IS_LINK = "isLink";
    private static final String ARG_CAPTCHA_ID = "captchaId";
    private static final String ARG_CAPTCHA_GUESS = "captchaGuess";

    private OnSubmitLinkListener listener;
    private SubmitTask submitTask;

    public interface OnSubmitLinkListener {
        void onSubmitLink(String name, String url);

        void onSubmitLinkCancelled();
    }

    public static SubmitLinkFragment newInstance(String accountName, String subreddit,
            String title, String text, boolean isLink, String captchaId, String captchaGuess) {
        Bundle args = new Bundle(7);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        args.putBoolean(ARG_IS_LINK, isLink);
        args.putString(ARG_CAPTCHA_ID, captchaId);
        args.putString(ARG_CAPTCHA_GUESS, captchaGuess);

        SubmitLinkFragment frag = new SubmitLinkFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSubmitLinkListener) {
            listener = (OnSubmitLinkListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (submitTask == null) {
            submitTask = new SubmitTask(getActivity());
            submitTask.execute();
        }
    }

    @Override
    public void onDestroy() {
        if (submitTask != null) {
            submitTask.cancel(true);
            submitTask = null;
        }
        super.onDestroy();
    }

    class SubmitTask extends AsyncTask<Void, Void, Result> {

        private Context context;

        SubmitTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getFragmentManager(),
                    getString(R.string.submit_link_submitting));
        }

        @Override
        protected Result doInBackground(Void... params) {
            try {
                String accountName = getArguments().getString(ARG_ACCOUNT_NAME);
                AccountManager manager = AccountManager.get(context);
                Account account = AccountUtils.getAccount(context, accountName);
                String cookie = AccountUtils.getCookie(manager, account);
                if (cookie == null) {
                    return null;
                }
                String modhash = AccountUtils.getModhash(manager, account);
                if (modhash == null) {
                    return null;
                }

                String subreddit = getArguments().getString(ARG_SUBREDDIT);
                String title = getArguments().getString(ARG_TITLE);
                String text = getArguments().getString(ARG_TEXT);
                boolean isLink = getArguments().getBoolean(ARG_IS_LINK);
                String captchaId = getArguments().getString(ARG_CAPTCHA_ID);
                String captchaGuess = getArguments().getString(ARG_CAPTCHA_GUESS);

                return RedditApi.submit(subreddit, title, text, isLink,
                        captchaId, captchaGuess, cookie, modhash);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (OperationCanceledException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (AuthenticatorException e) {
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
                listener.onSubmitLink(result.name, result.url);
            }
        }
    }
}
