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
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class FilterAdapter extends BaseFilterAdapter {

    private final LayoutInflater inflater;
    private final ArrayList<String> names = new ArrayList<String>(6);
    private CharSequence title;

    public FilterAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    protected void clear() {
        names.clear();
    }

    @Override
    protected void add(Context context, int resId, int value) {
        names.add(context.getString(resId));
    }

    public int getFilter(int position) {
        return position;
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

    static class ViewHolder {
        TextView text1;
        TextView text2;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = makeView(parent);
        }

        ViewHolder h = (ViewHolder) v.getTag();
        h.text1.setText(title);
        h.text2.setText(getItem(position));
        return v;
    }

    private View makeView(ViewGroup parent) {
        View v = inflater.inflate(R.layout.filter_row, parent, false);
        ViewHolder h = new ViewHolder();
        h.text1 = (TextView) v.findViewById(R.id.text1);
        h.text2 = (TextView) v.findViewById(R.id.text2);
        v.setTag(h);
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.account_filter_dropdown_row, parent, false);
        }
        TextView tv = (TextView) v.findViewById(R.id.text1);
        tv.setText(getItem(position));
        return v;
    }
}
