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

package com.btmura.android.reddit.sidebar;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;

public class DetailsAdapter extends BaseAdapter {

    private Details item;
    private final Context context;
    private final LayoutInflater inflater;

    public DetailsAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void swapData(Details item) {
        this.item = item;
        notifyDataSetChanged();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public int getCount() {
        return item != null ? 1 : 0;
    }

    public Details getItem(int position) {
        return item;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = inflater.inflate(R.layout.details_row, parent, false);

        Details info = getItem(position);

        TextView title = (TextView) v.findViewById(R.id.title);
        title.setText(info.title);

        TextView desc = (TextView) v.findViewById(R.id.description);
        desc.setText(Formatter.formatInfo(context, info.description));
        desc.setMovementMethod(LinkMovementMethod.getInstance());

        return v;
    }
}
