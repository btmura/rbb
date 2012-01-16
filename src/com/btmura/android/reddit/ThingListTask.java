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

public class ThingListTask extends AsyncTask<Topic, Thing, Boolean> {
	
	private static final String TAG = "ThreadListTask";

	private final ThingListFragment frag;
	
	private final ThingListAdapter adapter; 
	
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
	protected void onProgressUpdate(Thing... threads) {
		super.onProgressUpdate(threads);
		adapter.addAll(threads);
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
			parseListing(reader);
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
		String id = null;
		String title = null;
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("name")) {
				id = reader.nextString();
			} else if (name.equals("title")) {
				title = reader.nextString();
			} else {
				reader.skipValue();
			}
		}
		publishProgress(new Thing(id, title));
		reader.endObject();
	}
}
