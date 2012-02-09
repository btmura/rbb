package com.btmura.android.reddit;

import java.net.URLEncoder;
import java.util.List;

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
	
	public static CharSequence moreCommentsUrl(String subreddit, String linkId, List<String> children) {
		StringBuilder b = new StringBuilder("http://www.reddit.com/api/morechildren")
				.append("?link_id=").append(linkId)
				.append("&api_type=json")
				.append("&r=").append(subreddit)
				.append("&children=");
		int numChildren = children.size();
		String delim = URLEncoder.encode(",");
		for (int i = 0; i < numChildren; i++) {
			b.append(children.get(i));
			if (i + 1 < numChildren) {
				b.append(delim);
			}
		}
		return b;
	}
}
