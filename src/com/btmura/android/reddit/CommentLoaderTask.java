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
import android.view.View;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class CommentLoaderTask extends AsyncTask<Thing, Comment, Boolean> implements JsonParseListener {
	
	private static final String TAG = "CommentLoaderTask";
	
	private final ThingFragment frag;
	private final CommentAdapter adapter;
	
	private int nesting;
	
	public CommentLoaderTask(ThingFragment frag, CommentAdapter adapter) {
		this.frag = frag;
		this.adapter = adapter;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		frag.showProgress(ThingFragment.VIEW_COMMENTS, View.VISIBLE);
		adapter.clear();
	}
	
	@Override
	protected void onProgressUpdate(Comment... comments) {
		super.onProgressUpdate(comments);
		adapter.addAll(comments);
		frag.showProgress(ThingFragment.VIEW_COMMENTS, View.GONE);
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
			new JsonParser(this).withReplies(true).parseListingArray(reader);
			stream.close();
			
			return true;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return false;
	}

	public void onDataStart(int nesting) {
		this.nesting = nesting;
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
		if (!text.trim().isEmpty()) {
			publishProgress(new Comment(Html.fromHtml(text).toString(), nesting));
		}
	}

	public void onBody(String body) {
		publishProgress(new Comment(Html.fromHtml(body).toString(), nesting));
	}

	public void onDataEnd() {	
	}
}
