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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Comment;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CommentAdapter extends BaseAdapter {

    private final ArrayList<Comment> items = new ArrayList<Comment>();
    private final Context context;
    private final LayoutInflater inflater;
    private final long now = System.currentTimeMillis() / 1000;

    public CommentAdapter(Context context, LayoutInflater inflater) {
        this.context = context;
        this.inflater = inflater;
    }

    public void swapData(List<Comment> newItems) {
        items.clear();
        if (newItems != null) {
            items.ensureCapacity(items.size() + newItems.size());
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

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = createView(position, parent);
        }
        setView(position, v);
        return v;
    }

    private View createView(int position, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case Comment.TYPE_HEADER:
                return makeView(R.layout.comment_header_row, parent);

            case Comment.TYPE_COMMENT:
                return makeView(R.layout.comment_row, parent);

            default:
                throw new IllegalArgumentException("Unexpected view type: "
                        + getItemViewType(position));
        }
    }

    private View makeView(int layout, ViewGroup parent) {
        View v = inflater.inflate(layout, parent, false);
        v.setTag(createViewHolder(v));
        return v;
    }

    private static ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.title = (TextView) v.findViewById(R.id.title);
        holder.body = (TextView) v.findViewById(R.id.body);
        holder.status = (TextView) v.findViewById(R.id.status);
        return holder;
    }

    static class ViewHolder {
        TextView title;
        TextView body;
        TextView status;
    }

    private void setView(int position, View v) {
        Comment c = getItem(position).assureFormat(context, now);
        ViewHolder h = (ViewHolder) v.getTag();
        switch (c.type) {
            case Comment.TYPE_HEADER:
                setHeader(h, c);
                break;

            case Comment.TYPE_COMMENT:
                setComment(h, c);
                break;

            default:
                throw new IllegalArgumentException("Unsupported view type: " + c.type);
        }
    }

    private void setHeader(ViewHolder h, Comment c) {
        h.title.setText(c.title);
        h.body.setMovementMethod(LinkMovementMethod.getInstance());
        h.body.setVisibility(c.body != null && c.body.length() > 0 ? View.VISIBLE : View.GONE);
        h.body.setText(c.body);
        h.status.setText(c.status);
    }

    private void setComment(ViewHolder h, Comment c) {
        h.body.setMovementMethod(LinkMovementMethod.getInstance());
        h.body.setText(c.body);
        h.status.setText(c.status);
        setPadding(h.body, c.nesting);
        setPadding(h.status, c.nesting);
    }

    private static void setPadding(View v, int nesting) {
        v.setPadding(v.getPaddingRight() + nesting * 20, v.getPaddingTop(), v.getPaddingRight(),
                v.getPaddingBottom());
    }
}
