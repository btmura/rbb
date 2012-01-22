package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class ThingFragment extends Fragment {

	private static final String TAG = "ThingFragment";
	
	private static final String STATE_URL = "url";

	private ThingHolder thingHolder;

	private ProgressBar progress;
	private WebView webView;

	private ResolveUrlTask task;
	private String url;

	public static ThingFragment newInstance() {
		return new ThingFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		thingHolder = (ThingHolder) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.thing_fragment, container, false);
		progress = (ProgressBar) view.findViewById(R.id.progress);
		webView = (WebView) view.findViewById(R.id.webview);
		setupWebView(webView);
		return view;
	}
	
	private void setupWebView(WebView webView) {
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setLoadWithOverviewMode(true);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setUseWideViewPort(true);		
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
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			url = savedInstanceState.getString(STATE_URL);
		}
	}

	@Override
	public void onStart() {
		Log.v(TAG, "onStart");
		super.onStart();
		if (url == null) {
			Log.v(TAG, "Resolving url...");
			task = new ResolveUrlTask();
			task.execute();
		} else {
			loadUrl();
		}
	}
	
	class ResolveUrlTask extends AsyncTask<Void, Void, Void> implements JsonParseListener {
		
		private String resolvedUrl;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Void doInBackground(Void... voidRay) {
			try {
				Thing thing = thingHolder.getThing();
				if (thing == null) {
					return null;
				}
		
				URL url = new URL("http://www.reddit.com/by_id/" + thing.name
						+ ".json");
				Log.v(TAG, url.toString());
		
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				connection.connect();
		
				InputStream stream = connection.getInputStream();
				try {
					JsonReader reader = new JsonReader(new InputStreamReader(
							stream));
					new JsonParser(this).parse(reader);
				} finally {
					stream.close();
				}
			} catch (MalformedURLException e) {
				Log.e(TAG, "", e);
			} catch (IOException e) {
				Log.e(TAG, "", e);
			}
			return null;
		}
		
		public void onUrl(String url) {
			resolvedUrl = url;
		}
		
		@Override
		protected void onPostExecute(Void intoTheVoid) {
			super.onPostExecute(intoTheVoid);
			if (resolvedUrl == null || resolvedUrl.isEmpty()) {
				Log.v(TAG, "Url is null");
				return;
			}
			url = resolvedUrl;
			loadUrl();
		}
	}

	private void loadUrl() {
		webView.loadUrl(url);
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
	public void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_URL, url);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.thing_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case R.id.menu_copy_link:
			handleCopyLinkItem();
			return true;
		
		case R.id.menu_view:
			handleViewItem();
			return true;
		}
		return false;
	}

	private void handleCopyLinkItem() {
		if (url != null) {
			ClipboardManager clip = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clip.setText(url);
			Toast.makeText(getActivity(), url, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void handleViewItem() {
		if (url != null) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));
		}
	}
}
