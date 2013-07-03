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

import java.util.regex.Pattern;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubePlayerFragment extends Fragment implements OnInitializedListener {

    private static final String ARG_URL = "url";

    private static final Pattern PATTERN_FIND_URL =
            Pattern.compile("(youtube|youtu\\.be)", Pattern.CASE_INSENSITIVE);

    private String videoId;

    public static boolean isPlayableWithYouTube(Context context, String url) {
        return isYouTubeAvailable(context) && PATTERN_FIND_URL.matcher(url).find();
    }

    private static boolean isYouTubeAvailable(Context context) {
        return YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(context)
                == YouTubeInitializationResult.SUCCESS;
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

        videoId = Uri.parse(getUrl()).getQueryParameter("v");

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
            player.loadVideo(videoId);
        }
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult result) {

    }

    private String getUrl() {
        return getArguments().getString(ARG_URL);
    }
}
