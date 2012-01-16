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

	public final String name;
	
	public final String title;
	
	public Thing(String name, String title) {
		this.name = name;
		this.title = title;
	}
	
	private Thing(Parcel parcel) {
		this.name = parcel.readString();
		this.title = parcel.readString();
	}
	
	public String getId() {
		int sepIndex = name.indexOf('_');
		return name.substring(sepIndex + 1);
	}
	
	@Override
	public String toString() {
		return title;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(title);
	}		
}
