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
	
	public final String url;
	
	public final boolean isSelf;
	
	public Thing(String id, String title, String url, boolean isSelf) {
		this.id = id;
		this.title = title;
		this.url = url;
		this.isSelf = isSelf;
	}
	
	private Thing(Parcel parcel) {
		this.id = parcel.readString();
		this.title = parcel.readString();
		this.url = parcel.readString();
		this.isSelf = parcel.readInt() == 1;
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
		dest.writeString(url);
		dest.writeInt(isSelf ? 1 : 0);
	}		
}
