package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import com.google.gson.stream.JsonReader;

public class CommentLoaderTask extends AsyncTask<Entity, Void, ArrayList<Entity>> {
	
	private static final String TAG = "CommentLoaderTask";
	
	private final TaskListener<ArrayList<Entity>> listener;


	public CommentLoaderTask(TaskListener<ArrayList<Entity>> listener) {
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}

	@Override
	protected void onPostExecute(ArrayList<Entity> comments) {
		listener.onPostExecute(comments);
	}

	@Override
	protected ArrayList<Entity> doInBackground(Entity... things) {
		try {
			URL commentsUrl = new URL("http://www.reddit.com/comments/" + things[0].getId() + ".json");
			Log.v(TAG, commentsUrl.toString());
			
			HttpURLConnection connection = (HttpURLConnection) commentsUrl.openConnection();
			connection.connect();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			EntityParser parser = new EntityParser();
			parser.parseListingArray(reader);
			stream.close();
	
			Log.v(TAG, "Number of comments: " + parser.entities.size());
			return parser.entities;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	class EntityParser extends JsonParser {
		
		private final ArrayList<Entity> entities = new ArrayList<Entity>(250);
		
		@Override
		public void onEntityStart() {
			Entity e = new Entity();
			entities.add(e);
		}
		
		@Override
		public void onKind(JsonReader reader) throws IOException {
			Entity e = entities.get(entityIndex);
			String kind = reader.nextString();
			if (entityIndex == 0) {
				e.type = Entity.TYPE_HEADER;
			} else if ("more".equalsIgnoreCase(kind)) {
				e.type = Entity.TYPE_MORE;
			} else {
				e.type = Entity.TYPE_COMMENT;
			}
			e.nesting = replyNesting;
		}
		
		@Override
		public void onTitle(JsonReader reader) throws IOException {
			Entity e = entities.get(entityIndex);
			e.title = getString(reader);
			Log.v(TAG, e.title);
		}
		
		@Override
		public void onSelfText(JsonReader reader) throws IOException {
			Entity e = entities.get(entityIndex);
			e.selfText = getString(reader);
		}
		
		@Override
		public void onBody(JsonReader reader) throws IOException {
			Entity e = entities.get(entityIndex);
			e.body = getString(reader);
		}
		
		@Override
		public boolean parseReplies() {
			return true;
		}
		
		String getString(JsonReader reader) throws IOException {
			return format(reader.nextString().trim());
		}
		
		String format(String text) {
			return Html.fromHtml(text.replaceAll("\n", "<br />")).toString();
		}
	}
}
