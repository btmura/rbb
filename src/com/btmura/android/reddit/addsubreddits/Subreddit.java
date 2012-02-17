package com.btmura.android.reddit.addsubreddits;

import android.os.Parcel;
import android.os.Parcelable;

class Subreddit implements Parcelable {

	public static final Parcelable.Creator<Subreddit> CREATOR = new Parcelable.Creator<Subreddit>() {
		public Subreddit createFromParcel(Parcel source) {
			return new Subreddit(source);
		}
		
		public Subreddit[] newArray(int size) {
			return new Subreddit[size];
		}
	};
	
	public String displayName;
	public String title;
	public String description;
		
	Subreddit() {
	}
	
	Subreddit(Parcel parcel) {
		displayName = parcel.readString();
		title = parcel.readString();
		description = parcel.readString();
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(displayName);
		dest.writeString(title);
		dest.writeString(description);
	}
	
	@Override
	public String toString() {
		return displayName;
	}
	
	public int describeContents() {
		return 0;
	}
}
