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

package com.btmura.android.reddit.activity;

import java.util.ArrayList;

import com.btmura.android.reddit.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FilterAdapter extends BaseAdapter {

    public static final int FILTER_HOT = 0;
    public static final int FILTER_NEW = 1;
    public static final int FILTER_CONTROVERSIAL = 2;
    public static final int FILTER_TOP = 3;

    private final LayoutInflater inflater;
    private final ArrayList<String> names = new ArrayList<String>(4);
    private CharSequence title;

    public FilterAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        names.add(context.getString(R.string.filter_hot));
        names.add(context.getString(R.string.filter_new));
        names.add(context.getString(R.string.filter_controversial));
        names.add(context.getString(R.string.filter_top));
    }

    public int getCount() {
        return names.size();
    }

    public String getItem(int position) {
        return names.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = makeView(parent);
        }

        ViewHolder h = (ViewHolder) v.getTag();
        h.line1.setText(title);
        h.line2.setText(getItem(position));
        return v;
    }

    private View makeView(ViewGroup parent) {
        View v = inflater.inflate(R.layout.filter_row, parent, false);
        ViewHolder h = new ViewHolder();
        h.line1 = (TextView) v.findViewById(R.id.line1);
        h.line2 = (TextView) v.findViewById(R.id.line2);
        v.setTag(h);
        return v;
    }

    static class ViewHolder {
        TextView line1;
        TextView line2;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        TextView tv = (TextView) v.findViewById(android.R.id.text1);
        tv.setText(getItem(position));
        return v;
    }
}
