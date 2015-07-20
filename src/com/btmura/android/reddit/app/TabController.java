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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

class TabController {

  private static final String STATE_SELECTED_TAB_INDEX = "selectedTabIndex";

  private final ActionBar bar;
  private final List<Tab> tabs = new ArrayList<Tab>(3);
  private int selectedTabIndex;
  private boolean tabListenerDisabled;

  public TabController(ActionBar bar, Bundle savedInstanceState) {
    this.bar = bar;
    if (savedInstanceState != null) {
      this.selectedTabIndex = savedInstanceState.getInt(
          STATE_SELECTED_TAB_INDEX);
    }
  }

  public Tab addTab(Tab tab) {
    tabs.add(tab);
    return tab;
  }

  public void setupTabs() {
    if (bar.getNavigationMode() != ActionBar.NAVIGATION_MODE_TABS) {
      int count = tabs.size();
      for (int i = 0; i < count; i++) {
        bar.addTab(tabs.get(i));
      }

      tabListenerDisabled = selectedTabIndex != 0;
      bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      tabListenerDisabled = false;
      if (selectedTabIndex != 0) {
        bar.setSelectedNavigationItem(selectedTabIndex);
      }
    }
  }

  public boolean selectTab(Tab tab) {
    if (!tabListenerDisabled) {
      selectedTabIndex = tab.getPosition();
      return true;
    }
    return false;
  }

  public boolean isTabSelected(Tab tab) {
    return tabs.get(selectedTabIndex) == tab;
  }

  public void saveInstanceState(Bundle outState) {
    outState.putInt(STATE_SELECTED_TAB_INDEX, selectedTabIndex);
  }
}
