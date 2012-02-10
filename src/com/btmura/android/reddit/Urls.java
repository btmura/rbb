package com.btmura.android.reddit;

public class Urls {
	
	public static CharSequence subredditUrl(String name, int filter, String after) {
		StringBuilder b = new StringBuilder("http://www.reddit.com/r/").append(name);
		
		switch (filter) {
		case FilterAdapter.FILTER_HOT:
			break;
		
		case FilterAdapter.FILTER_NEW:
			b.append("/new");
			break;
		
		case FilterAdapter.FILTER_CONTROVERSIAL:
			b.append("/controversial");
			break;
			
		case FilterAdapter.FILTER_TOP:
			b.append("/top");
			break;
		
		default:
			throw new IllegalArgumentException(Integer.toString(filter));
		}
		
		b.append("/.json");
		if (after != null) {
			b.append("?count=25&after=").append(after);
		}
		return b;
	}
	
	public static CharSequence commentsUrl(String id) {
		return new StringBuilder("http://www.reddit.com/comments/").append(id).append(".json");
	}
}
