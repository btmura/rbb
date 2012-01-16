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
	
	private Topic(Parcel in) {
		this.title = in.readString();
		this.isFrontPage = in.readInt() == 1;
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
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeInt(isFrontPage ? 1 : 0);
	}
}
