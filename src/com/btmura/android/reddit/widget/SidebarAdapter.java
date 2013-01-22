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
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.net.RedditApi.SidebarResult;

public class SidebarAdapter extends BaseAdapter implements OnClickListener {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DESCRIPTION = 1;

    public interface OnSidebarButtonClickListener {
        void onAddClicked();

        void onViewClicked();
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final boolean showHeaderButtons;

    private OnSidebarButtonClickListener listener;
    private SidebarResult item;

    public SidebarAdapter(Context context, boolean showHeaderButtons) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.showHeaderButtons = showHeaderButtons;
    }

    public void setOnSidebarButtonClickListener(OnSidebarButtonClickListener listener) {
        this.listener = listener;
    }

    public void swapData(SidebarResult item) {
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

    public SidebarResult getItem(int position) {
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

        SidebarResult sr = getItem(position);
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

    private void setSubreddit(SidebarResult sb, View v, int position) {
        switch (getItemViewType(position)) {
            case TYPE_HEADER:
                TextView title = (TextView) v.findViewById(R.id.title);
                title.setText(sb.title);

                TextView status = (TextView) v.findViewById(R.id.status);
                status.setText(context.getResources().getQuantityString(R.plurals.subscribers,
                        sb.subscribers, sb.subscribers));

                int visibility = showHeaderButtons ? View.VISIBLE : View.GONE;

                View add = v.findViewById(R.id.add);
                add.setVisibility(visibility);
                add.setOnClickListener(this);

                View view = v.findViewById(R.id.view);
                view.setVisibility(visibility);
                view.setOnClickListener(this);

                break;

            case TYPE_DESCRIPTION:
                TextView desc = (TextView) v;
                desc.setText(sb.description);
                desc.setMovementMethod(LinkMovementMethod.getInstance());
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public void onClick(View v) {
        if (listener != null) {
            switch (v.getId()) {
                case R.id.add:
                    listener.onAddClicked();
                    break;

                case R.id.view:
                    listener.onViewClicked();
                    break;

                default:
                    throw new IllegalArgumentException();
            }
        }
    }
}
