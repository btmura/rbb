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

import com.google.gson.stream.JsonReader;

public class ThingLoaderTask extends AsyncTask<Topic, Void, List<Thing>> {
	
	private static final String TAG = "ThingLoaderTask";

	private final TaskListener<List<Thing>> listener;

	public ThingLoaderTask(TaskListener<List<Thing>> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(List<Thing> things) {
		listener.onPostExecute(things);
	}

	@Override
	protected List<Thing> doInBackground(Topic... topics) {
		try {
			URL url = new URL(topics[0].getUrl());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			ThingParser parser = new ThingParser();
			parser.parseListingObject(reader);
			stream.close();
			
			connection.disconnect();
			
			return parser.things;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return null;
	}
	
	class ThingParser extends JsonParser {
		
		private final ArrayList<Thing> things = new ArrayList<Thing>();
		
		String id;
		String title;
		String url;
		boolean isSelf;
		
		@Override
		public void onId(JsonReader reader) throws IOException {
			this.id = reader.nextString();
		}
		
		@Override
		public void onTitle(JsonReader reader) throws IOException {
			this.title = reader.nextString();
		}
		
		@Override
		public void onUrl(JsonReader reader) throws IOException {
			this.url = reader.nextString();
		}
		
		@Override
		public void onIsSelf(JsonReader reader) throws IOException {
			this.isSelf = reader.nextBoolean();
		}
		
		@Override
		public void onEntityEnd() {
			if (id != null && title != null && url != null) {
				things.add(new Thing(id, Html.fromHtml(title).toString(), url, isSelf));
			}
			id = title = url = null;
		}
	}
}
