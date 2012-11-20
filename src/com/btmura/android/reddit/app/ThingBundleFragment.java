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

package com.btmura.android.reddit.app;

import java.io.IOException;

import android.app.Activity;
import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.btmura.android.reddit.net.RedditApi;

/**
 * {@link ThingBundleFragment} returns information about a thing specified by
 * thing ID via a {@link OnThingBundleLoadedListener}. It extends
 * {@link ListFragment} to show a loading symbol which does not go away.
 */
public class ThingBundleFragment extends ListFragment {

    public static final String TAG = "ThingBundleFragment";

    private static final String ARG_THING_ID = "thingId";

    private OnThingBundleLoadedListener listener;
    private LoadTask loadTask;

    public interface OnThingBundleLoadedListener {
        void onThingBundleLoaded(Bundle thingBundle);
    }

    public static ThingBundleFragment newInstance(String thingId) {
        Bundle args = new Bundle(1);
        args.putString(ARG_THING_ID, thingId);

        ThingBundleFragment frag = new ThingBundleFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingBundleLoadedListener) {
            listener = (OnThingBundleLoadedListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (loadTask == null) {
            loadTask = new LoadTask();
            loadTask.execute();
        }
    }

    @Override
    public void onDestroy() {
        if (loadTask != null) {
            loadTask.cancel(true);
            loadTask = null;
        }
        super.onDestroy();
    }

    class LoadTask extends AsyncTask<Void, Void, Bundle> {

        @Override
        protected Bundle doInBackground(Void... voidRay) {
            SystemClock.sleep(10000);
            String thingId = getArguments().getString(ARG_THING_ID);
            try {
                return RedditApi.getThingBundle(thingId, null);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bundle result) {
            if (listener != null) {
                listener.onThingBundleLoaded(result);
            }
        }
    }

}
