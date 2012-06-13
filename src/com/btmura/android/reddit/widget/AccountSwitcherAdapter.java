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

import android.accounts.Account;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class AccountSwitcherAdapter extends BaseAdapter {

    private final ArrayList<Account> accounts = new ArrayList<Account>();
    private final LayoutInflater inflater;

    public AccountSwitcherAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setAccounts(Account[] accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Collections.addAll(this.accounts, accounts);
        }
        notifyDataSetChanged();
    }

    public int getCount() {
        return accounts.size();
    }

    public Account getItem(int position) {
        return accounts.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(R.layout.account_switcher_row, parent, false);
        }
        tv.setText(getItem(position).name);
        return tv;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(R.layout.account_switcher_dropdown_row, parent, false);
        }
        tv.setText(getItem(position).name);
        return tv;
    }

}
