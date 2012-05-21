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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Thing;

public class ThingAdapter extends BaseAdapter {

    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final ArrayList<Thing> items = new ArrayList<Thing>(30);
    private final Context context;
    private final LayoutInflater inflater;
    private final String subredditName;
    private final boolean singleChoice;
    private final long now = System.currentTimeMillis() / 1000;

    private String selectedName;
    private int selectedPosition;
    private int bodyWidth;

    public ThingAdapter(Context context, String subredditName, boolean singleChoice) {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.subredditName = subredditName;
        this.singleChoice = singleChoice;
    }

    public void swapData(List<Thing> newItems) {
        items.clear();
        if (newItems != null) {
            items.ensureCapacity(newItems.size());
            items.addAll(newItems);
            notifyDataSetChanged();
        } else {
            notifyDataSetInvalidated();
        }
    }

    public void setSelectedThing(String name, int position) {
        selectedName = name;
        selectedPosition = position;
    }

    public String getSelectedThingName() {
        return selectedName;
    }

    public int getSelectedThingPosition() {
        return selectedPosition;
    }

    public boolean isSelectedThing(String name, int position) {
        if (position != selectedPosition) {
            return false;
        }
        if (selectedName != null) {
            return selectedName.equals(name);
        }
        return name == null;
    }

    public void setThingBodyWidth(int bodyWidth) {
        this.bodyWidth = bodyWidth;
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
                return new ThingView(context);

            case Thing.TYPE_MORE:
                return inflater.inflate(R.layout.thing_more_row, parent, false);

            default:
                throw new IllegalArgumentException();
        }
    }

    private void setView(int position, View v) {
        Thing t = getItem(position).assureFormat(context, subredditName, now);
        switch (t.type) {
            case Thing.TYPE_THING:
                setThing((ThingView) v, t, position);
                break;

            case Thing.TYPE_MORE:
                break;

            default:
                throw new IllegalArgumentException("Unsupported view type: "
                        + getItemViewType(position));
        }
    }

    private void setThing(ThingView v, Thing t, int position) {
        int resId;
        if (singleChoice && position == selectedPosition && t.name.equals(selectedName)) {
            resId = R.drawable.selector_chosen;
        } else {
            resId = R.drawable.selector_normal;
        }
        v.setBackgroundResource(resId);
        v.setBodyWidth(bodyWidth);
        v.setThing(t);
        if (t.hasThumbnail()) {
            thumbnailLoader.setThumbnail(v, t.thumbnail);
        } else {
            thumbnailLoader.clearThumbnail(v);
        }
    }
}
