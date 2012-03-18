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

package com.btmura.android.reddit.browser;

import android.app.Fragment;
import android.os.Bundle;

public class ControlFragment extends Fragment {

    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_THING = "thing";
    private static final String ARG_THING_POSITION = "thingPosition";
    private static final String ARG_FILTER = "filter";

    private Subreddit topic;
    private Thing thing;
    private int thingPosition;
    private int filter;

    public static ControlFragment newInstance(Subreddit sr, Thing thing, int thingPosition,
            int filter) {
        ControlFragment frag = new ControlFragment();
        Bundle b = new Bundle(4);
        b.putParcelable(ARG_SUBREDDIT, sr);
        b.putParcelable(ARG_THING, thing);
        b.putInt(ARG_THING_POSITION, thingPosition);
        b.putInt(ARG_FILTER, filter);
        frag.setArguments(b);
        return frag;
    }

    public Subreddit getTopic() {
        return topic;
    }

    public Thing getThing() {
        return thing;
    }

    public int getThingPosition() {
        return thingPosition;
    }

    public int getFilter() {
        return filter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        topic = getArguments().getParcelable(ARG_SUBREDDIT);
        thing = getArguments().getParcelable(ARG_THING);
        thingPosition = getArguments().getInt(ARG_THING_POSITION);
        filter = getArguments().getInt(ARG_FILTER);
    }
}
