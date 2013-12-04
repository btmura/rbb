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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.util.Objects;

/**
 * Adapter used for showing accounts in the navigation drawer.
 */
public class AccountResultAdapter extends BaseAdapter implements OnClickListener {

    private final ArrayList<Item> items = new ArrayList<Item>();
    private final Context context;
    private final LayoutInflater inflater;
    private final int layout;

    private OnAccountMessagesSelectedListener listener;
    private String selectedAccountName;

    public interface OnAccountMessagesSelectedListener {
        void onAccountMessagesSelected(String accountName);
    }

    public static class Item {

        private final String accountName;
        private final String linkKarma;
        private final String commentKarma;
        private final boolean hasMail;

        private Item(String accountName, String linkKarma, String commentKarma, boolean hasMail) {
            this.accountName = accountName;
            this.linkKarma = linkKarma;
            this.commentKarma = commentKarma;
            this.hasMail = hasMail;
        }

        public String getAccountName() {
            return accountName;
        }
    }

    public static AccountResultAdapter newNavigationFragmentInstance(Context context) {
        return new AccountResultAdapter(context, R.layout.account_navigation_row);
    }

    public static AccountResultAdapter newAccountListInstance(Context context) {
        return new AccountResultAdapter(context, R.layout.account_list_row);
    }

    public static AccountResultAdapter newNameInstance(Context context) {
        return new AccountResultAdapter(context, R.layout.account_name_row);
    }

    private AccountResultAdapter(Context context, int layout) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.layout = layout;
    }

    public void setOnAccountMessagesSelectedListener(OnAccountMessagesSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedAccountName(String accountName) {
        if (!Objects.equals(this.selectedAccountName, accountName)) {
            this.selectedAccountName = accountName;
            notifyDataSetChanged();
        }
    }

    public void setAccountResult(AccountResult result) {
        items.clear();
        // Only show names if we have more than the app storage account.
        if (result != null && result.accountNames != null && result.accountNames.length > 1) {
            int count = result.accountNames.length;
            for (int i = 0; i < count; i++) {
                String linkKarma = getKarmaCount(result.linkKarma, i);
                String commentKarma = getKarmaCount(result.commentKarma, i);
                boolean hasMail = getHasMail(result.hasMail, i);
                add(result.accountNames[i], linkKarma, commentKarma, hasMail);
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

    private boolean getHasMail(boolean[] hasMail, int index) {
        return hasMail != null && hasMail[index];
    }

    private void add(String accountName, String linkKarma, String commentKarma, boolean hasMail) {
        items.add(new Item(accountName, linkKarma, commentKarma, hasMail));
    }

    public int findAccountName(String accountName) {
        int count = items.size();
        for (int i = 0; i < count; i++) {
            Item item = getItem(i);
            if (accountName.equals(item.accountName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Item getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    static class ViewHolder {
        TextView accountName;
        ImageButton messagesButton;
        TextView linkKarma;
        TextView commentKarma;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(layout, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.accountName = (TextView) v.findViewById(R.id.account_name);
            vh.messagesButton = (ImageButton) v.findViewById(R.id.messages_button);
            vh.linkKarma = (TextView) v.findViewById(R.id.link_karma);
            vh.commentKarma = (TextView) v.findViewById(R.id.comment_karma);
            v.setTag(vh);
        }
        setView(v, position);
        return v;
    }

    private void setView(View view, int position) {
        Item item = getItem(position);
        boolean activated = Objects.equals(selectedAccountName, item.accountName);
        ViewHolder vh = (ViewHolder) view.getTag();

        if (vh.accountName != null) {
            vh.accountName.setText(Accounts.getTitle(context, item.accountName));
            vh.accountName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            vh.accountName.setActivated(activated);
        }

        if (vh.messagesButton != null) {
            vh.messagesButton.setVisibility(item.hasMail ? View.VISIBLE : View.GONE);
            vh.messagesButton.setTag(item.accountName);
            vh.messagesButton.setOnClickListener(this);
        }

        if (vh.linkKarma != null) {
            vh.linkKarma.setText(item.linkKarma);
            vh.linkKarma.setActivated(activated);
        }

        if (vh.commentKarma != null) {
            vh.commentKarma.setText(item.commentKarma);
            vh.commentKarma.setActivated(activated);
        }
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        Item item = getItem(position);
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(R.layout.account_name_dropdown_row, parent, false);
        }
        tv.setText(Accounts.getTitle(context, item.accountName));
        return tv;
    }

    @Override
    public void onClick(View v) {
        if (listener != null) {
            String accountName = (String) v.getTag();
            listener.onAccountMessagesSelected(accountName);
        }
    }
}
