/*
 * Copyright (C) 2015 Brian Muramatsu
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

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.net.AccessTokenResult;
import com.btmura.android.reddit.net.Urls;

import java.io.IOException;

public class AddAccountFragment extends Fragment {

    private static final String TAG = "AddAccountFragment";

    private static final String ARG_URL = "url";

    public static AddAccountFragment newInstance(String url) {
        Bundle args = new Bundle(1);
        args.putString(ARG_URL, url);

        AddAccountFragment frag = new AddAccountFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = getUrlArgument();
        if (!TextUtils.isEmpty(url)) {
            Uri uri = Uri.parse(url);
            addText(uri.toString());

            CharSequence clientId = getString(R.string.key_reddit_client_id);
            String code = uri.getQueryParameter("code");

            try {
                AccessTokenResult result = AccessTokenResult.getAccessToken(clientId, code, Urls.OAUTH_REDIRECT_URL);
                addText("at: " + result.accessToken);
                addText("tt: " + result.tokenType);
                addText("ei: " + result.expiresIn);
                addText("sc: " + result.scope);
                addText("rt: " + result.refreshToken);
            } catch (IOException e) {
                addText(e.getMessage());
            }
        }
    }

    private void addText(CharSequence text) {
        Log.d(TAG, text.toString());
    }

    private String getUrlArgument() {
        return getArguments().getString(ARG_URL);
    }
}
