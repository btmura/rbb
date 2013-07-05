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

import java.util.List;

import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubePlayerFragment extends Fragment implements OnInitializedListener {

    private static final String TAG = "YouTubePlayerFragment";

    private static final String ARG_URL = "url";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_YOUTUBE = 1;
    private static final int MATCH_YOUTU_BE = 2;

    static {
        MATCHER.addURI("youtube.com", "watch", MATCH_YOUTUBE);
        MATCHER.addURI("www.youtube.com", "watch", MATCH_YOUTUBE);
        MATCHER.addURI("youtu.be", "*", MATCH_YOUTU_BE);
        MATCHER.addURI("www.youtu.be", "*", MATCH_YOUTU_BE);
    }

    public static boolean isPlayableWithYouTube(Context context, String url) {
        return isYouTubeAvailable(context) && isYouTubeVideoUrl(url);
    }

    private static boolean isYouTubeAvailable(Context context) {
        return YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(context)
                == YouTubeInitializationResult.SUCCESS;
    }

    static boolean isYouTubeVideoUrl(String url) {
        return !TextUtils.isEmpty(getVideoId(url));
    }

    static String getVideoId(String url) {
        Uri uri = Uri.parse(url);
        switch (MATCHER.match(uri)) {
            case MATCH_YOUTUBE:
                return uri.getQueryParameter("v");

            case MATCH_YOUTU_BE:
                List<String> segments = uri.getPathSegments();
                return segments.size() == 1 ? segments.get(0) : null;

            default:
                return null;
        }
    }

    public static YouTubePlayerFragment newInstance(String url) {
        Bundle args = new Bundle(1);
        args.putString(ARG_URL, url);

        YouTubePlayerFragment frag = new YouTubePlayerFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.youtube, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        YouTubePlayerSupportFragment frag = YouTubePlayerSupportFragment.newInstance();
        frag.initialize(getString(R.string.key_youtube), this);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.youtube_fragment, frag);
        ft.commit();
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player,
            boolean wasRestored) {
        if (!wasRestored) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "url: " + getUrl() + " videoId: " + getVideoId(getUrl()));
            }
            player.loadVideo(getVideoId(getUrl()));
        }
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult result) {
    }

    private String getUrl() {
        return getArguments().getString(ARG_URL);
    }
}
