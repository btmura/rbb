package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewFragment;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class ThingWebFragment extends WebViewFragment {

	private static final String TAG = "ThingWebFragment";

	private ThingHolder thingHolder;
	
	private LoadUrlTask task;
	
	@Override
	public void onAttach(Activity activity) {
		Log.v(TAG, "onAttach");
		super.onAttach(activity);
		thingHolder = (ThingHolder) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		WebView view = (WebView) super.onCreateView(inflater, container, savedInstanceState);
		view.setWebViewClient(new ThingWebClient());
		view.getSettings().setBuiltInZoomControls(true);
		view.getSettings().setDisplayZoomControls(false);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setLoadWithOverviewMode(true);
		view.getSettings().setSupportZoom(true);
		view.getSettings().setUseWideViewPort(true);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		task = new LoadUrlTask();
		task.execute();	
	}
	
	class LoadUrlTask extends AsyncTask<Void, Void, String> implements JsonParseListener {
		
		private String url;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
		
		@Override
		protected String doInBackground(Void... voidRay) {
			try {
				Thing thing = thingHolder.getThing();
				if (thing == null) {
					return null;
				}
				
				URL url = new URL("http://www.reddit.com/by_id/" + thing.name + ".json");
				Log.v(TAG, url.toString());
				
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				
				InputStream stream = connection.getInputStream();
				try {
					JsonReader reader = new JsonReader(new InputStreamReader(stream));
					new JsonParser(this).parse(reader);
				} finally {
					stream.close();
				}
			} catch (MalformedURLException e) {
				Log.e(TAG, "", e);
			} catch (IOException e) {
				Log.e(TAG, "", e);
			}
			return url;
		}
		
		public void onUrl(String url) {
			this.url = url;
		}
		
		@Override
		protected void onPostExecute(String url) {
			super.onPostExecute(url);
			if (url == null) {
				Log.v(TAG, "Url is null");
				return;
			}
			
			WebView webView = getWebView();
			if (webView == null) {
				Log.v(TAG, "WebView is null");
				return;
			}
			
			webView.loadUrl(url);
		}
	}

	@Override
	public void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}

	class ThingWebClient extends WebViewClient {
	}
}
