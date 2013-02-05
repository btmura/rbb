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
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;

public class AccountFilterAdapter extends BaseFilterAdapter {

    static class Item {
        static final int NUM_TYPES = 3;
        static final int TYPE_CATEGORY = 0;
        static final int TYPE_ACCOUNT_NAME = 1;
        static final int TYPE_FILTER = 2;

        private final int type;
        private final String text1;
        private final String text2;
        private final int value;

        Item(int type, String text1, String text2, int value) {
            this.type = type;
            this.text1 = text1;
            this.text2 = text2;
            this.value = value;
        }
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final ArrayList<Item> items = new ArrayList<Item>();
    private ArrayList<Item> filters;
    private String accountName;
    private String subreddit;
    private int filter;

    public AccountFilterAdapter(Context context) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    protected void clear() {
        if (filters != null) {
            filters.clear();
        }
    }

    @Override
    protected void add(Context context, int resId, int value) {
        if (filters == null) {
            filters = new ArrayList<Item>(6);
        }
        filters.add(new Item(Item.TYPE_FILTER, context.getString(resId), null, value));
    }

    public void setAccountInfo(String[] accountNames, String[] karmaCounts) {
        items.clear();
        if (accountNames != null) {
            int count = accountNames.length;
            for (int i = 0; i < count; i++) {
                String karmaText = karmaCounts != null ? karmaCounts[i] : null;
                addItem(Item.TYPE_ACCOUNT_NAME, accountNames[i], karmaText, -1);
            }
        }
        if (filters != null) {
            addItem(Item.TYPE_CATEGORY, context.getString(R.string.filter_category), null, -1);
            items.addAll(filters);
        }
        notifyDataSetChanged();
    }

    private void addItem(int type, String text1, String text2, int value) {
        items.add(new Item(type, text1, text2, value));
    }

    public void updateState(int position) {
        Item item = getItem(position);
        switch (item.type) {
            case Item.TYPE_ACCOUNT_NAME:
                accountName = item.text1;
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
            if (item.type == Item.TYPE_ACCOUNT_NAME && accountName.equals(item.text1)) {
                return i;
            }
        }
        return -1;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
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
        return Item.NUM_TYPES;
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
        h.accountName.setText(getTitle(accountName, false));

        if (subreddit != null) {
            h.subreddit.setText(Subreddits.getTitle(context, subreddit));
            h.subreddit.setVisibility(View.VISIBLE);
        } else {
            h.subreddit.setVisibility(View.GONE);
        }

        if (filters != null) {
            h.filter.setText(filters.get(filter).text1);
            h.filter.setVisibility(View.VISIBLE);
            h.divider.setVisibility(View.VISIBLE);
        } else {
            h.filter.setVisibility(View.GONE);
            h.divider.setVisibility(View.GONE);
        }
        return v;
    }

    private View makeView(ViewGroup parent) {
        View v = inflater.inflate(R.layout.account_filter_row, parent, false);
        ViewHolder h = new ViewHolder();
        h.accountName = (TextView) v.findViewById(R.id.account_name);
        h.subreddit = (TextView) v.findViewById(R.id.subreddit_name);
        h.filter = (TextView) v.findViewById(R.id.filter);
        h.divider = v.findViewById(R.id.divider);
        v.setTag(h);
        return v;
    }

    static class ViewHolder {
        TextView accountName;
        TextView subreddit;
        TextView filter;
        View divider;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        // Tag each widget with the layout. Check the layout each time to make
        // sure it's the one we want, since a change in the number of accounts
        // can cause prior views to be the wrong type for the new slots. The
        // adapter isn't smart enough to give us the correct views.
        int layout = getDropDownLayout(position);
        if (v == null || layout != ((Integer) v.getTag())) {
            v = inflater.inflate(layout, parent, false);
            v.setTag(Integer.valueOf(layout));
        }

        Item item = getItem(position);

        // TODO: Use more elaborate ViewHolder to avoid finds.

        TextView tv1 = (TextView) v.findViewById(R.id.text1);
        tv1.setText(getTitle(item.text1, true));

        // Only non-category rows have a second textview.
        TextView tv2 = (TextView) v.findViewById(R.id.text2);
        if (tv2 != null) {
            tv2.setText(item.text2);
            tv2.setVisibility(!TextUtils.isEmpty(item.text2) ? View.VISIBLE : View.GONE);
        }

        return v;
    }

    private int getDropDownLayout(int position) {
        switch (getItemViewType(position)) {
            case Item.TYPE_ACCOUNT_NAME:
            case Item.TYPE_FILTER:
                return R.layout.account_filter_dropdown_row;

            case Item.TYPE_CATEGORY:
                return R.layout.account_filter_category_row;

            default:
                throw new IllegalArgumentException();
        }
    }

    private String getTitle(String accountName, boolean dropdown) {
        if (AccountUtils.isAccount(accountName)) {
            return accountName;
        } else if (dropdown) {
            return context.getString(R.string.account_app_storage);
        } else {
            return context.getString(R.string.app_name);
        }
    }
}
