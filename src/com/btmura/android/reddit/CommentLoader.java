package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.common.Formatter;
import com.btmura.android.reddit.common.JsonParser;

public class CommentLoader extends AsyncTaskLoader<List<Comment>> {

	private static final String TAG = "CommentLoader";
	
	private final CharSequence url;
	private List<Comment> comments;
	
	public CommentLoader(Context context, CharSequence url) {
		super(context);
		this.url = url;
	}
	
	@Override
	protected void onStartLoading() {
		Log.v(TAG, "onStartLoading");
		super.onStartLoading();
		if (comments != null) {
			deliverResult(comments);
		} else {
			forceLoad();
		}
	}
	
	@Override
	public void deliverResult(List<Comment> comments) {
		Log.v(TAG, "deliverResult");
		this.comments = comments;
		super.deliverResult(comments);
	}
	
	@Override
	protected void onAbandon() {
		Log.v(TAG, "onAbandon");
		super.onAbandon();
	}
	
	@Override
	public void onCanceled(List<Comment> data) {
		Log.v(TAG, "onCancelled");
		super.onCanceled(data);
	}
	
	@Override
	protected void onReset() {
		Log.v(TAG, "onReset");
		super.onReset();
	}
	
	@Override
	public List<Comment> loadInBackground() {
		try {
			URL u = new URL(url.toString());
			Log.v(TAG, url.toString());
			
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.connect();
			
			InputStream stream = conn.getInputStream();
			JsonReader reader = new JsonReader(new InputStreamReader(stream));
			CommentParser parser = new CommentParser();
			parser.parseListingArray(reader);
			stream.close();
			
			return parser.comments;
			
		} catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}
	
	class CommentParser extends JsonParser {
		
		private final List<Comment> comments = new ArrayList<Comment>(360);
		
		@Override
		public boolean shouldParseReplies() {
			return true;
		}
		
		@Override
		public void onEntityStart(int index) {
			comments.add(new Comment());
		}
		
		@Override
		public void onKind(JsonReader reader, int index) throws IOException {
			Comment c = comments.get(index);
			String kind = reader.nextString();
			if (index == 0) {
				c.type = Comment.TYPE_HEADER;
			} else if ("more".equalsIgnoreCase(kind)) {
				c.type = Comment.TYPE_MORE;
			} else {
				c.type = Comment.TYPE_COMMENT;
			}
			c.nesting = replyNesting;
		}
		
		@Override
		public void onTitle(JsonReader reader, int index) throws IOException {
			comments.get(index).title = Formatter.format(readTrimmedString(reader, "")).toString();
		}
		
		@Override
		public void onAuthor(JsonReader reader, int index) throws IOException {
			comments.get(index).author = readTrimmedString(reader, "");
		}
		
		@Override
		public void onSelfText(JsonReader reader, int index) throws IOException {
			comments.get(index).body = Formatter.format(readTrimmedString(reader, ""));
		}
		
		@Override
		public void onBody(JsonReader reader, int index) throws IOException {
			comments.get(index).body = Formatter.format(readTrimmedString(reader, ""));
		}
		
		@Override
		public void onNumComments(JsonReader reader, int index) throws IOException {
			comments.get(index).numComments = reader.nextInt();
		}
		
		@Override
		public void onUps(JsonReader reader, int index) throws IOException {
			comments.get(index).ups = reader.nextInt();
		}
		
		@Override
		public void onDowns(JsonReader reader, int index) throws IOException {
			comments.get(index).downs = reader.nextInt();
		}
		
		@Override
		public void onEntityEnd(int index) {
			Comment c = comments.get(index);
			switch (c.type) {
			case Comment.TYPE_HEADER:
				c.status = getHeaderStatus(c);
				c.author = null;
				break;
			
			case Comment.TYPE_COMMENT:
				c.status = getCommentStatus(c);
				c.author = null;
				break;
				
			case Comment.TYPE_MORE:
				break;
				
			default:
				throw new IllegalArgumentException("Unsupported type: " + c.type);
			}
		}
		
		private CharSequence getHeaderStatus(Comment c) {
			return getContext().getString(R.string.comment_header_status, c.author, c.ups - c.downs, c.numComments);
		}
		
		private CharSequence getCommentStatus(Comment c) {
			return getContext().getString(R.string.comment_comment_status, c.author, c.ups - c.downs);
		}
		
		@Override
		public void onParseEnd() {
			int size = comments.size();
			for (int i = 0; i < size; ) {
				Comment c = comments.get(i);
				if (c.type == Comment.TYPE_MORE) {
					comments.remove(i);
					size--;
				} else {
					i++;
				}
			}
		}
	}
}
