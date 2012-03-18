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

package com.btmura.android.reddit.browser;

import java.util.ArrayList;
import java.util.List;

import com.btmura.android.reddit.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ThingAdapter extends BaseAdapter {

    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final ArrayList<Thing> items = new ArrayList<Thing>();
    private final Context context;
    private final LayoutInflater inflater;
    private final String parentSubreddit;
    private final boolean singleChoice;
    private final long now = System.currentTimeMillis() / 1000;
    private int chosenPosition = -1;

    public ThingAdapter(Context context, LayoutInflater inflater, Subreddit subreddit,
            boolean singleChoice) {
        this.context = context;
        this.inflater = inflater;
        this.parentSubreddit = subreddit.name;
        this.singleChoice = singleChoice;
    }

    public void swapData(List<Thing> newItems) {
        items.clear();
        if (newItems != null) {
            items.ensureCapacity(items.size() + newItems.size());
            items.addAll(newItems);
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    public void setChosenPosition(int position) {
        chosenPosition = position;
    }

    public int getChosenPosition() {
        return chosenPosition;
    }

    public List<Thing> getItems() {
        return items;
    }

    public int getCount() {
        return items.size();
    }

    public Thing getItem(int position) {
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
        View v = convertView;
        if (v == null) {
            v = createView(position, parent);
        }
        setView(position, v);
        return v;
    }

    private View createView(int position, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case Thing.TYPE_THING:
                return makeView(R.layout.thing_row, parent);

            case Thing.TYPE_MORE:
                return makeView(R.layout.thing_more_row, parent);

            default:
                throw new IllegalArgumentException();
        }
    }

    private View makeView(int layout, ViewGroup parent) {
        View v = inflater.inflate(layout, parent, false);
        v.setTag(createViewHolder(v));
        return v;
    }

    private static ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.thumbnailStub = (ViewStub) v.findViewById(R.id.thumbnail_stub);
        holder.thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
        holder.title = (TextView) v.findViewById(R.id.title);
        holder.status = (TextView) v.findViewById(R.id.status);
        holder.progress = (ProgressBar) v.findViewById(R.id.progress);
        return holder;
    }

    static class ViewHolder {
        ViewStub thumbnailStub;
        ImageView thumbnail;
        TextView title;
        TextView status;
        ProgressBar progress;
    }

    private void setView(int position, View v) {
        Thing t = getItem(position).assureFormat(context, parentSubreddit, now);
        ViewHolder h = (ViewHolder) v.getTag();
        switch (t.type) {
            case Thing.TYPE_THING:
                setThing(v, h, t, position);
                break;

            case Thing.TYPE_MORE:
                setMore(h, t);
                break;

            default:
                throw new IllegalArgumentException("Unsupported view type: "
                        + getItemViewType(position));
        }
    }

    private void setThing(View v, ViewHolder h, Thing t, int position) {
        v.setBackgroundResource(singleChoice && position == chosenPosition ? R.drawable.selector_chosen
                : R.drawable.selector_normal);
        h.title.setText(t.title);
        h.status.setText(t.status);
        setThumbnail(v, h, t);
    }

    private void setThumbnail(View v, ViewHolder h, Thing t) {
        if (t.hasThumbnail()) {
            if (h.thumbnail == null) {
                h.thumbnail = (ImageView) h.thumbnailStub.inflate();
                h.thumbnailStub = null;
            }
            thumbnailLoader.setThumbnail(h.thumbnail, t.thumbnail);
        } else {
            if (h.thumbnail != null) {
                thumbnailLoader.clearThumbnail(h.thumbnail);
            }
        }
    }

    private void setMore(ViewHolder h, Thing e) {
        h.title.setCompoundDrawables(null, null, null, null);
    }
}
