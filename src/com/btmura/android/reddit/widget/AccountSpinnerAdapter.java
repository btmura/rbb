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

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class AccountSpinnerAdapter extends BaseAdapter {

    static class Item {
        public static final int TYPE_CATEGORY = 0;
        public static final int TYPE_ACCOUNT_NAME = 1;
        public static final int TYPE_FILTER = 2;

        private final int type;
        private final String text;
        private final int value;

        public Item(int type, String text, int value) {
            this.type = type;
            this.text = text;
            this.value = value;
        }
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final ArrayList<Item> items = new ArrayList<Item>();
    private final ArrayList<Item> filters = new ArrayList<Item>();
    private final boolean showFilters;

    private String accountName;
    private String subredditName;
    private int filter;

    public AccountSpinnerAdapter(Context context, boolean showFilters) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
        this.showFilters = showFilters;
        addFilter(R.string.filter_hot, FilterAdapter.FILTER_HOT);
        addFilter(R.string.filter_top, FilterAdapter.FILTER_TOP);
        addFilter(R.string.filter_controversial, FilterAdapter.FILTER_CONTROVERSIAL);
        addFilter(R.string.filter_new, FilterAdapter.FILTER_NEW);
    }

    private void addFilter(int textId, int value) {
        filters.add(new Item(Item.TYPE_FILTER, context.getString(textId), value));
    }

    public void setAccountNames(String[] accountNames) {
        items.clear();
        if (accountNames != null) {
            int count = accountNames.length;
            for (int i = 0; i < count; i++) {
                items.add(new Item(Item.TYPE_ACCOUNT_NAME, accountNames[i], -1));
            }
        }
        if (showFilters) {
            addItem(Item.TYPE_CATEGORY, R.string.filter_category, -1);
            items.addAll(filters);
        }
        notifyDataSetChanged();
    }

    private void addItem(int type, int textId, int value) {
        items.add(new Item(type, context.getString(textId), value));
    }

    public void updateState(int position) {
        Item item = getItem(position);
        switch (item.type) {
            case Item.TYPE_ACCOUNT_NAME:
                accountName = item.text;
                break;

            case Item.TYPE_FILTER:
                filter = item.value;
                break;
        }
        notifyDataSetChanged();
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public int findAccountName(String accountName) {
        int count = items.size();
        for (int i = 0; i < count; i++) {
            Item item = getItem(i);
            if (item.type == Item.TYPE_ACCOUNT_NAME && accountName.equals(item.text)) {
                return i;
            }
        }
        return -1;
    }


    public String getSubredditName() {
        return subredditName;
    }

    public void setSubredditName(String subredditName) {
        this.subredditName = subredditName;
        notifyDataSetChanged();
    }

    public int getFilter() {
        return filter;
    }

    public void setFilter(int filter) {
        this.filter = filter;
        notifyDataSetChanged();
    }

    public Item getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != Item.TYPE_CATEGORY;
    }

    public int getCount() {
        return items.size();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = makeView(parent);
        }
        ViewHolder h = (ViewHolder) v.getTag();
        h.accountName.setText(getDisplayLabel(accountName));
        if (showFilters) {
            h.filter.setText(filters.get(filter).text);
            h.filter.setVisibility(View.VISIBLE);
        } else {
            h.filter.setVisibility(View.GONE);
        }
        return v;
    }

    private View makeView(ViewGroup parent) {
        View v = inflater.inflate(R.layout.account_spinner_row, parent, false);
        ViewHolder h = new ViewHolder();
        h.accountName = (TextView) v.findViewById(R.id.account_name);
        h.filter = (TextView) v.findViewById(R.id.filter);
        v.setTag(h);
        return v;
    }

    static class ViewHolder {
        TextView accountName;
        TextView filter;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setView(getDropDownLayout(position), position, convertView, parent);
    }

    private int getDropDownLayout(int position) {
        switch (getItemViewType(position)) {
            case Item.TYPE_ACCOUNT_NAME:
            case Item.TYPE_FILTER:
                return R.layout.account_spinner_dropdown_row;

            case Item.TYPE_CATEGORY:
                return R.layout.account_spinner_category_row;

            default:
                throw new IllegalArgumentException();
        }
    }

    private View setView(int layout, int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(layout, parent, false);
        }
        Item item = getItem(position);
        tv.setText(getDisplayLabel(item.text));
        return tv;
    }

    private String getDisplayLabel(String accountName) {
        if (TextUtils.isEmpty(accountName)) {
            return context.getString(R.string.app_name);
        } else {
            return accountName;
        }
    }
}
