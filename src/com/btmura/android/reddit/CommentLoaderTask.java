package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
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
			e.title = reader.nextString();
		}
		
		@Override
		public void onSelfText(JsonReader reader) throws IOException {
			Entity e = entities.get(entityIndex);
			e.selfText = linkify(reader.nextString());
		}
		
		@Override
		public void onBody(JsonReader reader) throws IOException {
			Entity e = entities.get(entityIndex);
			e.body = linkify(reader.nextString());
		}

		@Override
		public boolean parseReplies() {
			return true;
		}
	}
	
	static Pattern NAMED_LINK_PATTERN = Pattern.compile("(\\[([^\\]]+?)\\]\\(([^\\)]+?)\\))");
	static Pattern LINK_PATTERN = Pattern.compile("http[s]?://([A-Za-z0-9\\./\\-_#\\?&=;,]+)");
	
	private static SpannableStringBuilder linkify(CharSequence text) {
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		
		Matcher m = NAMED_LINK_PATTERN.matcher(text);
		for (int deleted = 0; m.find(); ) {
			String whole = m.group(1);
			String title = m.group(2);
			String url = m.group(3);
			
			int start = m.start() - deleted;
			int end = m.end() - deleted;
			builder.replace(start, end, title);
			
			if (url.startsWith("/r/")) {
				url = "http://www.reddit.com" + url;
			} else if (!url.startsWith("http://")) {
				url = "http://" + url;
			}
			
			URLSpan span = new URLSpan(url);
			builder.setSpan(span, start, start + title.length(), 0);
			
			deleted += whole.length() - title.length();
		}
		
		m.usePattern(LINK_PATTERN);
		m.reset(builder);
		while (m.find()) {
			URLSpan span = new URLSpan(m.group());
			builder.setSpan(span, m.start(), m.end(), 0);
		}

		return builder;
	}
}
