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

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.net.Urls;

/**
 * Fragment that presents a Reddit login screen inside a WebView.
 */
public class LoginFragment extends Fragment {

  private static final String ARG_EXPECTED_STATE_TOKEN = "expectedStateToken";

  private OnLoginListener listener;
  private WebView webView;
  private ProgressBar progressBar;

  public interface OnLoginListener {
    void onLoginSuccess(String code);

    void onLoginCancelled();
  }

  public static LoginFragment newInstance(CharSequence expectedStateToken) {
    Bundle args = new Bundle(1);
    args.putCharSequence(ARG_EXPECTED_STATE_TOKEN, expectedStateToken);
    LoginFragment frag = new LoginFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnLoginListener) {
      listener = (OnLoginListener) activity;
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.login_fragment, container, false);
    webView = (WebView) v.findViewById(R.id.web_view);
    progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
    setupWebView();
    return v;
  }

  private void setupWebView() {
    // Clear reddit cookie to present a fresh login form.
    if (!BuildConfig.DEBUG) {
      CookieManager.getInstance().setCookie(".reddit.com", "reddit_session=");
    }

    WebSettings settings = webView.getSettings();
    settings.setBuiltInZoomControls(true);
    settings.setDisplayZoomControls(false);
    settings.setLoadWithOverviewMode(true);
    settings.setSupportZoom(true);
    settings.setUseWideViewPort(true);

    // Don't save usernames entered into the login forms.
    settings.setSaveFormData(false);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (progressBar != null) {
          progressBar.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        if (progressBar != null) {
          progressBar.setVisibility(View.GONE);
        }
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // WebView will show an unrecognized scheme error unless we intercept
        // the OAuth callback URL and fire off an intent instead.
        return url != null
            && url.startsWith(Urls.OAUTH_REDIRECT_URL)
            && handleOAuthRedirectUrl(url);
      }
    });

    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int newProgress) {
        if (progressBar != null) {
          progressBar.setProgress(newProgress);
        }
      }
    });
  }

  boolean handleOAuthRedirectUrl(String url) {
    Uri uri = Uri.parse(url);

    String error = uri.getQueryParameter("error");
    if (!TextUtils.isEmpty(error)) {
      if (listener != null) {
        listener.onLoginCancelled();
      }
      return true;
    }

    String state = uri.getQueryParameter("state");
    String code = uri.getQueryParameter("code");
    if (TextUtils.equals(state, getExpectedStateToken())
        && !TextUtils.isEmpty(code)) {
      if (listener != null) {
        listener.onLoginSuccess(code);
      }
      return true;
    }

    return false;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getActivity().setTitle(R.string.login_fragment_title);
    if (savedInstanceState == null) {
      loadLoginUrl();
    } else {
      webView.restoreState(savedInstanceState);
    }
  }

  private void loadLoginUrl() {
    CharSequence url = Urls.authorize(getActivity(), getExpectedStateToken());
    webView.loadUrl(url.toString());
  }

  @Override
  public void onResume() {
    super.onResume();
    webView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    webView.onPause();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    webView.saveState(outState);
  }

  @Override
  public void onDetach() {
    webView.destroy();
    super.onDetach();
  }

  private CharSequence getExpectedStateToken() {
    return getArguments().getCharSequence(ARG_EXPECTED_STATE_TOKEN);
  }
}
