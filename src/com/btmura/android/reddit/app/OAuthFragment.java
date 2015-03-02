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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.net.Urls;

public class OAuthFragment extends Fragment {

    public static final String TAG = "OAuth2Fragment";

    private static final String ARG_LOGIN = "login";

    private WebView webView;

    public static OAuthFragment newInstance(String login) {
        Bundle args = new Bundle(1);
        args.putString(ARG_LOGIN, login);

        OAuthFragment frag = new OAuthFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.oauth_frag, container, false);
        webView = (WebView) v.findViewById(R.id.webview);
        setupWebView(webView);
        return v;
    }

    private void setupWebView(WebView wv) {
        wv.setWebViewClient(new WebViewClient() {});
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            // Remove cookie so user can enter account name.
            CookieManager.getInstance().removeSessionCookie();
            webView.loadUrl(getUrl());
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private String getUrl() {
        StringBuilder state = new StringBuilder("rbb_")
                .append(System.currentTimeMillis());

        return Urls.authorize(
                getString(R.string.key_reddit_client_id),
                state,
                Urls.OAUTH_REDIRECT_URL).toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();;
    }

    @Override
    public void onPause() {
        webView.onPause();;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onDetach() {
        webView.destroy();
        webView = null;
        super.onDetach();
    }
}
