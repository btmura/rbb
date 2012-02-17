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
import android.text.SpannedString;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.EntityListFragment.LoadResult;
import com.btmura.android.reddit.common.Formatter;
import com.btmura.android.reddit.common.JsonParser;


public class CommentLoaderTask extends AsyncTask<Void, Void, LoadResult<Void>> {
	
	static class CommentLoaderResult implements LoadResult<Void> {
		ArrayList<Entity> entities;
		
		public ArrayList<Entity> getEntities() {
			return entities;
		}
		
		public Void getMoreKey() {
			return null;
		}
	}
	
	private static final String TAG = "CommentLoaderTask";

	private final Context context;
	private final CharSequence commentUrl;
	private final TaskListener<LoadResult<Void>> listener;

	public CommentLoaderTask(Context context, CharSequence commentUrl, TaskListener<LoadResult<Void>> listener) {
		this.context = context;
		this.commentUrl = commentUrl;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onPreExecute();
	}

	@Override
	protected void onPostExecute(LoadResult<Void> result) {
		listener.onPostExecute(result);
	}

	@Override
	protected LoadResult<Void> doInBackground(Void... intoTheVoid) {
		CommentLoaderResult result = new CommentLoaderResult();
		try {
			URL url = new URL(commentUrl.toString());
			Log.v(TAG, url.toString());
			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			long t1 = SystemClock.currentThreadTimeMillis();
			
			InputStream stream = connection.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			EntityParser parser = new EntityParser();
			parser.parseListingArray(reader);
			stream.close();
			
			long t2 = SystemClock.currentThreadTimeMillis();
			Log.v(TAG, Long.toString(t2 - t1));
			Log.v(TAG, Integer.toString(parser.entities.size()));
			
			result.entities = parser.entities;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, "", e);
		} catch (IOException e) {
			Log.e(TAG, "", e);
		}
		return result;
	}
	
	class EntityParser extends JsonParser {
		
		private final ArrayList<Entity> entities = new ArrayList<Entity>(360);
		
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
			getEntity(index).title = Formatter.format(getString(reader)).toString();
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			getEntity(index).author = getString(reader);
		}

		@Override
		public void onSelfText(JsonReader reader, int index) throws IOException {
			getEntity(index).selfText = getString(reader);
		}
		
		@Override
		public void onBody(JsonReader reader, int index) throws IOException {
			getEntity(index).body = getString(reader);
		}
		
		@Override
		public void onNumComments(JsonReader reader, int index) throws IOException {
			getEntity(index).numComments = reader.nextInt();
		}
		
		@Override
		public void onUps(JsonReader reader, int index) throws IOException {
			getEntity(index).ups = reader.nextInt();
		}
		
		@Override
		public void onDowns(JsonReader reader, int index) throws IOException {
			getEntity(index).downs = reader.nextInt();
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
			case Entity.TYPE_HEADER:
				e.line1 = e.title;
				e.line2 = Formatter.format(e.selfText);
				e.line3 = getInfo(e);
				break;
				
			case Entity.TYPE_COMMENT:
				e.line1 = Formatter.format(e.body);
				e.line2 = getInfo(e);
				break;
				
			case Entity.TYPE_MORE:
				e.line1 = new SpannedString(context.getString(R.string.load_more));
				e.progress = false;
				break;
			
			default:
				throw new IllegalArgumentException("Unsupported type: " + e.type);
			}
		}

		private CharSequence getInfo(Entity e) {
			if (e.type == Entity.TYPE_HEADER) {
				return context.getString(R.string.entity_thing_info, e.author, e.ups - e.downs, e.numComments);
			} else {
				return context.getString(R.string.entity_comment_info, e.author, e.ups - e.downs);
			}
		}
		
		@Override
		public void onParseEnd() {
			int size = entities.size();
			for (int i = 0; i < size; ) {
				Entity e = entities.get(i);
				if (e.type == Entity.TYPE_MORE) {
					entities.remove(i);
					size--;
				} else {
					i++;
				}
			}
		}
		
		@Override
		public boolean parseReplies() {
			return true;
		}
	}
}
