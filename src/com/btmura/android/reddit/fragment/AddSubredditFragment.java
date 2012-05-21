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
import android.content.ContentValues;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.text.InputFilters;

public class AddSubredditFragment extends DialogFragment implements
        View.OnClickListener,
        CheckBox.OnCheckedChangeListener {

    public static final String TAG = "AddSubredditFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
        InputFilters.SUBREDDIT_NAME_FILTER,
    };

    public interface SubredditNameHolder {
        CharSequence getSubredditName();
    }

    private SubredditNameHolder nameHolder;

    private EditText nameField;
    private CheckBox addFrontPage;
    private Button cancel;
    private Button add;

    public static AddSubredditFragment newInstance() {
        return new AddSubredditFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        nameHolder = (SubredditNameHolder) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.add_subreddit);
        View v = inflater.inflate(R.layout.add_subreddit, container, false);

        CharSequence name = nameHolder.getSubredditName();
        int length = name != null ? name.length() : 0;
        nameField = (EditText) v.findViewById(R.id.subreddit_name);
        nameField.setText(name);
        nameField.setSelection(length, length);
        nameField.setFilters(INPUT_FILTERS);

        addFrontPage = (CheckBox) v.findViewById(R.id.add_front_page);
        addFrontPage.setOnCheckedChangeListener(this);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        add = (Button) v.findViewById(R.id.add);
        add.setOnClickListener(this);

        return v;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        nameField.setEnabled(!isChecked);
        if (!nameField.isEnabled()) {
            nameField.setError(null);
        }
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
        String name;
        if (addFrontPage.isChecked()) {
            name = "";
        } else {
            name = nameField.getText().toString();
        }

        if (!addFrontPage.isChecked() && TextUtils.isEmpty(name)) {
            nameField.setError(getString(R.string.error_blank_field));
            return;
        }

        ContentValues values = new ContentValues(1);
        values.put(Subreddits.COLUMN_NAME, name);
        Provider.addInBackground(getActivity(), Subreddits.CONTENT_URI, values);

        dismiss();
    }
}
