package com.btmura.android.reddit;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

public class JsonParser {
	
	public interface JsonParseListener {
		void onUrl(String url);
	}
	
	private final JsonParseListener listener;
	
	public JsonParser(JsonParseListener listener) {
		this.listener = listener;
	}

	public void parse(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("data")) {
				parseListingData(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseListingData(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("children")) {
				parseChildren(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseChildren(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			parseThread(reader);
		}
		reader.endArray();
	}
	
	private void parseThread(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("data")) {
				parseThreadData(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void parseThreadData(JsonReader reader) throws IOException {
		reader.beginObject();
		String url = null;
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("url")) {
				url = reader.nextString();
			} else {
				reader.skipValue();
			}
		}

		if (url != null && url.length() > 0) {
			listener.onUrl(url);
		}
		reader.endObject();
	}
}
