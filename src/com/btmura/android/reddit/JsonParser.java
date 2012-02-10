package com.btmura.android.reddit;

import java.io.IOException;

import android.util.JsonReader;
import android.util.JsonToken;

public class JsonParser {

	public int replyNesting;
	
	private int entityIndex;
	
	public void parseListingArray(JsonReader reader) throws IOException {
		reset();
		onParseStart();
		doParseListingArray(reader);
		onParseEnd();
	}
	
	public void parseListingObject(JsonReader reader) throws IOException {
		reset();
		onParseStart();
		doParseListingObject(reader);
		onParseEnd();
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
			} else if ("after".equals(name)) {
				onAfter(reader);
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
			if ("id".equals(name)) {
				onId(r, i);
			} else if ("name".equals(name)) {
				onName(r, i);
			} else if ("title".equals(name)) {
				onTitle(r, i);
			} else if ("author".equals(name)) {
				onAuthor(r, i);
			} else if ("subreddit".equals(name)) {
				onSubreddit(r, i);
			} else if ("url".equals(name)) {
				onUrl(r, i);
			} else if ("permalink".equals(name)) {
				onPermaLink(r, i);
			} else if ("is_self".equals(name)) {
				onIsSelf(r, i);
			} else if ("selftext".equals(name)) {
				onSelfText(r, i);
			} else if ("body".equals(name)) {
				onBody(r, i);
			} else if ("num_comments".equals(name)) {
				onNumComments(r, i);
			} else if ("score".equals(name)) {
				onScore(r, i);
			} else if ("ups".equals(name)) {
				onUps(r, i);
			} else if ("downs".equals(name)) {
				onDowns(r, i);
			} else if ("children".equals(name)) {
				onChildren(r, i);
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
	
	public void onParseStart() {
	}
	
	public void onAfter(JsonReader reader) throws IOException {
		reader.skipValue();
	}
	
	public void onEntityStart(int index) {
	}
	
	public void onKind(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onId(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onName(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onTitle(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onAuthor(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onSubreddit(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onUrl(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onPermaLink(JsonReader reader, int index) throws IOException {
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
	
	public void onNumComments(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onScore(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onUps(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onDowns(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onChildren(JsonReader reader, int index) throws IOException {
		reader.skipValue();
	}
	
	public void onEntityEnd(int index) {
	}
	
	public void onParseEnd() {
	}
	
	public boolean parseReplies() {
		return false;
	}
}
