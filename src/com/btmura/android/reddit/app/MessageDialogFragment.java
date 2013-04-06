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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public class MessageDialogFragment extends DialogFragment {

    public static final String TAG = "MessageDialogFragment";

    private static final String ARG_MESSAGE = "m";

    public static final MessageDialogFragment showMessage(FragmentManager fm, CharSequence message) {
        Bundle args = new Bundle(1);
        args.putCharSequence(ARG_MESSAGE, message);
        MessageDialogFragment frag = new MessageDialogFragment();
        frag.setArguments(args);
        frag.show(fm, TAG);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(getArguments().getCharSequence(ARG_MESSAGE))
                .create();
    }
}
