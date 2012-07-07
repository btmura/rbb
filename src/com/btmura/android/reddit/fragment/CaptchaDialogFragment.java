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
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.CaptchaLoader;
import com.btmura.android.reddit.text.InputFilters;

public class CaptchaDialogFragment extends DialogFragment implements LoaderCallbacks<Bitmap>,
        OnClickListener {

    public static final String TAG = "CaptchaDialogFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
            InputFilters.NO_SPACES_FILTER,
    };

    private static final String ARG_CAPTCHA_ID = "ci";

    public interface OnCaptchaGuessListener {
        void onCaptchaGuess(String id, String guess);

        void onCaptchaCancelled();
    }

    private OnCaptchaGuessListener listener;
    private ImageView captcha;
    private EditText guess;
    private Button cancel;
    private Button ok;

    public static CaptchaDialogFragment newInstance(String captchaId) {
        Bundle args = new Bundle(1);
        args.putString(ARG_CAPTCHA_ID, captchaId);
        CaptchaDialogFragment f = new CaptchaDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_MinWidth);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnCaptchaGuessListener) {
            listener = (OnCaptchaGuessListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.captcha, container, false);
        captcha = (ImageView) v.findViewById(R.id.captcha);

        guess = (EditText) v.findViewById(R.id.guess);
        guess.setFilters(INPUT_FILTERS);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        ok = (Button) v.findViewById(R.id.ok);
        ok.setOnClickListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
        return new CaptchaLoader(getActivity(), getArguments().getString(ARG_CAPTCHA_ID));
    }

    public void onLoadFinished(Loader<Bitmap> loader, Bitmap result) {
        captcha.setImageBitmap(result);
    }

    public void onLoaderReset(Loader<Bitmap> loader) {
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
        dismiss();
        if (listener != null) {
            listener.onCaptchaCancelled();
        }
    }

    private void handleOk() {
        if (guess.getText().length() <= 0) {
            guess.setError(getString(R.string.error_blank_field));
            return;
        }
        dismiss();
        if (listener != null) {
            listener.onCaptchaGuess(getCaptchaId(), guess.getText().toString());
        }
    }

    private String getCaptchaId() {
        return getArguments().getString(ARG_CAPTCHA_ID);
    }
}
