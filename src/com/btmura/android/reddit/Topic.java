package com.btmura.android.reddit;

import android.os.Parcel;
import android.os.Parcelable;

public class Topic implements Parcelable {
	
	public static final Parcelable.Creator<Topic> CREATOR = new Parcelable.Creator<Topic>() {
		public Topic createFromParcel(Parcel source) {
			return new Topic(source);
		}
		
		public Topic[] newArray(int size) {
			return new Topic[size];
		}
	};
	
	public final String title;
	public final String after;
	
	public static Topic newTopic(String title) {
		return new Topic(title, null);
	}
	
	public Topic withAfter(String after) {
		return after != null ? new Topic(title, after) : this;
	}
	
	private Topic(String title, String after) {
		this.title = title;
		this.after = after;
	}
	
	private Topic(Parcel in) {
		this.title = in.readString();
		this.after = in.readString();
	}
	
	public CharSequence getUrl() {
		StringBuilder b = new StringBuilder("http://www.reddit.com/r/").append(title).append("/.json");
		if (after != null) {
			b.append("?count=25&after=").append(after);
		}
		return b;
	}
	
	@Override
	public String toString() {
		return title;
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(after);
	}
}
