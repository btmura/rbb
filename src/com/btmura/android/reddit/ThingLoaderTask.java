package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.stream.JsonReader;

public class ThingLoaderTask extends AsyncTask<Topic, Void, ArrayList<Entity>> {
	
	private static final String TAG = "ThingLoaderTask";

	private final TaskListener<ArrayList<Entity>> listener;

	public ThingLoaderTask(TaskListener<ArrayList<Entity>> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(ArrayList<Entity> things) {
		listener.onPostExecute(things);
	}

	@Override
	protected ArrayList<Entity> doInBackground(Topic... topics) {
		try {
			URL url = new URL(topics[0].getUrl().toString());
			Log.v(TAG, url.toString());
			
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
		
		private final ArrayList<Entity> things = new ArrayList<Entity>(50);
		
		private String after;
		
		@Override
		public void onEntityStart(int index) {
			Entity e = new Entity();
			e.type = Entity.TYPE_TITLE;
			things.add(e);
		}
		
		@Override
		public void onId(JsonReader reader, int index) throws IOException {
			things.get(index).name = reader.nextString();
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			things.get(index).title = Formatter.formatTitle(reader.nextString()).toString();
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			things.get(index).author = reader.nextString();
		}
		
		@Override
		public void onUrl(JsonReader reader, int index) throws IOException {
			things.get(index).url = reader.nextString();
		}
		
		@Override
		public void onPermaLink(JsonReader reader, int index) throws IOException {
			things.get(index).permaLink = reader.nextString();
		}
		
		@Override
		public void onIsSelf(JsonReader reader, int index) throws IOException {
			things.get(index).isSelf = reader.nextBoolean();
		}
		
		@Override
		public void onAfter(JsonReader reader) throws IOException {
			after = reader.nextString();
		}
		
		@Override
		public void onParseEnd() {
			if (after != null) {
				Entity e = new Entity();
				e.type = Entity.TYPE_MORE;
				e.after = after;
				things.add(e);
			}
		}
	}
}
