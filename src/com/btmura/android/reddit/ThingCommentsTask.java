package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.stream.JsonReader;

public class ThingCommentsTask extends AsyncTask<Thing, ThingPart, Boolean> {
	
	private static final String TAG = "ThingTask";
	
	private final ListFragment frag;
	
	private final ThingPartAdapter adapter;
	
	public ThingCommentsTask(ListFragment frag, ThingPartAdapter adapter) {
		this.frag = frag;
		this.adapter = adapter;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (frag.isAdded()) {
			frag.setListShown(false);
		}
		adapter.clear();
	}
	
	@Override
	protected void onProgressUpdate(ThingPart... parts) {
		super.onProgressUpdate(parts);
		adapter.addAll(parts);
		if (frag.isAdded()) {
			frag.setListShown(true);
		}
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
			parseThings(reader, true);
			stream.close();
			
			return true;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return false;
	}
	
	private void parseThings(JsonReader reader, boolean onlyComments) throws IOException {
		Log.v(TAG, "parseThings");
		reader.beginArray();
		while (reader.hasNext()) {
			parseListing(reader, onlyComments);
		}
		reader.endArray();
	}

	private void parseListing(JsonReader reader, boolean onlyComments) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("data")) {
				parseListingData(reader, onlyComments);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseListingData(JsonReader reader, boolean onlyComments) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("children")) {
				parseChildren(reader, onlyComments);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseChildren(JsonReader reader, boolean onlyComments) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			parseThread(reader, onlyComments);
		}
		reader.endArray();
	}
	
	private void parseThread(JsonReader reader, boolean onlyComments) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("data")) {
				parseThreadData(reader, onlyComments);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseThreadData(JsonReader reader, boolean onlyComments) throws IOException {
		reader.beginObject();
		String title = null;
		String selfText = null;
		String body = null;
		String url = null;
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("title")) {
				title = reader.nextString();
			} else if (name.equals("selftext")) {
				selfText = reader.nextString();
			} else if (name.equals("url")) {
				url = reader.nextString();
			} else if (name.equals("body")) {
				body = reader.nextString();
			} else {
				reader.skipValue();
			}
		}
		if (!onlyComments) {
			if (title != null && title.length() > 0) {
				publishProgress(ThingPart.text(title));
			}
			if (selfText != null && selfText.length() > 0) {
				publishProgress(ThingPart.text(selfText));
			}
			if (url != null && url.length() > 0) {
				publishProgress(ThingPart.link(url));
			}
		} else {
			if (body != null) {
				publishProgress(ThingPart.text(body));
			}
		}
		reader.endObject();
	}
}
