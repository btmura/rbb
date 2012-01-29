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
import android.text.SpannedString;
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
				e.line2 = linkify(e.selfText);
				break;
				
			case Entity.TYPE_COMMENT:
				e.line1 = linkify(e.body);
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
	
	static Pattern ESCAPED_PATTERN = Pattern.compile("&([A-Za-z]+);");
	static Pattern NAMED_LINK_PATTERN = Pattern.compile("\\[([^\\]]+?)\\]\\(([^\\)]+?)\\)");
	static Pattern LINK_PATTERN = Pattern.compile("http[s]?://([A-Za-z0-9\\./\\-_#\\?&=;,]+)");
	
	private static SpannableStringBuilder linkify(CharSequence text) {
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		
		Matcher m = ESCAPED_PATTERN.matcher(text);
		for (int deleted = 0; m.find(); ) {
			String escaped = m.group(1);
			
			int start = m.start() - deleted;
			int end = m.end() - deleted;
			
			if ("amp".equals(escaped)) {
				builder.replace(start, end, "&");
				deleted += 4;
			} else if ("gt".equals(escaped)) {
				builder.replace(start, end, ">");
				deleted += 3;
			} 
		}
		
		m.usePattern(NAMED_LINK_PATTERN);
		m.reset(builder);
		for (int deleted = 0; m.find(); ) {
			String whole = m.group();
			String title = m.group(1);
			String url = m.group(2);
				
			int start = m.start() - deleted;
			int end = m.end() - deleted;
			builder.replace(start, end, title);
			
			if (url.startsWith("/r/")) {
				url = "http://www.reddit.com" + url;
			} else if (!url.startsWith("http://") && !url.startsWith("https://")) {
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
