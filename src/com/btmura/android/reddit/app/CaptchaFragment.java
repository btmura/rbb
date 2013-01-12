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
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.CaptchaLoader;
import com.btmura.android.reddit.content.CaptchaLoader.CaptchaResult;
import com.btmura.android.reddit.text.InputFilters;

public class CaptchaFragment extends DialogFragment implements LoaderCallbacks<CaptchaResult>,
        OnClickListener {

    public static final String TAG = "CaptchaFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
            InputFilters.NO_SPACE_FILTER,
    };

    private static final String ARG_CAPTCHA_ID = "captchaId";
    private static final String ARG_EXTRAS = "extras";

    public interface OnCaptchaGuessListener {
        void onCaptchaGuess(String id, String guess, Bundle extras);

        void onCaptchaCancelled();
    }

    private OnCaptchaGuessListener listener;
    private String captchaId;
    private Bundle extras;

    private View progress;
    private ViewStub errorStub;
    private ImageView captcha;
    private EditText guess;
    private Button cancel;
    private Button ok;

    public static CaptchaFragment newInstance(String captchaId, Bundle extras) {
        Bundle args = new Bundle(2);
        args.putString(ARG_CAPTCHA_ID, captchaId);
        args.putBundle(ARG_EXTRAS, extras);

        CaptchaFragment frag = new CaptchaFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnCaptchaGuessListener) {
            listener = (OnCaptchaGuessListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captchaId = getArguments().getString(ARG_CAPTCHA_ID);
        extras = getArguments().getBundle(ARG_EXTRAS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getDialog().setTitle(R.string.captcha_title);
        View v = inflater.inflate(R.layout.captcha, container, false);

        progress = v.findViewById(R.id.progress);
        errorStub = (ViewStub) v.findViewById(R.id.error_stub);
        captcha = (ImageView) v.findViewById(R.id.captcha);

        guess = (EditText) v.findViewById(R.id.guess);
        guess.setFilters(INPUT_FILTERS);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        ok = (Button) v.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        ok.setEnabled(false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<CaptchaResult> onCreateLoader(int id, Bundle args) {
        return new CaptchaLoader(getActivity(), captchaId);
    }

    public void onLoadFinished(Loader<CaptchaResult> loader, CaptchaResult result) {
        progress.setVisibility(View.GONE);
        if (result == null) {
            if (errorStub != null) {
                errorStub.inflate();
                errorStub = null;
            }
            return;
        }
        captchaId = result.iden;
        captcha.setImageBitmap(result.captchaBitmap);
        ok.setEnabled(true);
    }

    public void onLoaderReset(Loader<CaptchaResult> loader) {
        captcha.setImageBitmap(null);
    }

    public void onClick(View v) {
        if (v == cancel) {
            handleCancel();
        } else if (v == ok) {
            handleOk();
        }
    }

    private void handleCancel() {
        if (listener != null) {
            listener.onCaptchaCancelled();
        }
        dismiss();
    }

    private void handleOk() {
        if (guess.getText().length() <= 0) {
            guess.setError(getString(R.string.error_blank_field));
            return;
        }
        // TODO: Disable widgets if captcha image wasn't retrieved.
        if (listener != null) {
            listener.onCaptchaGuess(captchaId, guess.getText().toString(), extras);
        }
        dismiss();
    }
}
