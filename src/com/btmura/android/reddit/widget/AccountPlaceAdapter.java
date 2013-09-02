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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.util.Objects;

public class AccountPlaceAdapter extends BaseAdapter implements OnClickListener {

    static class PlaceItem {

        static final int NUM_TYPES = 2;

        public static final int TYPE_CATEGORY = 0;
        public static final int TYPE_PLACE_BUTTONS = 1;

        private final int type;
        private final String text;

        static PlaceItem newCategory(Context context, int textResId) {
            return new PlaceItem(TYPE_CATEGORY, context.getString(textResId));
        }

        static PlaceItem newPlaceButtons() {
            return new PlaceItem(TYPE_PLACE_BUTTONS, null);
        }

        private PlaceItem(int type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    public interface OnPlaceSelectedListener {

        public static final int PLACE_SUBREDDIT = 0;
        public static final int PLACE_PROFILE = 1;
        public static final int PLACE_SAVED = 2;
        public static final int PLACE_MESSAGES = 3;

        void onPlaceSelected(int place);
    }

    private final ArrayList<PlaceItem> items = new ArrayList<PlaceItem>();
    private final Context context;
    private final LayoutInflater inflater;
    private final OnPlaceSelectedListener listener;
    private int selectedPlace;

    public AccountPlaceAdapter(Context context, OnPlaceSelectedListener listener) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setSelectedPlace(int place) {
        if (!Objects.equals(this.selectedPlace, place)) {
            this.selectedPlace = place;
            notifyDataSetChanged();
        }
    }

    public void setAccountPlaces(boolean showDivider, boolean showPlaces) {
        items.clear();
        if (showDivider) {
            addItem(PlaceItem.newCategory(context, R.string.subreddits_category));
        }
        if (showPlaces) {
            addItem(PlaceItem.newPlaceButtons());
        }
        notifyDataSetChanged();
    }

    private void addItem(PlaceItem item) {
        items.add(item);
    }

    public PlaceItem getItem(int position) {
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
        return PlaceItem.NUM_TYPES;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != PlaceItem.TYPE_CATEGORY;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    static class ViewHolder {
        TextView category;
        View placeButtons;

        View subredditButton;
        View profileButton;
        View savedButton;
        View messagesButton;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.place_row, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.placeButtons = v.findViewById(R.id.place_buttons);
            vh.subredditButton = vh.placeButtons.findViewById(R.id.subreddit_button);
            vh.profileButton = vh.placeButtons.findViewById(R.id.profile_button);
            vh.savedButton = vh.placeButtons.findViewById(R.id.saved_button);
            vh.messagesButton = vh.placeButtons.findViewById(R.id.messages_button);
            vh.category = (TextView) v.findViewById(R.id.category);
            v.setTag(vh);
        }
        setView(v, position);
        return v;
    }

    private void setView(View view, int position) {
        ViewHolder vh = (ViewHolder) view.getTag();
        PlaceItem item = getItem(position);
        switch (item.type) {
            case PlaceItem.TYPE_PLACE_BUTTONS:
                vh.placeButtons.setVisibility(View.VISIBLE);
                vh.category.setVisibility(View.GONE);

                vh.subredditButton.setOnClickListener(this);
                vh.profileButton.setOnClickListener(this);
                vh.savedButton.setOnClickListener(this);
                vh.messagesButton.setOnClickListener(this);

                vh.subredditButton.setActivated(isPlace(OnPlaceSelectedListener.PLACE_SUBREDDIT));
                vh.profileButton.setActivated(isPlace(OnPlaceSelectedListener.PLACE_PROFILE));
                vh.savedButton.setActivated(isPlace(OnPlaceSelectedListener.PLACE_SAVED));
                vh.messagesButton.setActivated(isPlace(OnPlaceSelectedListener.PLACE_MESSAGES));
                break;

            case PlaceItem.TYPE_CATEGORY:
                vh.placeButtons.setVisibility(View.GONE);
                vh.category.setVisibility(View.VISIBLE);
                vh.category.setText(item.text);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean isPlace(int place) {
        return selectedPlace == place;
    }

    @Override
    public void onClick(View v) {
        if (listener != null) {
            switch (v.getId()) {
                case R.id.subreddit_button:
                    listener.onPlaceSelected(OnPlaceSelectedListener.PLACE_SUBREDDIT);
                    break;

                case R.id.profile_button:
                    listener.onPlaceSelected(OnPlaceSelectedListener.PLACE_PROFILE);
                    break;

                case R.id.saved_button:
                    listener.onPlaceSelected(OnPlaceSelectedListener.PLACE_SAVED);
                    break;

                case R.id.messages_button:
                    listener.onPlaceSelected(OnPlaceSelectedListener.PLACE_MESSAGES);
                    break;
            }
        }
    }
}
