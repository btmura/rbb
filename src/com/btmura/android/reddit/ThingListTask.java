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

	private final TaskListener<Thing, Boolean> listener;
	
	private String id;
	private String title;
	private String url;
	private boolean isSelf;
	
	public ThingListTask(TaskListener<Thing, Boolean> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onProgressUpdate(Thing... things) {
		listener.onProgressUpdate(things);	
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		listener.onPostExecute(result);
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
	
	public void onDataStart(int nesting) {
	}
	
	public void onSelfText(String text) {
	}
	
	public void onBody(String body) {
	}
}
