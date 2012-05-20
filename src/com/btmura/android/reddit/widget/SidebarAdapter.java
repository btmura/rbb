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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Subreddit;

public class SidebarAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DESCRIPTION = 1;

    private final LayoutInflater inflater;

    private Subreddit item;

    public SidebarAdapter(Context context) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void swapData(Subreddit item) {
        this.item = item;
        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public int getCount() {
        return item != null ? 2 : 0;
    }

    public Subreddit getItem(int position) {
        return item;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(getLayout(position), parent, false);
        }

        Subreddit sr = getItem(position);
        setSubreddit(sr, v, position);

        return v;
    }

    private int getLayout(int position) {
        switch (getItemViewType(position)) {
            case TYPE_HEADER:
                return R.layout.sidebar_header_row;

            case TYPE_DESCRIPTION:
                return R.layout.sidebar_row;

            default:
                throw new IllegalArgumentException();
        }
    }

    private void setSubreddit(Subreddit sr, View v, int position) {
        switch (getItemViewType(position)) {
            case TYPE_HEADER:
                TextView title = (TextView) v.findViewById(R.id.title);
                title.setText(sr.title);

                TextView status = (TextView) v.findViewById(R.id.status);
                status.setText(sr.status);
                break;

            case TYPE_DESCRIPTION:
                TextView desc = (TextView) v;
                desc.setText(sr.description);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }
}
