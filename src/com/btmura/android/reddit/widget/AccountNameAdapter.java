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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.util.Array;

/**
 * {@link BaseAdapter} for showing a list of account names.
 */
public class AccountNameAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private String[] accountNames = Array.EMPTY_STRING_ARRAY;

    public AccountNameAdapter(Context context) {
        this.inflater = LayoutInflater.from(context.getApplicationContext());
    }

    public void setAccountNames(String[] accountNames) {
        this.accountNames = accountNames != null ? accountNames : Array.EMPTY_STRING_ARRAY;
        notifyDataSetChanged();
    }

    public int findAccountName(String accountName) {
        int count = accountNames.length;
        for (int i = 0; i < count; i++) {
            if (accountName.equals(accountNames[i])) {
                return i;
            }
        }
        return -1;
    }

    public int getCount() {
        return accountNames.length;
    }

    public String getItem(int position) {
        return accountNames[position];
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) inflater.inflate(R.layout.account_name_row, parent, false);
        setDisplayName(tv, position);
        return tv;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) inflater.inflate(R.layout.account_name_dropdown_row, parent, false);
        setDisplayName(tv, position);
        return tv;
    }

    private void setDisplayName(TextView tv, int position) {
        String accountName = getItem(position);
        if (AccountUtils.isAccount(accountName)) {
            tv.setText(accountName);
        } else {
            tv.setText(R.string.app_name);
        }
    }
}
