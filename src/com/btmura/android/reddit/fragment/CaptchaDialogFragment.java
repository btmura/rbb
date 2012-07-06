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

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.provider.NetApi;

public class CaptchaDialogFragment extends DialogFragment {

    public static final String TAG = "CaptchaDialogFragment";

    private static final String ARG_CAPTCHA_ID = "ci";

    private ImageView captcha;
    private EditText guess;

    public static CaptchaDialogFragment newInstance(String captchaId) {
        Bundle args = new Bundle(1);
        args.putString(ARG_CAPTCHA_ID, captchaId);
        CaptchaDialogFragment f = new CaptchaDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.captcha, container, false);
        captcha = (ImageView) v.findViewById(R.id.captcha);
        guess = (EditText) v.findViewById(R.id.guess);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            new LoadCaptchaImageTask().execute();
        }
    }

    class LoadCaptchaImageTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            String id = getArguments().getString(ARG_CAPTCHA_ID);
            try {
                return NetApi.captcha(id);
            } catch (IOException e) {
                Log.e(TAG, "LoadCaptchaImageTask", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (result != null) {
                captcha.setImageBitmap(result);
            }
        }
    }
}
