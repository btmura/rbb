package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.btmura.android.reddit.ThingLoaderTask.ThingLoaderResult;
import com.google.gson.stream.JsonReader;

public class ThingLoaderTask extends AsyncTask<Topic, Void, ThingLoaderResult> {
	
	static class ThingLoaderResult {
		ArrayList<Entity> entities;
		String after;
	}
	
	private static final String TAG = "ThingLoaderTask";

	private final TaskListener<ThingLoaderResult> listener;

	public ThingLoaderTask(TaskListener<ThingLoaderResult> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(ThingLoaderResult result) {
		listener.onPostExecute(result);
	}

	@Override
	protected ThingLoaderResult doInBackground(Topic... topics) {
		ThingLoaderResult result = new ThingLoaderResult();
		try {
			URL url = new URL(topics[0].getUrl().toString());
			Log.v(TAG, url.toString());
			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			long t1 = SystemClock.currentThreadTimeMillis();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			ThingParser parser = new ThingParser();
			parser.parseListingObject(reader);
			stream.close();
			
			long t2 = SystemClock.currentThreadTimeMillis();
			Log.v(TAG, Long.toString(t2 - t1));
			
			connection.disconnect();
			
			result.entities = parser.entities;
			result.after = parser.after;

		} catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return result;
	}
	
	static class ThingParser extends JsonParser {
		
		private final ArrayList<Entity> entities = new ArrayList<Entity>(25);
		
		private String after;
		
		@Override
		public void onEntityStart(int index) {
			Entity e = new Entity();
			e.type = Entity.TYPE_TITLE;
			entities.add(e);
		}
		
		@Override
		public void onId(JsonReader reader, int index) throws IOException {
			getEntity(index).name = reader.nextString();
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			getEntity(index).title = Formatter.formatTitle(reader.nextString()).toString();
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			getEntity(index).author = reader.nextString();
		}
		
		@Override
		public void onUrl(JsonReader reader, int index) throws IOException {
			getEntity(index).url = reader.nextString();
		}
		
		@Override
		public void onPermaLink(JsonReader reader, int index) throws IOException {
			getEntity(index).permaLink = reader.nextString();
		}
		
		@Override
		public void onIsSelf(JsonReader reader, int index) throws IOException {
			getEntity(index).isSelf = reader.nextBoolean();
		}
		
		private Entity getEntity(int index) {
			return entities.get(index);
		}
		
		@Override
		public void onAfter(JsonReader reader) throws IOException {
			after = reader.nextString();
		}
	}
}
