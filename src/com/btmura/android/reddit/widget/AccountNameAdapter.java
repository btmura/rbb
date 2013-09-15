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

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.util.Objects;

/**
 * {@link BaseAdapter} for showing a list of account names.
 */
public class AccountNameAdapter extends BaseAdapter {

    private final ArrayList<String> accountNames = new ArrayList<String>();
    private final LayoutInflater inflater;
    private final int layout;

    public AccountNameAdapter(Context context, int layout) {
        this.inflater = LayoutInflater.from(context);
        this.layout = layout;
    }

    public void clear() {
        accountNames.clear();
        notifyDataSetChanged();
    }

    public void add(String accountName) {
        accountNames.add(accountName);
        notifyDataSetChanged();
    }

    public void addAll(String[] accountNames) {
        Collections.addAll(this.accountNames, accountNames);
        notifyDataSetChanged();
    }

    public int findAccountName(String accountName) {
        int count = getCount();
        for (int i = 0; i < count; i++) {
            if (Objects.equals(accountName, getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public int getCount() {
        return accountNames.size();
    }

    public String getItem(int position) {
        return accountNames.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) inflater.inflate(layout, parent, false);
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
            tv.setText(R.string.account_app_storage);
        }
    }
}
