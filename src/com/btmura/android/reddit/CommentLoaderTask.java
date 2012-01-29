package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.text.SpannedString;
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
		public void onEntityStart(int index) {
			entities.add(new Entity());
		}
		
		@Override
		public void onKind(JsonReader reader, int index) throws IOException {
			Entity e = entities.get(index);
			String kind = reader.nextString();
			if (index == 0) {
				e.type = Entity.TYPE_HEADER;
			} else if ("more".equalsIgnoreCase(kind)) {
				e.type = Entity.TYPE_MORE;
			} else {
				e.type = Entity.TYPE_COMMENT;
			}
			e.nesting = replyNesting;
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			Entity e = entities.get(index);
			e.title = reader.nextString();
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			Entity e = entities.get(index);
			e.author = reader.nextString();
		}
		
		@Override
		public void onSelfText(JsonReader reader, int index) throws IOException {
			Entity e = entities.get(index);
			e.selfText = reader.nextString();
		}
		
		@Override
		public void onBody(JsonReader reader, int index) throws IOException {
			Entity e = entities.get(index);
			e.body = reader.nextString();
		}
		
		@Override
		public void onEntityEnd(int index) {
			Entity e = entities.get(index);
			switch (e.type) {
			case Entity.TYPE_HEADER:
				e.line1 = new SpannedString(e.title);
				e.line2 = Formatter.format(e.selfText);
				break;
				
			case Entity.TYPE_COMMENT:
				e.line1 = Formatter.format(e.body);
				break;
				
			case Entity.TYPE_MORE:
				break;
			
			default:
				throw new IllegalArgumentException("Unsupported type: " + e.type);
			}
		}

		@Override
		public boolean parseReplies() {
			return true;
		}
	}
}
