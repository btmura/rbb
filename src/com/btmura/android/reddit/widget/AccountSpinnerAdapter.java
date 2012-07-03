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

        public final int type;
        public final String text;

        public Item(int type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final ArrayList<Item> items = new ArrayList<Item>();
    private final boolean showFilters;

    public AccountSpinnerAdapter(Context context, boolean showFilters) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
        this.showFilters = showFilters;
    }

    public void setAccountNames(String[] accountNames) {
        items.clear();
        if (accountNames != null) {
            int count = accountNames.length;
            for (int i = 0; i < count; i++) {
                items.add(new Item(Item.TYPE_ACCOUNT_NAME, accountNames[i]));
            }
        }
        if (showFilters) {
            items.add(new Item(Item.TYPE_CATEGORY, context.getString(R.string.filter_category)));
            items.add(new Item(Item.TYPE_FILTER, context.getString(R.string.filter_hot)));
            items.add(new Item(Item.TYPE_FILTER, context.getString(R.string.filter_top)));
            items.add(new Item(Item.TYPE_FILTER, context.getString(R.string.filter_controversial)));
            items.add(new Item(Item.TYPE_FILTER, context.getString(R.string.filter_new)));
        }
        notifyDataSetChanged();
    }

    public String getAccountName(int position) {
        return getItem(position).text;
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
        return setView(R.layout.account_spinner_row, position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return setView(getLayout(position), position, convertView, parent);
    }

    private int getLayout(int position) {
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
        if (TextUtils.isEmpty(item.text)) {
            tv.setText(R.string.app_name);
        } else {
            tv.setText(item.text);
        }
        return tv;
    }
}
