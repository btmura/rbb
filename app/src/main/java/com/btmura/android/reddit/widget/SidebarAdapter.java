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
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.net.SidebarResult;

public class SidebarAdapter extends BaseAdapter {

  private static final int TYPE_HEADER = 0;
  private static final int TYPE_DESCRIPTION = 1;

  private final Context ctx;
  private final LayoutInflater inflater;

  private SidebarResult result;

  public SidebarAdapter(Context ctx) {
    this.ctx = ctx;
    this.inflater = LayoutInflater.from(ctx);
  }

  public void swapData(SidebarResult result) {
    this.result = result;
    notifyDataSetChanged();
  }

  @Override
  public boolean isEnabled(int pos) {
    return getItemViewType(pos) == TYPE_HEADER;
  }

  @Override
  public int getCount() {
    if (result != null) {
      return !TextUtils.isEmpty(result.description) ? 2 : 1;
    }
    return 0;
  }

  @Override
  public SidebarResult getItem(int pos) {
    return result;
  }

  @Override
  public long getItemId(int pos) {
    return pos;
  }

  @Override
  public int getItemViewType(int pos) {
    return pos;
  }

  @Override
  public int getViewTypeCount() {
    return 2;
  }

  public View getView(int pos, View convertView, ViewGroup parent) {
    View v = convertView;
    if (v == null) {
      v = inflater.inflate(getLayout(pos), parent, false);
    }
    setSubreddit(v, pos);
    return v;
  }

  private int getLayout(int pos) {
    switch (getItemViewType(pos)) {
      case TYPE_HEADER:
        return R.layout.sidebar_header_row;

      case TYPE_DESCRIPTION:
        return R.layout.sidebar_row;

      default:
        throw new IllegalArgumentException();
    }
  }

  private void setSubreddit(View v, int pos) {
    switch (getItemViewType(pos)) {
      case TYPE_HEADER:
        ImageView headerImage = (ImageView) v.findViewById(R.id.header_image);
        if (result.headerImageBitmap != null) {
          headerImage.setImageBitmap(result.headerImageBitmap);
          headerImage.setVisibility(View.VISIBLE);
        } else {
          headerImage.setVisibility(View.GONE);
        }

        TextView title = (TextView) v.findViewById(R.id.title);
        title.setText(result.title);

        TextView status = (TextView) v.findViewById(R.id.status);
        status.setText(
            ctx.getResources().getQuantityString(R.plurals.subscribers,
                result.subscribers, result.subscribers));
        break;

      case TYPE_DESCRIPTION:
        TextView desc = (TextView) v;
        desc.setText(result.description);
        desc.setMovementMethod(LinkMovementMethod.getInstance());
        break;

      default:
        throw new IllegalArgumentException();
    }
  }
}
