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

import android.app.DialogFragment;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Accounts;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.text.InputFilters;

public class AddAccountFragment extends DialogFragment implements
        OnCheckedChangeListener,
        OnClickListener {

    public static final String TAG = "AddAccountFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
        InputFilters.LOGIN_FILTER,
    };

    private EditText login;
    private EditText password;
    private Button add;
    private Button cancel;

    public static AddAccountFragment newInstance() {
        return new AddAccountFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.add_account);
        View v = inflater.inflate(R.layout.add_account, container, false);

        login = (EditText) v.findViewById(R.id.login);
        login.setFilters(INPUT_FILTERS);

        password = (EditText) v.findViewById(R.id.password);

        CheckBox showPassword = (CheckBox) v.findViewById(R.id.show_password);
        showPassword.setOnCheckedChangeListener(this);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        add = (Button) v.findViewById(R.id.add);
        add.setOnClickListener(this);

        return v;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        TransformationMethod method = null;
        if (!isChecked) {
            method = PasswordTransformationMethod.getInstance();
        }
        password.setTransformationMethod(method);
    }

    public void onClick(View v) {
        if (v == cancel) {
            handleCancel();
        } else if (v == add) {
            handleAdd();
        }
    }

    private void handleCancel() {
        dismiss();
    }

    private void handleAdd() {
        if (login.getText().length() <= 0) {
            login.setError(getString(R.string.error_blank_field));
        }
        if (password.getText().length() <= 0) {
            password.setError(getString(R.string.error_blank_field));
        }
        if (login.getError() == null && password.getError() == null) {
            new LoginTask(login.getText().toString(), password.getText().toString()).execute();
        }
    }

    class LoginTask extends AsyncTask<Void, Void, String> {

        private final String login;
        private final String password;

        private LoginDialogFragment dialog;

        LoginTask(String login, String password) {
            this.login = login;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = LoginDialogFragment.newInstance();
            dialog.show(getFragmentManager(), LoginDialogFragment.TAG);
        }

        @Override
        protected String doInBackground(Void... params) {
            SystemClock.sleep(10000);
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            super.onPostExecute(error);
            if (error == null) {            
                dialog.dismiss();

                ContentValues values = new ContentValues(2);
                values.put(Accounts.COLUMN_LOGIN, login);
                values.put(Accounts.COLUMN_PASSWORD, password);
                Provider.addInBackground(getActivity(), Accounts.CONTENT_URI, values);
            }
        }        
    }
}
