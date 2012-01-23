package com.btmura.android.reddit;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JsonParser {
	
	public interface JsonParseListener {
		void onDataStart();
		void onId(String id);
		void onTitle(String title);
		void onUrl(String url);
		void onIsSelf(boolean isSelf);
		void onSelfText(String text);
		void onBody(String body);
		void onDataEnd();
	}
	
	private final JsonParseListener listener;
	
	private boolean includeReplies;
	
	public JsonParser(JsonParseListener listener) {
		this.listener = listener;
	}
	
	public JsonParser withReplies(boolean includeReplies) {
		this.includeReplies = includeReplies;
		return this;
	}
	
	public void parseListingArray(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			parseListing(reader);
		}
		reader.endArray();
	}

	public void parseListing(JsonReader reader) throws IOException {
		if (JsonToken.BEGIN_OBJECT.equals(reader.peek())) {
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("data")) {
					parseData(reader);
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		} else {
			reader.skipValue();
		}
	}
	
	private void parseData(JsonReader reader) throws IOException {
		listener.onDataStart();
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if ("name".equals(name)) {
				listener.onId(reader.nextString());
			} else if ("title".equals(name)) {
				listener.onTitle(reader.nextString());
			} else if ("url".equals(name)) {
				listener.onUrl(reader.nextString());
			} else if ("is_self".equals(name)) {
				listener.onIsSelf(reader.nextBoolean());
			} else if ("selftext".equals(name)) {
				listener.onSelfText(reader.nextString());
			} else if ("body".equals(name)) {
				listener.onBody(reader.nextString());
			} else if (includeReplies && "replies".equals(name)) {
				parseListing(reader);
			} else if ("children".equals(name)) {
				parseChildren(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		listener.onDataEnd();
	}
	
	private void parseChildren(JsonReader reader) throws IOException {
		if (JsonToken.BEGIN_ARRAY.equals(reader.peek())) {
			reader.beginArray();
			while (reader.hasNext()) {
				parseListing(reader);
			}
			reader.endArray();
		} else {
			reader.skipValue();
		}
	}
}
