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
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;

public class ThingFragment extends Fragment {

    static final String TAG = "ThingFragment";

    private static final String ARG_ACCOUNT_NAME = "an";
    private static final String ARG_THING_BUNDLE = "tb";

    private Bundle thingBundle;
    private ThingPagerAdapter adapter;
    private ViewPager thingPager;

    public static ThingFragment newInstance(String accountName, Bundle thingBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putBundle(ARG_THING_BUNDLE, thingBundle);

        ThingFragment frag = new ThingFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thingBundle = getArguments().getBundle(ARG_THING_BUNDLE);
        adapter = new ThingPagerAdapter(getChildFragmentManager(), getAccountName(), thingBundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.thing_frag, container, false);
        thingPager = (ViewPager) view.findViewById(R.id.thing_pager);
        thingPager.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private String getAccountName() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }
}
