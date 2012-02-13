package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.btmura.android.reddit.EntityListFragment.LoadResult;


public class ThingLoaderTask extends AsyncTask<Void, Void, LoadResult<String>> {
	
	static class ThingLoaderResult implements LoadResult<String> {
		ArrayList<Entity> entities;
		String after;
		
		public ArrayList<Entity> getEntities() {
			return entities;
		}
		
		public String getMoreKey() {
			return after;
		}
	}
	
	private static final String TAG = "ThingLoaderTask";

	private final Context context;
	private final String subreddit;
	private final CharSequence url;
	private final TaskListener<LoadResult<String>> listener;

	public ThingLoaderTask(Context context, String subreddit, CharSequence url, TaskListener<LoadResult<String>> listener) {
		this.context = context;
		this.subreddit = subreddit;
		this.url = url;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(LoadResult<String> result) {
		listener.onPostExecute(result);
	}

	@Override
	protected ThingLoaderResult doInBackground(Void... voidRays) {
		ThingLoaderResult result = new ThingLoaderResult();
		try {
			URL subredditUrl = new URL(url.toString());
			Log.v(TAG, subredditUrl.toString());
			
			HttpURLConnection connection = (HttpURLConnection) subredditUrl.openConnection();
			connection.connect();
			
			long t1 = SystemClock.currentThreadTimeMillis();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			ThingParser parser = new ThingParser();
			parser.parseListingObject(reader);
			stream.close();
			
			long t2 = SystemClock.currentThreadTimeMillis();
			Log.v(TAG, Long.toString(t2 - t1));
			Log.v(TAG, Integer.toString(parser.entities.size()));
			
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
	
	class ThingParser extends JsonParser {
		
		private final ArrayList<Entity> entities = new ArrayList<Entity>(25);
		private String after;
		
		@Override
		public void onEntityStart(int index) {
			Entity e = new Entity();
			e.type = Entity.TYPE_THING;
			entities.add(e);
		}
		
		@Override
		public void onName(JsonReader reader, int index) throws IOException {
			getEntity(index).name = getString(reader);
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			getEntity(index).title = Formatter.format(getString(reader)).toString();
		}
		
		@Override
		public void onSubreddit(JsonReader reader, int index) throws IOException {
			getEntity(index).subreddit = getString(reader);
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			getEntity(index).author = getString(reader);
		}
		
		@Override
		public void onUrl(JsonReader reader, int index) throws IOException {
			getEntity(index).url = getString(reader);
		}
		
		@Override
		public void onPermaLink(JsonReader reader, int index) throws IOException {
			getEntity(index).permaLink = getString(reader);
		}
		
		@Override
		public void onIsSelf(JsonReader reader, int index) throws IOException {
			getEntity(index).isSelf = reader.nextBoolean();
		}
		
		@Override
		public void onNumComments(JsonReader reader, int index) throws IOException {
			getEntity(index).numComments = reader.nextInt();
		}
		
		@Override
		public void onScore(JsonReader reader, int index) throws IOException {
			getEntity(index).score = reader.nextInt();
		}
		
		private Entity getEntity(int index) {
			return entities.get(index);
		}
		
		private String getString(JsonReader reader) throws IOException {
			return reader.nextString().trim();
		}
		
		@Override
		public void onEntityEnd(int index) {
			Entity e = entities.get(index);
			switch (e.type) {
			case Entity.TYPE_THING:
				e.line1 = e.title;
				e.line2 = getInfo(e);
				break;
			}
		}
		
		private CharSequence getInfo(Entity e) {
			if (subreddit.equalsIgnoreCase(e.subreddit)) {
				return context.getString(R.string.entity_thing_info, e.author, e.score, e.numComments);	
			} else {
				return context.getString(R.string.entity_thing_info_2, e.subreddit, e.author, e.score, e.numComments);
			}
		}
		
		@Override
		public void onAfter(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
			} else {
				after = reader.nextString();
			}
		}
	}
}
