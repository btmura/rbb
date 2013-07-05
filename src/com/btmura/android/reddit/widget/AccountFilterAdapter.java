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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.ThemePrefs;
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
        private final String text3;
        private final int value;

        Item(int type, String text1, String text2, String text3, int value) {
            this.type = type;
            this.text1 = text1;
            this.text2 = text2;
            this.text3 = text3;
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
        filters.add(new Item(Item.TYPE_FILTER, context.getString(resId), null, null, value));
    }

    public void setAccountInfo(String[] accountNames, int[] linkKarma, int[] commentKarma,
            boolean[] hasMail) {
        items.clear();
        if (accountNames != null) {
            int count = accountNames.length;
            for (int i = 0; i < count; i++) {
                String text2 = getKarmaCount(linkKarma, i);
                String text3 = getKarmaCount(commentKarma, i);
                int value = hasMail != null && hasMail[i] ? 1 : 0;
                addItem(Item.TYPE_ACCOUNT_NAME, accountNames[i], text2, text3, value);
            }
        }
        if (filters != null) {
            addItem(Item.TYPE_CATEGORY, context.getString(R.string.filter_category),
                    null, null, -1);
            items.addAll(filters);
        }
        notifyDataSetChanged();
    }

    private String getKarmaCount(int[] karmaCounts, int index) {
        if (karmaCounts != null && karmaCounts[index] != -1) {
            return context.getString(R.string.karma_count, karmaCounts[index]);
        }
        return null;
    }

    private void addItem(int type, String text1, String text2, String text3, int value) {
        items.add(new Item(type, text1, text2, text3, value));
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

    static class ViewHolder {
        TextView accountName;
        TextView subreddit;
        TextView filter;
        View divider;
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

    static class DropDownViewHolder {
        TextView accountFilter;
        ImageView statusIcon;
        View karmaCounts;
        TextView linkKarma;
        TextView commentKarma;
        TextView category;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.account_filter_dropdown_row, parent, false);
            DropDownViewHolder vh = new DropDownViewHolder();
            vh.accountFilter = (TextView) v.findViewById(R.id.account_filter);
            vh.statusIcon = (ImageView) v.findViewById(R.id.status_icon);
            vh.karmaCounts = v.findViewById(R.id.karma_counts);
            vh.linkKarma = (TextView) v.findViewById(R.id.link_karma);
            vh.commentKarma = (TextView) v.findViewById(R.id.comment_karma);
            vh.category = (TextView) v.findViewById(R.id.category);
            v.setTag(vh);
        }
        setDropdownView(v, position);
        return v;
    }

    private void setDropdownView(View view, int position) {
        DropDownViewHolder vh = (DropDownViewHolder) view.getTag();
        Item item = getItem(position);
        switch (item.type) {
            case Item.TYPE_ACCOUNT_NAME:
                vh.accountFilter.setText(getTitle(item.text1, true));
                vh.accountFilter.setVisibility(View.VISIBLE);

                if (item.value == 1) {
                    vh.statusIcon.setImageResource(ThemePrefs.getMessagesIcon(view.getContext()));
                    vh.statusIcon.setVisibility(View.VISIBLE);
                } else {
                    vh.statusIcon.setVisibility(View.GONE);
                }

                vh.karmaCounts.setVisibility(View.VISIBLE);
                vh.linkKarma.setText(item.text2);
                vh.commentKarma.setText(item.text3);
                vh.category.setVisibility(View.GONE);
                break;

            case Item.TYPE_FILTER:
                vh.accountFilter.setText(item.text1);
                vh.accountFilter.setVisibility(View.VISIBLE);
                vh.statusIcon.setVisibility(View.GONE);
                vh.karmaCounts.setVisibility(View.GONE);
                vh.category.setVisibility(View.GONE);
                break;

            case Item.TYPE_CATEGORY:
                vh.accountFilter.setVisibility(View.GONE);
                vh.statusIcon.setVisibility(View.GONE);
                vh.karmaCounts.setVisibility(View.GONE);
                vh.category.setText(item.text1);
                vh.category.setVisibility(View.VISIBLE);
                break;

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
