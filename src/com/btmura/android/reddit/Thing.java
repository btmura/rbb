package com.btmura.android.reddit;

public class Thing {

	public final String name;
	
	public final String title;
	
	public Thing(String name, String title) {
		this.name = name;
		this.title = title;
	}
	
	public String getId() {
		int sepIndex = name.indexOf('_');
		return name.substring(sepIndex + 1);
	}
	
	@Override
	public String toString() {
		return title;
	}
}
