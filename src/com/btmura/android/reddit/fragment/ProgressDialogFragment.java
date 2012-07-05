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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.btmura.android.reddit.Debug;

public class ProgressDialogFragment extends DialogFragment {

    public static final String TAG = "ProgressDialogFragment";
    public static final boolean DEBUG = Debug.DEBUG;

    public static final String ARG_MESSAGE = "m";

    public static ProgressDialogFragment newInstance(String message) {
        Bundle args = new Bundle(1);
        args.putString(ARG_MESSAGE, message);
        ProgressDialogFragment f = new ProgressDialogFragment();
        f.setArguments(args);
        return f;
    }

    private String message;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (message == null) {
            Bundle b = savedInstanceState != null ? savedInstanceState : getArguments();
            message = b.getString(ARG_MESSAGE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog d = new ProgressDialog(getActivity());
        if (message != null) {
            d.setMessage(message);
        }
        return d;
    }

    public void setMessage(int resId) {
        setMessage(getString(resId));
    }

    public void setMessage(String message) {
        this.message = message;
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog != null) {
            dialog.setMessage(message);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_MESSAGE, message);
    }
}
