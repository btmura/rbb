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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.btmura.android.reddit.Debug;

public class ProgressDialogFragment extends DialogFragment {

    public static final String TAG = "ProgressDialogFragment";
    public static final boolean DEBUG = Debug.DEBUG;

    public static final String ARG_MESSAGE = "m";

    public static ProgressDialogFragment showDialog(FragmentManager fm, String message) {
        Bundle args = new Bundle(1);
        args.putString(ARG_MESSAGE, message);
        ProgressDialogFragment f = new ProgressDialogFragment();
        f.setArguments(args);
        f.show(fm, TAG);
        return f;
    }

    public static void dismissDialog(FragmentManager fm) {
        Fragment f = fm.findFragmentByTag(TAG);
        if (f != null) {
            fm.beginTransaction().remove(f).commit();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog d = new ProgressDialog(getActivity());
        d.setMessage(getArguments().getString(ARG_MESSAGE));
        return d;
    }
}
