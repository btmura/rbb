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

import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.view.MenuHelper;

public class LinkFragment extends Fragment {

    public static final String TAG = "LinkFragment";

    private static final String ARG_TITLE = "title";
    private static final String ARG_URL = "url";

    private static final String STATE_URL = "url";

    private static final Pattern PATTERN_IMAGE = Pattern.compile(".*\\.(jpg|png|gif)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private OnThingEventListener listener;
    private WebView webView;
    private ProgressBar progress;

    public static LinkFragment newInstance(String title, String url) {
        Bundle b = new Bundle(2);
        b.putString(ARG_TITLE, title);
        b.putString(ARG_URL, url);
        LinkFragment frag = new LinkFragment();
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingEventListener) {
            listener = (OnThingEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.link, container, false);
        webView = (WebView) view.findViewById(R.id.link);
        progress = (ProgressBar) view.findViewById(R.id.progress);
        setupWebView(webView);
        return view;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setPluginState(PluginState.ON_DEMAND);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progress.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progress.setProgress(newProgress);
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String url;
        if (savedInstanceState != null) {
            url = savedInstanceState.getString(STATE_URL);
        } else {
            url = getArguments().getString(ARG_URL);
        }
        if (PATTERN_IMAGE.matcher(url).matches()) {
            String img = String.format("<img src=\"%s\" width=\"100%%\" />", url);
            webView.loadData(img, "text/html", null);
        } else {
            webView.loadUrl(url);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.link_menu, menu);
        MenuHelper.setShareProvider(menu.findItem(R.id.menu_link_share),
                getArguments().getString(ARG_TITLE),
                getArguments().getString(ARG_URL));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_comments:
                return handleComments();

            case R.id.menu_link_open:
                return handleOpen();

            case R.id.menu_link_copy_url:
                return handleCopyUrl();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean handleComments() {
        if (listener != null) {
            listener.onCommentMenuItemClick();
        }
        return true;
    }

    private boolean handleOpen() {
        MenuHelper.startIntentChooser(getActivity(), getArguments().getString(ARG_URL));
        return true;
    }

    private boolean handleCopyUrl() {
        MenuHelper.setClipAndToast(getActivity(), getArguments().getString(ARG_TITLE),
                getArguments().getString(ARG_URL));
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_URL, webView.getUrl());
    }

    @Override
    public void onDetach() {
        webView.destroy();
        webView = null;
        progress = null;
        super.onDetach();
    }
}
