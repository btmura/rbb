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
	
	public static Topic newTopic(String title) {
		return new Topic(title);
	}
	
	private Topic(String title) {
		this.title = title;
	}
	
	private Topic(Parcel in) {
		this.title = in.readString();
	}
	
	public String getUrl() {
		return "http://www.reddit.com/r/" + title + "/.json";
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
	}
}
