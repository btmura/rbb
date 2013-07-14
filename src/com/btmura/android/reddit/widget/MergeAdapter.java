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

import java.util.ArrayList;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class MergeAdapter extends BaseAdapter {

    private final AdapterObserver observer = new AdapterObserver();
    private final ArrayList<ListAdapter> adapters;

    public MergeAdapter(int capacity) {
        this.adapters = new ArrayList<ListAdapter>(capacity);
    }

    public void clear() {
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            adapters.get(i).unregisterDataSetObserver(observer);
        }
        adapters.clear();
        notifyDataSetChanged();
    }

    public void add(ListAdapter adapter) {
        adapter.registerDataSetObserver(observer);
        adapters.add(adapter);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        int count = 0;
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            count += adapters.get(i).getCount();
        }
        return count;
    }

    @Override
    public int getViewTypeCount() {
        int count = 0;
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            count += adapters.get(i).getViewTypeCount();
        }
        return count;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        int start = 0;
        int end = 0;
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            ListAdapter adapter = adapters.get(i);
            end = start + adapter.getCount();
            if (position >= start && position < end) {
                return adapter.getItemViewType(position - start);
            }
            start += adapter.getCount();
        }
        return -1;
    }

    @Override
    public Object getItem(int position) {
        int start = 0;
        int end = 0;
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            ListAdapter adapter = adapters.get(i);
            end = start + adapter.getCount();
            if (position >= start && position < end) {
                return adapter.getItem(position - start);
            }
            start += adapter.getCount();
        }
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int start = 0;
        int end = 0;
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            ListAdapter adapter = adapters.get(i);
            end = start + adapter.getCount();
            if (position >= start && position < end) {
                return adapter.getView(position - start, convertView, parent);
            }
            start += adapter.getCount();
        }
        return null;
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
