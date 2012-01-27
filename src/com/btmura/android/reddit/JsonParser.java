package com.btmura.android.reddit;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JsonParser {

	public int entityIndex;
	public int replyNesting;
		
	public void parseListingArray(JsonReader reader) throws IOException {
		reset();
		doParseListingArray(reader);
	}
	
	public void parseListingObject(JsonReader reader) throws IOException {
		reset();
		doParseListingObject(reader);
	}
	
	void reset() {
		entityIndex = -1;
		replyNesting = 0;
	}
	
	void doParseListingArray(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			doParseListingObject(reader);
		}
		reader.endArray();
	}

	void doParseListingObject(JsonReader reader) throws IOException {
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
	
	void doParseListingData(JsonReader reader) throws IOException {
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
	
	void doParseListingChildren(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			doParseEntityObject(reader);
		}
		reader.endArray();
	}
	
	void doParseEntityObject(JsonReader reader) throws IOException {
		++entityIndex;
		onEntityStart();
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if ("kind".equals(name)) {
				onKind(reader);
			} else if ("data".equals(name)) {
				doParseEntityData(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		onEntityEnd();
	}
	
	void doParseEntityData(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if ("name".equals(name)) {
				onId(reader);
			} else if ("title".equals(name)) {
				onTitle(reader);
			} else if ("url".equals(name)) {
				onUrl(reader);
			} else if ("is_self".equals(name)) {
				onIsSelf(reader);
			} else if ("selftext".equals(name)) {
				onSelfText(reader);
			} else if ("body".equals(name)) {
				onBody(reader);
			} else if ("replies".equals(name)) {
				if (parseReplies()) {
					replyNesting++;
					doParseListingObject(reader);
					replyNesting--;
				} else {
					reader.skipValue();
				}
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
	}
	
	public void onEntityStart() {
	}
	
	public void onKind(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onId(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onTitle(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onUrl(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onIsSelf(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onSelfText(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onBody(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onEntityEnd() {
	}
	
	public boolean parseReplies() {
		return false;
	}
}
