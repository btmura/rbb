package com.btmura.android.reddit;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class LinkFragment extends Fragment {
	
	private ThingHolder thingHolder;

	private WebView webView;
	private ProgressBar progress;
	
	public static LinkFragment newInstance() {
		return new LinkFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		thingHolder = (ThingHolder) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.link_fragment, container, false);
		webView = (WebView) view.findViewById(R.id.link);
		progress = (ProgressBar) view.findViewById(R.id.progress);
		setupWebView(webView);
		return view;
	}
	
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
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		webView.loadUrl(thingHolder.getThing().url);
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
	public void onDestroy() {
		super.onDestroy();
		webView.destroy();
	}
}
