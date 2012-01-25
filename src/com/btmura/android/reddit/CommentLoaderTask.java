package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import com.btmura.android.reddit.JsonParser.JsonParseListener;
import com.google.gson.stream.JsonReader;

public class CommentLoaderTask extends AsyncTask<Thing, Void, List<Comment>> implements JsonParseListener {
	
	private static final String TAG = "CommentLoaderTask";
	
	private final TaskListener<List<Comment>> listener;
	private final List<Comment> comments = new ArrayList<Comment>();
	private int nesting;
	
	public CommentLoaderTask(TaskListener<List<Comment>> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}

	@Override
	protected void onPostExecute(List<Comment> comments) {
		listener.onPostExecute(comments);
	}

	@Override
	protected List<Comment> doInBackground(Thing... threads) {
		try {
			URL commentsUrl = new URL("http://www.reddit.com/comments/" + threads[0].getId() + ".json");
			Log.v(TAG, commentsUrl.toString());
			
			HttpURLConnection connection = (HttpURLConnection) commentsUrl.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			new JsonParser(this).withReplies(true).parseListingArray(reader);
			stream.close();
			
			return comments;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return null;
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
			comments.add(new Comment(Html.fromHtml(text).toString(), nesting));
		}
	}

	public void onBody(String body) {
		comments.add(new Comment(Html.fromHtml(body).toString(), nesting));
	}

	public void onDataEnd() {	
	}
}
