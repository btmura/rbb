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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class AccountSpinnerAdapter extends BaseAdapter {

    private final LayoutInflater inflater;

    private String[] accountNames;

    public AccountSpinnerAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setAccountNames(String[] newAccountNames) {
        accountNames = newAccountNames;
        notifyDataSetChanged();
    }

    public int getCount() {
        return accountNames != null ? accountNames.length : 0;
    }

    public String getItem(int position) {
        return accountNames[position];
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return setView(R.layout.account_spinner_row, position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setView(R.layout.account_spinner_dropdown_row, position, convertView, parent);
    }

    private View setView(int layout, int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(layout, parent, false);
        }
        String accountName = getItem(position);
        if (TextUtils.isEmpty(accountName)) {
            tv.setText(R.string.no_account);
        } else {
            tv.setText(accountName);
        }
        return tv;
    }
}
