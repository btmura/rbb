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
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Comment;

public class CommentAdapter extends BaseAdapter {

    private final ArrayList<Comment> items = new ArrayList<Comment>();
    private final Context context;

    public CommentAdapter(Context context) {
        this.context = context;
    }

    public void swapData(List<Comment> newItems) {
        items.clear();
        if (newItems != null) {
            items.ensureCapacity(newItems.size());
            items.addAll(newItems);
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    public int getCount() {
        return items.size();
    }

    public Comment getItem(int position) {
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
        return 2;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        CommentView v = (CommentView) convertView;
        if (v == null) {
            v = new CommentView(context);
        }
        v.setBackgroundResource(R.drawable.selector_normal);
        v.setComment(getItem(position));
        return v;
    }
}
