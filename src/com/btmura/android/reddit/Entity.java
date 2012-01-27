package com.btmura.android.reddit;

import android.os.Parcel;
import android.os.Parcelable;

public class Entity implements Parcelable {
	
	public static final int TYPE_THING = 0;
	public static final int TYPE_COMMENT = 1;
	public static final int TYPE_MORE = 2;
	
	public int type;
	public String name;
	public String title;
	public String url;
	public boolean isSelf;
	public String selfText;
	public String body;	
	public int nesting;
	
	public Entity() {
	}
	
	public static final Parcelable.Creator<Entity> CREATOR = new Parcelable.Creator<Entity>() {
		public Entity createFromParcel(Parcel source) {
			return new Entity(source);
		}
		
		public Entity[] newArray(int size) {
			return new Entity[size];
		}
	};
	
	Entity(Parcel parcel) {
		type = parcel.readInt();
		name = parcel.readString();
		title = parcel.readString();
		url = parcel.readString();
		isSelf = parcel.readInt() == 1;
		selfText = parcel.readString();
		body = parcel.readString();
		nesting = parcel.readInt();
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(type);
		dest.writeString(name);
		dest.writeString(title);
		dest.writeString(url);
		dest.writeInt(isSelf ? 1 : 0);
		dest.writeString(selfText);
		dest.writeString(body);
		dest.writeInt(nesting);
	}
	
	public String getId() {
		int sepIndex = name.indexOf('_');
		return name.substring(sepIndex + 1);
	}

	public int describeContents() {
		return 0;
	}
}
