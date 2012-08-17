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

package com.btmura.android.reddit.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.provider.SubredditProvider;

public class GeneralSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // TODO: Figure out to specify extras in the XML.
        Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {SubredditProvider.AUTHORITY});

        Preference pref = new Preference(getActivity());
        pref.setTitle(R.string.settings_sync_settings);
        pref.setIntent(intent);

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        prefScreen.addPreference(pref);
        setPreferenceScreen(prefScreen);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // TODO: Figure out how to move this somehow to SettingsActivity.
        menu.findItem(R.id.menu_add_account).setVisible(false);
    }
}
