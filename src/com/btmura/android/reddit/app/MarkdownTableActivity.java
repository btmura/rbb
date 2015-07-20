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

package com.btmura.android.reddit.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;

public class MarkdownTableActivity extends FragmentActivity {

  public static final String EXTRA_TABLE_DATA = "tableData";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setTheme(ThemePrefs.getDialogWhenLargeTheme(this));
    setContentView(R.layout.markdown_table);
    setFragments(savedInstanceState);
  }

  private void setFragments(Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.replace(android.R.id.content,
          MarkdownTableFragment.newInstance(getTableData()));
      ft.commit();
    }
  }

  private String getTableData() {
    return getIntent().getStringExtra(EXTRA_TABLE_DATA);
  }
}
