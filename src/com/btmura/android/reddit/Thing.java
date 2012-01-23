package com.btmura.android.reddit;

import android.os.Parcel;
import android.os.Parcelable;

public class Thing implements Parcelable {
	
	public static final Parcelable.Creator<Thing> CREATOR = new Parcelable.Creator<Thing>() {
		public Thing createFromParcel(Parcel source) {
			return new Thing(source);
		}
		
		public Thing[] newArray(int size) {
			return new Thing[size];
		}
	};

	public final String id;
	
	public final String title;
	
	public Thing(String id, String title) {
		this.id = id;
		this.title = title;
	}
	
	private Thing(Parcel parcel) {
		this.id = parcel.readString();
		this.title = parcel.readString();
	}
	
	public String getId() {
		int sepIndex = id.indexOf('_');
		return id.substring(sepIndex + 1);
	}
	
	@Override
	public String toString() {
		return title;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(title);
	}		
}
