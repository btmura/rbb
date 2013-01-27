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

    // TODO: Rename this to "SubmitFragment"

    public static final String TAG = "ComposeFragment";

    private static final String ARG_TYPE = "type";
    private static final String ARG_EXTRAS = "extras";
    private static final String ARG_CAPTCHA_ID = "captchaId";
    private static final String ARG_CAPTCHA_GUESS = "captchaGuess";

    private static final String EXTRA_ACCOUNT_NAME = "accountName";
    private static final String EXTRA_DESTINATION = "destination";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_IS_LINK = "isLink";

    public interface OnComposeListener {
        void onComposeSuccess(int type, String name, String url);

        void onComposeCaptchaFailure(String captchaId, Bundle extras);

        void onComposeCancelled();
    }

    private OnComposeListener listener;
    private ComposeTask task;

    public static Bundle newExtras(String accountName, String destination, String title,
            String text, boolean isLink) {
        Bundle extras = new Bundle(6);
        extras.putString(EXTRA_ACCOUNT_NAME, accountName);
        extras.putString(EXTRA_DESTINATION, destination);
        extras.putString(EXTRA_TITLE, title);
        extras.putString(EXTRA_TEXT, text);
        extras.putBoolean(EXTRA_IS_LINK, isLink);
        return extras;
    }

    public static ComposeFragment newInstance(int type, Bundle extras, String captchaId,
            String captchaGuess) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_TYPE, type);
        args.putBundle(ARG_EXTRAS, extras);
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
            Bundle extras = getArguments().getBundle(ARG_EXTRAS);
            String captchaId = getArguments().getString(ARG_CAPTCHA_ID);
            String captchaGuess = getArguments().getString(ARG_CAPTCHA_GUESS);
            task = new ComposeTask(getActivity(), type, extras, captchaId, captchaGuess);
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
        private final Bundle extras;
        private final String captchaId;
        private final String captchaGuess;

        ComposeTask(Context context, int type, Bundle extras, String captchaId,
                String captchaGuess) {
            this.context = context.getApplicationContext();
            this.type = type;
            this.extras = extras;
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
                String accountName = extras.getString(EXTRA_ACCOUNT_NAME);
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

                String destination = extras.getString(EXTRA_DESTINATION);
                String title = extras.getString(EXTRA_TITLE);
                String text = extras.getString(EXTRA_TEXT);
                boolean isLink = extras.getBoolean(EXTRA_IS_LINK);

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
                showMessage(getString(R.string.error));
            } else if (result.hasRateLimitError()) {
                showMessage(result.getErrorMessage(context));
            } else if (result.hasBadCaptchaError()) {
                if (listener != null) {
                    listener.onComposeCaptchaFailure(result.captcha, extras);
                }
            } else if (result.hasErrors()) {
                showMessage(result.getErrorMessage(context));
            } else {
                if (listener != null) {
                    listener.onComposeSuccess(type, result.name, result.url);
                }
            }
        }

        private void showMessage(CharSequence error) {
            MessageDialogFragment.showMessage(getFragmentManager(), error);
        }
    }
}
