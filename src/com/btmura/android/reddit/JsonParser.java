package com.btmura.android.reddit;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JsonParser {

	public int replyNesting;
	
	private int entityIndex;
	
	public void parseListingArray(JsonReader reader) throws IOException {
		reset();
		doParseListingArray(reader);
	}
	
	public void parseListingObject(JsonReader reader) throws IOException {
		reset();
		doParseListingObject(reader);
	}
	
	private void reset() {
		entityIndex = -1;
		replyNesting = 0;
	}
	
	private void doParseListingArray(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			doParseListingObject(reader);
		}
		reader.endArray();
	}

	private void doParseListingObject(JsonReader reader) throws IOException {
		if (JsonToken.BEGIN_OBJECT == reader.peek()) {
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if ("data".equals(name)) {
					doParseListingData(reader);
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		} else {
			reader.skipValue();
		}
	}
	
	private void doParseListingData(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if ("children".equals(name)) {
				doParseListingChildren(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	private void doParseListingChildren(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			doParseEntityObject(reader);
		}
		reader.endArray();
	}
	
	private void doParseEntityObject(JsonReader r) throws IOException {
		int i = ++entityIndex;
		onEntityStart(i);
		r.beginObject();
		while (r.hasNext()) {
			String name = r.nextName();
			if ("kind".equals(name)) {
				onKind(r, i);
			} else if ("data".equals(name)) {
				doParseEntityData(r, i);
			} else {
				r.skipValue();
			}
		}
		r.endObject();
		onEntityEnd(i);
	}
	
	private void doParseEntityData(JsonReader r, int i) throws IOException {
		r.beginObject();
		while (r.hasNext()) {
			String name = r.nextName();
			if ("name".equals(name)) {
				onId(r, i);
			} else if ("title".equals(name)) {
				onTitle(r, i);
			} else if ("author".equals(name)) {
				onAuthor(r, i);
			} else if ("url".equals(name)) {
				onUrl(r, i);
			} else if ("is_self".equals(name)) {
				onIsSelf(r, i);
			} else if ("selftext".equals(name)) {
				onSelfText(r, i);
			} else if ("body".equals(name)) {
				onBody(r, i);
			} else if ("ups".equals(name)) {
				onUps(r, i);
			} else if ("downs".equals(name)) {
				onDowns(r, i);
			} else if ("replies".equals(name)) {
				if (parseReplies()) {
					replyNesting++;
					doParseListingObject(r);
					replyNesting--;
				} else {
					r.skipValue();
				}
			} else {
				r.skipValue();
			}
		}
		r.endObject();
	}
	
	public void onEntityStart(int index) {
	}
	
	public void onKind(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onId(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onTitle(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onAuthor(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onUrl(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onIsSelf(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onSelfText(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onBody(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onUps(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onDowns(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onEntityEnd(int index) {
	}
	
	public boolean parseReplies() {
		return false;
	}
}
