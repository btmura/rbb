/*
 * Copyright (C) 2013 Brian Muramatsu
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

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class MergeAdapter extends BaseAdapter {

    private final ListAdapter[] adapters;

    public MergeAdapter(ListAdapter... adapters) {
        this.adapters = adapters;

        AdapterObserver observer = new AdapterObserver();
        for (int i = 0; i < adapters.length; i++) {
            adapters[i].registerDataSetObserver(observer);
        }
    }

    @Override
    public int getCount() {
        int count = 0;
        int adapterCount = adapters.length;
        for (int i = 0; i < adapterCount; i++) {
            count += adapters[i].getCount();
        }
        return count;
    }

    @Override
    public int getViewTypeCount() {
        int count = 0;
        int adapterCount = adapters.length;
        for (int i = 0; i < adapterCount; i++) {
            count += adapters[i].getViewTypeCount();
        }
        return count;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        int adapterIndex = getAdapterIndex(position);
        int adapterPosition = getAdapterPosition(position);

        int viewTypeOffset = 0;
        for (int i = 0; i < adapterIndex; i++) {
            viewTypeOffset += adapters[i].getViewTypeCount();
        }
        return viewTypeOffset + adapters[adapterIndex].getItemViewType(adapterPosition);
    }

    @Override
    public Object getItem(int position) {
        int adapterIndex = getAdapterIndex(position);
        int adapterPosition = getAdapterPosition(position);
        return adapters[adapterIndex].getItem(adapterPosition);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int adapterIndex = getAdapterIndex(position);
        int adapterPosition = getAdapterPosition(position);
        return adapters[adapterIndex].getView(adapterPosition, convertView, parent);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        int adapterIndex = getAdapterIndex(position);
        int adapterPosition = getAdapterPosition(position);
        return adapters[adapterIndex].isEnabled(adapterPosition);
    }

    public int getAdapterIndex(int position) {
        int start = 0;
        int end = 0;
        int adapterCount = adapters.length;
        for (int i = 0; i < adapterCount; i++) {
            ListAdapter adapter = adapters[i];
            end = start + adapter.getCount();
            if (position >= start && position < end) {
                return i;
            }
            start += adapter.getCount();
        }
        return -1;
    }

    public int getAdapterPosition(int position) {
        int start = 0;
        int end = 0;
        int adapterCount = adapters.length;
        for (int i = 0; i < adapterCount; i++) {
            ListAdapter adapter = adapters[i];
            end = start + adapter.getCount();
            if (position >= start && position < end) {
                return position - start;
            }
            start += adapter.getCount();
        }
        return -1;
    }

    class AdapterObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            super.onChanged();
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            notifyDataSetInvalidated();
        }
    }
}
