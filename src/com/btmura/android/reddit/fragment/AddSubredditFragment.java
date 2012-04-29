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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

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
        final EditText subredditName = (EditText) v.findViewById(R.id.subreddit_name);        
        final CheckBox addFrontPage = (CheckBox) v.findViewById(R.id.add_front_page);
        
        addFrontPage.setOnCheckedChangeListener(new OnCheckedChangeListener() {           
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                subredditName.setEnabled(!isChecked);
            }
        });
                
        String name = nameHolder.getSubredditName();
        int length = name != null ? name.length() : 0;
        subredditName.setText(name);
        subredditName.setSelection(length, length);
        subredditName.setFilters(INPUT_FILTERS);
                
        return new AlertDialog.Builder(getActivity()).setView(v)
                .setTitle(R.string.add_subreddit)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.button_add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name;
                        if (addFrontPage.isChecked()) {
                            name = "";
                        } else {
                            name = subredditName.getText().toString();
                        }
                        
                        ContentValues values = new ContentValues(1);                        
                        values.put(Subreddits.COLUMN_NAME, name);
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
