package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class ThingListTask extends AsyncTask<Topic, Thing, Boolean> implements JsonParseListener {
	
	private static final String TAG = "ThreadListTask";

	private final ThingListFragment frag;
	private final ThingListAdapter adapter; 
	
	private String id;
	private String title;
	private String url;
	private boolean isSelf;
	
	public ThingListTask(ThingListFragment frag, ThingListAdapter adapter) {
		this.frag = frag;
		this.adapter = adapter;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (frag.isVisible()) {
			frag.setListShown(false);
		}
		adapter.clear();
	}

	@Override
	protected void onProgressUpdate(Thing... things) {
		super.onProgressUpdate(things);
		adapter.addAll(things);
		if (frag.isVisible()) {
			frag.setListShown(true);
		}
	}

	@Override
	protected Boolean doInBackground(Topic... topics) {
		try {
			URL url = new URL(topics[0].getUrl());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			new JsonParser(this).parseListing(reader);
			stream.close();
			
			connection.disconnect();
			
			return true;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return false;
	}

	public void onId(String id) {
		this.id = id;
	}
	
	public void onTitle(String title) {
		this.title = title;
	}
	
	public void onUrl(String url) {	
		this.url = url;
	}
	
	public void onIsSelf(boolean isSelf) {
		this.isSelf = isSelf;
	}
	
	public void onDataEnd() {	
		publishProgress(new Thing(id, Html.fromHtml(title).toString(), url, isSelf));
	}
	
	public void onDataStart() {
	}
	
	public void onSelfText(String text) {
	}
	
	public void onBody(String body) {
	}
}
