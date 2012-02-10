package com.btmura.android.reddit;

public class Urls {
	
	public static CharSequence subredditUrl(String name, String after) {
		StringBuilder b = new StringBuilder("http://www.reddit.com/r/").append(name).append("/.json");
		if (after != null) {
			b.append("?count=25&after=").append(after);
		}
		return b;
	}
	
	public static CharSequence commentsUrl(String id) {
		return new StringBuilder("http://www.reddit.com/comments/").append(id).append(".json");
	}
}
