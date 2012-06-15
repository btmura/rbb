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

public class AccountSpinnerAdapter extends BaseAdapter {

    private static final Account NO_ACCOUNT = null;

    private final ArrayList<Account> accounts = new ArrayList<Account>();
    private final LayoutInflater inflater;

    public AccountSpinnerAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setAccounts(Account[] newAccounts) {
        accounts.clear();
        if (newAccounts != null && newAccounts.length > 0) {
            Collections.addAll(accounts, newAccounts);
        } else {
            accounts.add(NO_ACCOUNT);
        }
        notifyDataSetChanged();
    }

    public String getAccountName(int position) {
        Account account = getItem(position);
        return account != null ? account.name : "";
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
        return setView(R.layout.account_spinner_row, position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setView(R.layout.account_spinner_dropdown_row, position, convertView, parent, true);
    }

    private View setView(int layout, int position, View convertView, ViewGroup parent,
            boolean dropdown) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(layout, parent, false);
        }
        Account account = getItem(position);
        if (account != null) {
            tv.setText(account.name);
        } else if (dropdown) {
            tv.setText(R.string.no_account);
        } else {
            tv.setText(R.string.app_name);
        }
        return tv;
    }
}
