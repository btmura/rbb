package com.btmura.android.reddit;

public class ThingPart {
	
	public static final int TYPE_TEXT = 0;
	
	public static final int TYPE_LINK = 1;
	
	public final int type;
	
	public final String value;
	
	public static final ThingPart text(String value) {
		return new ThingPart(TYPE_TEXT, value);
	}
	
	public static final ThingPart link(String value) {
		return new ThingPart(TYPE_LINK, value);
	}
	
	private ThingPart(int type, String value) {
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
