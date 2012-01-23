package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class CommentLoaderTask extends AsyncTask<Thing, String, Boolean> implements JsonParseListener {
	
	private static final String TAG = "CommentLoaderTask";
	
	private final CommentAdapter adapter;
	
	public CommentLoaderTask(CommentAdapter adapter) {
		this.adapter = adapter;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		adapter.clear();
	}
	
	@Override
	protected void onProgressUpdate(String... comments) {
		super.onProgressUpdate(comments);
		adapter.addAll(comments);
	}

	@Override
	protected Boolean doInBackground(Thing... threads) {
		try {
			URL commentsUrl = new URL("http://www.reddit.com/comments/" + threads[0].getId() + ".json");
			Log.v(TAG, commentsUrl.toString());
			
			HttpURLConnection connection = (HttpURLConnection) commentsUrl.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			new JsonParser(this).parseArray(reader);
			stream.close();
			
			return true;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return false;
	}

	public void onDataStart() {
	}

	public void onId(String id) {
	}

	public void onTitle(String title) {
	}

	public void onUrl(String url) {
	}

	public void onIsSelf(boolean isSelf) {
	}

	public void onSelfText(String text) {	
	}

	public void onBody(String body) {
		publishProgress(body);
	}

	public void onDataEnd() {	
	}
}
