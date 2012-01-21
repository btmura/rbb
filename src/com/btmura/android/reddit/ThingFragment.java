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

	private ThingHolder thingHolder;

	private ProgressBar progress;
	private WebView webView;
	private MenuItem copyItem;

	private LoadUrlTask task;
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

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		webView.onResume();
	}

	@Override
	public void onStart() {
		super.onStart();
		task = new LoadUrlTask();
		task.execute();
	}

	@Override
	public void onPause() {
		super.onPause();
		webView.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}

	private void setupWebView(WebView webView) {
		webView.setWebViewClient(new ThingWebClient());
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setLoadWithOverviewMode(true);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setUseWideViewPort(true);
	}

	class ThingWebClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			progress.setVisibility(View.GONE);
		}
	}

	class LoadUrlTask extends AsyncTask<Void, Void, Void> implements
			JsonParseListener {
		
		private String loadedUrl;

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
			loadedUrl = url;
		}

		@Override
		protected void onPostExecute(Void intoTheVoid) {
			super.onPostExecute(intoTheVoid);
			if (loadedUrl == null || loadedUrl.isEmpty()) {
				Log.v(TAG, "Url is null");
				return;
			}
			url = loadedUrl;
			webView.loadUrl(url);
			copyItem.setEnabled(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.thing_fragment, menu);
		copyItem = menu.findItem(R.id.menu_copy_link);
		copyItem.setEnabled(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.v(TAG, "onOptionsItemSelected!");
		switch (item.getItemId()) {
		case R.id.menu_copy_link:
			handleCopyLink();
			break;
		}
		return true;
	}

	private void handleCopyLink() {
		Log.v(TAG, url);
		ClipboardManager clip = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		clip.setText(url);
		Toast.makeText(getActivity(), url, Toast.LENGTH_SHORT).show();
	}
}
