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

	static final int VIEW_LINK = 0;
	static final int VIEW_COMMENTS = 1;
	
	private static final String STATE_DISPLAYED_VIEW = "displayedView";
	
	private ThingHolder thingHolder;

	private WebView linkView;
	private ListView commentsView;
	
	private int[] viewProgress = new int[2];
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
				showProgress(VIEW_LINK, View.VISIBLE);
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				showProgress(VIEW_LINK, View.GONE);
			}
		});
	}
	
	void showProgress(int view, int visibility) {
		viewProgress[view] = visibility;
		if (isShowing(view)) {
			progress.setVisibility(visibility);
		}
	}
	
	void refreshProgress(int view) {
		progress.setVisibility(viewProgress[view]);
	}
	
	private boolean isShowing(int view) {
		switch (view) {
		case VIEW_LINK:
			return linkView.getVisibility() == View.VISIBLE;
		case VIEW_COMMENTS:
			return commentsView.getVisibility() == View.VISIBLE;
		}
		return false;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		
		Thing thing = thingHolder.getThing();

		adapter = new CommentAdapter(getActivity());
		commentsView.setAdapter(adapter);
		
		task = new CommentLoaderTask(this, adapter);
		task.execute(thing);		
		linkView.loadUrl(thing.url);

		restoreDisplayedView(savedInstanceState, thing);
	}
	
	private void restoreDisplayedView(Bundle savedInstanceState, Thing thing) {
		int displayedView;
		if (savedInstanceState != null) {
			displayedView = savedInstanceState.getInt(STATE_DISPLAYED_VIEW);
		} else {
			displayedView = thing.isSelf ? VIEW_COMMENTS : VIEW_LINK;
		}
		setDisplayedView(displayedView);	
	}
	
	private void setDisplayedView(int view) {
		linkView.setVisibility(view == VIEW_LINK ? View.VISIBLE : View.GONE);
		commentsView.setVisibility(view == VIEW_COMMENTS ? View.VISIBLE : View.GONE);
		refreshProgress(view);
		getActivity().invalidateOptionsMenu();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_DISPLAYED_VIEW, 
				linkView.getVisibility() == View.VISIBLE ? VIEW_LINK : VIEW_COMMENTS);
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
		setDisplayedView(VIEW_LINK);
	}
	
	private void handleCommentsItem() {
		setDisplayedView(VIEW_COMMENTS);
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
