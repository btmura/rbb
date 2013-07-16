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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Accounts;

public class NavigationAdapter extends BaseAdapter {

    public static class Item {

        static final int NUM_TYPES = 1;

        public static final int TYPE_ACCOUNT_NAME = 0;

        private final int type;
        private final String text1;
        private final String text2;
        private final String text3;
        private final int value;

        static Item newAccount(Context context, String name, String linkKarma,
                String commentKarma, int hasMail) {
            return new Item(TYPE_ACCOUNT_NAME, name, linkKarma, commentKarma, hasMail);
        }

        private Item(int type, String text1, String text2, String text3, int value) {
            this.type = type;
            this.text1 = text1;
            this.text2 = text2;
            this.text3 = text3;
            this.value = value;
        }

        public int getType() {
            return type;
        }

        public String getAccountName() {
            return text1;
        }
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final ArrayList<Item> items = new ArrayList<Item>();

    public NavigationAdapter(Context context) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
    }

    public void setAccountResult(AccountResult result) {
        items.clear();
        if (result.accountNames != null && result.accountNames.length > 1) {
            int count = result.accountNames.length;
            for (int i = 0; i < count; i++) {
                String text2 = getKarmaCount(result.linkKarma, i);
                String text3 = getKarmaCount(result.commentKarma, i);
                int value = result.hasMail != null && result.hasMail[i] ? 1 : 0;
                addItem(Item.newAccount(context, result.accountNames[i], text2, text3, value));
            }
        }
        notifyDataSetChanged();
    }

    private String getKarmaCount(int[] karmaCounts, int index) {
        if (karmaCounts != null && karmaCounts[index] != -1) {
            return context.getString(R.string.karma_count, karmaCounts[index]);
        }
        return null;
    }

    private void addItem(Item item) {
        items.add(item);
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
    public int getCount() {
        return items.size();
    }

    static class ViewHolder {
        TextView accountFilter;
        ImageView statusIcon;
        View karmaCounts;
        TextView linkKarma;
        TextView commentKarma;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.account_row, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.accountFilter = (TextView) v.findViewById(R.id.account_filter);
            vh.statusIcon = (ImageView) v.findViewById(R.id.status_icon);
            vh.karmaCounts = v.findViewById(R.id.karma_counts);
            vh.linkKarma = (TextView) v.findViewById(R.id.link_karma);
            vh.commentKarma = (TextView) v.findViewById(R.id.comment_karma);
            v.setTag(vh);
        }
        setView(v, position);
        return v;
    }

    private void setView(View view, int position) {
        ViewHolder vh = (ViewHolder) view.getTag();
        Item item = getItem(position);
        switch (item.type) {
            case Item.TYPE_ACCOUNT_NAME:
                vh.accountFilter.setText(Accounts.getTitle(context, item.text1));
                vh.accountFilter.setVisibility(View.VISIBLE);
                vh.accountFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

                if (item.value == 1) {
                    vh.statusIcon.setImageResource(ThemePrefs.getMessagesIcon(view.getContext()));
                    vh.statusIcon.setVisibility(View.VISIBLE);
                } else {
                    vh.statusIcon.setVisibility(View.GONE);
                }

                vh.karmaCounts.setVisibility(View.VISIBLE);
                vh.linkKarma.setText(item.text2);
                vh.commentKarma.setText(item.text3);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }
}
