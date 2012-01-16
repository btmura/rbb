package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.stream.JsonReader;

public class ThingTask extends AsyncTask<Thing, String, Boolean> {
	
	private static final String TAG = "ThingTask";
	
	private final ThingFragment frag;
	
	private final ThingAdapter adapter;
	
	public ThingTask(ThingFragment frag, ThingAdapter adapter) {
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
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		adapter.addAll(values);
		if (frag.isVisible()) {
			frag.setListShown(true);
		}
	}

	@Override
	protected Boolean doInBackground(Thing... threads) {
		try {
			Log.v(TAG, "Loading thing");
			URL url = new URL("http://www.reddit.com/by_id/" + threads[0].name + ".json");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			parseListing(reader);
			stream.close();
			
			url = new URL("http://www.reddit.com/comments/" + threads[0].getId() + ".json");
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			stream = connection.getInputStream();
			
			reader = new JsonReader(new InputStreamReader(stream));
			parseThings(reader);
			stream.close();
			
			return true;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return false;
	}
	
	private void parseThings(JsonReader reader) throws IOException {
		Log.v(TAG, "parseThings");
		reader.beginArray();
		while (reader.hasNext()) {
			parseListing(reader);
		}
		reader.endArray();
	}

	private void parseListing(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("data")) {
				parseListingData(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseListingData(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("children")) {
				parseChildren(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseChildren(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			parseThread(reader);
		}
		reader.endArray();
	}
	
	private void parseThread(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("data")) {
				parseThreadData(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseThreadData(JsonReader reader) throws IOException {
		reader.beginObject();
		String title = null;
		String description = null;
		String body = null;
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("title")) {
				title = reader.nextString();
			} else if (name.equals("selftext")) {
				description = reader.nextString();
			} else if (name.equals("body")) {
				body = reader.nextString();
			} else {
				reader.skipValue();
			}
		}
		if (title != null) {
			publishProgress(title);
		}
		if (description != null) {
			publishProgress(description);
		}
		if (body != null) {
			publishProgress(body);
		}
		reader.endObject();
	}
}
