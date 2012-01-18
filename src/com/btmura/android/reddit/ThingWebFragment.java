package com.btmura.android.reddit;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewFragment;

public class ThingWebFragment extends WebViewFragment implements ThingFragment {

	private static final String TAG = "ThingWebFragment";

	private static final String STATE_THING = "thing";
	private static final String STATE_THING_POSITION = "thingPosition";
	private static final String STATE_URL = "url";

	private Thing thing;
	private int thingPosition;
	private String url;
	
	public void setThing(Thing thing, int position) {
		this.thing = thing;
		this.thingPosition = position;
	}
	
	public Thing getThing() {
		return thing;
	}
	
	public int getThingPosition() {
		return thingPosition;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			thing = savedInstanceState.getParcelable(STATE_THING);
			thingPosition = savedInstanceState.getInt(STATE_THING_POSITION);
			url = savedInstanceState.getString(STATE_URL);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_THING, thing);
		outState.putInt(STATE_THING_POSITION, thingPosition);
		outState.putString(STATE_URL, url);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		WebView view = (WebView) super.onCreateView(inflater, container, savedInstanceState);
		view.setWebViewClient(new ThingWebClient());
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setSupportZoom(true);
		view.getSettings().setDisplayZoomControls(false);
		view.getSettings().setLoadWithOverviewMode(true);
		view.getSettings().setUseWideViewPort(true);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.v(TAG, "Loading url: " + url);
		getWebView().loadUrl(url);
	}
	
	class ThingWebClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
}
