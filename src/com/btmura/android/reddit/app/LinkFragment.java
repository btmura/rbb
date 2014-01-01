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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.util.Strings;

public class LinkFragment extends Fragment implements OnLongClickListener {

    public static final String TAG = "LinkFragment";

    private static final String ARG_URL = "url";

    private WebView webView;
    private ProgressBar progress;

    public static LinkFragment newInstance(CharSequence url) {
        Bundle b = new Bundle(1);
        b.putCharSequence(ARG_URL, url);
        LinkFragment frag = new LinkFragment();
        frag.setArguments(b);
        return frag;
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
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setPluginState(PluginState.ON_DEMAND);
        settings.setUseWideViewPort(true);

        webView.setOnLongClickListener(this);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (progress != null) {
                    progress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progress != null) {
                    progress.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progress != null) {
                    progress.setProgress(newProgress);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(getUrl());
        }
    }

    private String getUrl() {
        return Strings.toString(getArguments().getCharSequence(ARG_URL));
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onDetach() {
        webView.destroy();
        webView = null;
        progress = null;
        super.onDetach();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onLongClick(View v) {
        HitTestResult hit = webView.getHitTestResult();
        if (!TextUtils.isEmpty(hit.getExtra())) {
            switch (hit.getType()) {
                case HitTestResult.IMAGE_TYPE:
                case HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                    handleHit(hit,
                            R.array.link_image_menu_items,
                            new ImageClickListener(hit.getExtra()));
                    return true;

                case HitTestResult.ANCHOR_TYPE:
                case HitTestResult.SRC_ANCHOR_TYPE:
                    handleHit(hit,
                            R.array.link_anchor_menu_items,
                            new AnchorClickListener(hit.getExtra()));
                    return true;

                default:
                    return false;
            }
        }
        return false;
    }

    private void handleHit(HitTestResult hit, int arrayResId, OnClickListener listener) {
        new AlertDialog.Builder(getActivity())
                .setTitle(hit.getExtra())
                .setItems(arrayResId, listener)
                .show();
    }

    class ImageClickListener implements OnClickListener {

        // The following constants need to match the R.array.link_image_menu_items array.

        private static final int ITEM_OPEN = 0;
        private static final int ITEM_SAVE = 1;
        private static final int ITEM_SHARE = 2;
        private static final int ITEM_COPY_URL = 3;

        private final String url;

        ImageClickListener(String url) {
            this.url = url;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case ITEM_OPEN:
                    MenuHelper.openUrl(getActivity(), url);
                    break;

                case ITEM_SAVE:
                    MenuHelper.downloadUrl(getActivity(), url, url);
                    break;

                case ITEM_SHARE:
                    MenuHelper.shareImageUrl(getActivity(), url);
                    break;

                case ITEM_COPY_URL:
                    MenuHelper.copyUrl(getActivity(), url, url);
                    break;

                default:
                    break;
            }
        }
    }

    class AnchorClickListener implements OnClickListener {

        // The following constants need to match the R.array.link_image_menu_items array.

        private static final int ITEM_OPEN = 0;
        private static final int ITEM_SHARE = 1;
        private static final int ITEM_COPY_URL = 2;

        private final String url;

        AnchorClickListener(String url) {
            this.url = url;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case ITEM_OPEN:
                    MenuHelper.openUrl(getActivity(), url);
                    break;

                case ITEM_SHARE:
                    MenuHelper.shareImageUrl(getActivity(), url);
                    break;

                case ITEM_COPY_URL:
                    MenuHelper.copyUrl(getActivity(), url, url);
                    break;

                default:
                    break;
            }
        }
    }
}
