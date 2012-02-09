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
	
	public final String name;
	
	public static Topic newTopic(String name) {
		return new Topic(name, null);
	}
		
	private Topic(String name, String after) {
		this.name = name;
	}
	
	private Topic(Parcel in) {
		this.name = in.readString();
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
	}
}
