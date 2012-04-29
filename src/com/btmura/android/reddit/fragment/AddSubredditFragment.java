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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;

public class AddSubredditFragment extends DialogFragment {

    public static final String TAG = "AddSubredditFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
        new SubredditInputFilter(),
    };

    public interface SubredditNameHolder {
        String getSubredditName();
    }

    private SubredditNameHolder nameHolder;

    public static AddSubredditFragment newInstance() {
        return new AddSubredditFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        nameHolder = (SubredditNameHolder) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.add_subreddit, null, false);

        final RadioButton customButton = (RadioButton) v.findViewById(R.id.custom_button);
        final RadioButton frontPageButton = (RadioButton) v.findViewById(R.id.front_page_button);

        final EditText customText = (EditText) v.findViewById(R.id.custom_text);
        final TextView frontPageText = (TextView) v.findViewById(R.id.front_page_text);

        customText.setText(nameHolder.getSubredditName());
        customText.setFilters(INPUT_FILTERS);
        customText.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                customButton.setChecked(true);
            }
        });

        frontPageText.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                frontPageButton.setChecked(true);
            }
        });

        customButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                frontPageButton.setChecked(!isChecked);
            }
        });

        frontPageButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                customButton.setChecked(!isChecked);
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.add_subreddit_title)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.add_subreddit_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ContentValues values = new ContentValues();
                                if (customButton.isChecked()) {
                                    values.put(Subreddits.COLUMN_NAME, customText.getText()
                                            .toString());
                                } else {
                                    values.put(Subreddits.COLUMN_NAME, "");
                                }
                                Provider.addSubredditInBackground(getActivity(), values);
                            }
                        }).create();
    }

    static class SubredditInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '+') {
                    return "";
                }
            }
            return null;
        }
    }
}
