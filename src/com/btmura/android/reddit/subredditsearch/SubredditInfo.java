package com.btmura.android.reddit.subredditsearch;

import android.os.Parcel;
import android.os.Parcelable;

class SubredditInfo implements Parcelable {

	public static final Parcelable.Creator<SubredditInfo> CREATOR = new Parcelable.Creator<SubredditInfo>() {
		public SubredditInfo createFromParcel(Parcel source) {
			return new SubredditInfo(source);
		}
		
		public SubredditInfo[] newArray(int size) {
			return new SubredditInfo[size];
		}
	};
	
	public String displayName;
	public String title;
	public String description;
	public int subscribers;
	public String status;
	
	SubredditInfo() {
	}
	
	SubredditInfo(Parcel parcel) {
		displayName = parcel.readString();
		title = parcel.readString();
		description = parcel.readString();
		status = parcel.readString();
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(displayName);
		dest.writeString(title);
		dest.writeString(description);
		dest.writeString(status);
	}
	
	public int describeContents() {
		return 0;
	}
}
