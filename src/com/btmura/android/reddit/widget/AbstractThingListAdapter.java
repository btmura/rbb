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

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.util.Objects;

public abstract class AbstractThingListAdapter extends BaseCursorAdapter {

    protected int thingBodyWidth;
    protected long nowTimeMs;
    protected String selectedThingId;
    protected String selectedLinkId;

    AbstractThingListAdapter(Context context) {
        super(context, null, 0);
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        switch (getKind(position)) {
            case Kinds.KIND_MORE:
                return 0;

            default:
                return 1;
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        nowTimeMs = System.currentTimeMillis();
        return super.swapCursor(newCursor);
    }

    @Override
    public boolean isEnabled(int position) {
        return getKind(position) != Kinds.KIND_MORE;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        switch (getKind(cursor.getPosition())) {
            case Kinds.KIND_MORE:
                LayoutInflater inflater = LayoutInflater.from(context);
                return inflater.inflate(R.layout.thing_more_row, parent, false);

            default:
                return new ThingView(context);
        }
    }

    public int getKind(int position) {
        return getInt(position, getKindIndex());
    }

    public String getSelectedThingId() {
        return selectedThingId;
    }

    public String getSelectedLinkId() {
        return selectedLinkId;
    }

    public void setSelectedThing(String thingId, String linkId) {
        if (!Objects.equals(selectedThingId, thingId)
                || !Objects.equals(selectedLinkId, linkId)) {
            selectedThingId = thingId;
            selectedLinkId = linkId;
            notifyDataSetChanged();
        }
    }

    public void setSelectedPosition(int position) {
        setSelectedThing(getThingId(position), getLinkId(position));
    }

    // Abstract methods.

    abstract int getAuthorIndex();

    abstract int getKindIndex();

    abstract String getThingId(int position);

    abstract String getLinkId(int position);

}
