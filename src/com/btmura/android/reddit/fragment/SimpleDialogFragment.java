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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;

public class SimpleDialogFragment extends DialogFragment {
    
    public static final String TAG = "SimpleDialogFragment";

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_PROGRESS = 1;
    
    private static final String ARG_TYPE = "t";
    private static final String ARG_MESSAGE = "m";
    
    public static final SimpleDialogFragment showMessage(FragmentManager fm, String msg) {
        return show(fm, TYPE_MESSAGE, msg);
    }
    
    public static final SimpleDialogFragment showProgress(FragmentManager fm, String msg) {
        return show(fm, TYPE_PROGRESS, msg);
    }
    
    private static final SimpleDialogFragment show(FragmentManager fm, int type, String msg) {
        Bundle args = new Bundle(2);
        args.putInt(ARG_TYPE, type);
        args.putString(ARG_MESSAGE, msg);
        
        SimpleDialogFragment frag = new SimpleDialogFragment();
        frag.setArguments(args);
        frag.show(fm, TAG);
        return frag;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        switch (getType()) {
            case TYPE_MESSAGE:
                return createMessageDialog();
                
            case TYPE_PROGRESS:
                return createProgressDialog();
            
            default:
                throw new IllegalArgumentException();
        }        
    }
    
    private Dialog createMessageDialog() {
        return new AlertDialog.Builder(getActivity())
            .setMessage(getMessage())
            .setPositiveButton(android.R.string.ok, null)
            .create();
    }
    
    private Dialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getMessage());
        return dialog;
    }
    
    private int getType() {
        return getArguments().getInt(ARG_TYPE);
    }
    
    private String getMessage() {
        return getArguments().getString(ARG_MESSAGE);
    }
}
