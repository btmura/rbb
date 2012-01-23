package com.btmura.android.reddit;

public class Comment {

	public final String body;
	
	public final int nesting;
	
	public Comment(String body, int nestingAmount) {
		this.body = body;
		this.nesting = nestingAmount;
	}
	
	@Override
	public String toString() {
		return body;
	}
}
