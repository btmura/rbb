package com.btmura.android.reddit;


public class Topic {
	
	public final String title;
	
	private final boolean isFrontPage;

	public static Topic frontPage() {
		return new Topic("front page", true);
	}
	
	public static Topic newTopic(String title) {
		return new Topic(title, false);
	}
	
	private Topic(String title, boolean isFrontPage) {
		this.title = title;
		this.isFrontPage = isFrontPage;
	}
	
	public String getUrl() {
		if (isFrontPage) {
			return "http://www.reddit.com/.json";
		} else {
			return "http://www.reddit.com/r/" + title + "/.json";
		}
	}
	
	@Override
	public String toString() {
		return title;
	}
}
