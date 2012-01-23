package com.btmura.android.reddit;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ThingFragment extends Fragment {

	private ThingHolder thingHolder;

	private WebView linkView;
	private ListView commentsView;
	private ProgressBar progress;
	
	private CommentAdapter adapter;
	private CommentLoaderTask task;

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
		linkView = (WebView) view.findViewById(R.id.link);
		commentsView = (ListView) view.findViewById(R.id.comments);
		setupWebView(linkView);
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
		setHasOptionsMenu(true);
		
		Thing thing = thingHolder.getThing();

		adapter = new CommentAdapter(getActivity());
		commentsView.setAdapter(adapter);
		
		task = new CommentLoaderTask(adapter);
		task.execute(thing);
		
		linkView.loadUrl(thing.url);
		switchViews(!thing.isSelf);
	}
	
	private void switchViews(boolean showLink) {
		linkView.setVisibility(showLink ? View.VISIBLE : View.GONE);
		commentsView.setVisibility(showLink ? View.GONE : View.VISIBLE);
	}

	@Override
	public void onResume() {
		super.onResume();
		linkView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		linkView.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (task != null) {
			task.cancel(true);
			task = null;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.thing_fragment, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_link).setVisible(commentsView.getVisibility() == View.VISIBLE);
		menu.findItem(R.id.menu_comments).setVisible(linkView.getVisibility() == View.VISIBLE);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case R.id.menu_link:
			handleLinkItem();
			return true;
			
		case R.id.menu_comments:
			handleCommentsItem();
			return true;
			
		case R.id.menu_copy_link:
			handleCopyLinkItem();
			return true;
		
		case R.id.menu_view:
			handleViewItem();
			return true;
		}
		return false;
	}
	
	private void handleLinkItem() {
		switchViews(true);
		getActivity().invalidateOptionsMenu();
	}
	
	private void handleCommentsItem() {
		switchViews(false);
		getActivity().invalidateOptionsMenu();
	}

	private void handleCopyLinkItem() {
		Thing thing = thingHolder.getThing();
		ClipboardManager clip = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		clip.setText(thing.url);
		Toast.makeText(getActivity(), thing.url, Toast.LENGTH_SHORT).show();	
	}
	
	private void handleViewItem() {
		Thing thing = thingHolder.getThing();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(thing.url));
		startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));	
	}
}
