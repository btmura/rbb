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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.btmura.android.reddit.R;

import java.util.ArrayList;

public class FilterAdapter extends BaseFilterAdapter {

  private final LayoutInflater inflater;
  private final ArrayList<String> names = new ArrayList<String>(7);
  private final ArrayList<Integer> values = new ArrayList<Integer>(7);
  private CharSequence title;
  private CharSequence subtitle;

  public FilterAdapter(Context ctx) {
    inflater = LayoutInflater.from(ctx);
  }

  @Override
  public void clear() {
    names.clear();
    values.clear();
  }

  @Override
  protected void add(Context ctx, int resId, int value) {
    names.add(ctx.getString(resId));
    values.add(value);
  }

  public int findFilter(int filter) {
    int count = values.size();
    for (int i = 0; i < count; i++) {
      if (values.get(i) == filter) {
        return i;
      }
    }
    return -1;
  }

  public int getFilter(int pos) {
    return values.get(pos);
  }

  public int getCount() {
    return names.size();
  }

  public String getItem(int pos) {
    return names.get(pos);
  }

  public long getItemId(int pos) {
    return pos;
  }

  public void setTitle(CharSequence title) {
    this.title = title;
    notifyDataSetChanged();
  }

  public void setSubtitle(CharSequence subtitle) {
    this.subtitle = subtitle;
    notifyDataSetChanged();
  }

  static class ViewHolder {
    TextView text1;
    TextView text2;
    TextView text3;
  }

  public View getView(int pos, View convertView, ViewGroup parent) {
    View v = convertView;
    if (v == null) {
      v = makeView(parent);
    }

    ViewHolder h = (ViewHolder) v.getTag();
    h.text1.setText(title);
    if (!TextUtils.isEmpty(subtitle)) {
      h.text2.setText(subtitle);
      h.text2.setVisibility(View.VISIBLE);
    } else {
      h.text2.setVisibility(View.GONE);
    }
    h.text3.setText(getItem(pos));
    return v;
  }

  private View makeView(ViewGroup parent) {
    View v = inflater.inflate(R.layout.filter_row, parent, false);
    ViewHolder h = new ViewHolder();
    h.text1 = (TextView) v.findViewById(R.id.text1);
    h.text2 = (TextView) v.findViewById(R.id.text2);
    h.text3 = (TextView) v.findViewById(R.id.text3);
    v.setTag(h);
    return v;
  }

  // TODO: Remove code duplication with AccountFilterAdapter.
  @Override
  public View getDropDownView(int pos, View convertView, ViewGroup parent) {
    View v = convertView;
    if (v == null) {
      v = inflater.inflate(R.layout.account_filter_dropdown_row, parent, false);
    }
    TextView tv = (TextView) v.findViewById(R.id.account_filter);
    tv.setText(getItem(pos));
    return v;
  }
}
