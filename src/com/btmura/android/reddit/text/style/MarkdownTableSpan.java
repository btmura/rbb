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

package com.btmura.android.reddit.text.style;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

import com.btmura.android.reddit.app.MarkdownTableActivity;
import com.btmura.android.reddit.content.Contexts;

public class MarkdownTableSpan extends ClickableSpan {

  private final String tableData;

  public MarkdownTableSpan(String tableData) {
    this.tableData = tableData;
  }

  @Override
  public void onClick(View widget) {
    Context ctx = widget.getContext();
    Intent intent = new Intent(ctx, MarkdownTableActivity.class);
    intent.putExtra(MarkdownTableActivity.EXTRA_TABLE_DATA, tableData);
    Contexts.startActivity(widget.getContext(), intent);
  }
}
