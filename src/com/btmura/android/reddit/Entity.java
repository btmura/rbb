package com.btmura.android.reddit;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Entity implements Parcelable {
	
	public static final int NUM_TYPES = 4;
	public static final int TYPE_THING = 0;
	public static final int TYPE_HEADER = 1;
	public static final int TYPE_COMMENT = 2;
	public static final int TYPE_MORE = 3;
	
	public int type;
	public String name;
	public String title;
	public String subreddit;
	public String author;
	public String url;
	public String thumbnail;
	public String permaLink;
	public boolean isSelf;
	public String selfText;
	public String body;
	public int numComments;
	public int score;
	public int ups;
	public int downs;
	public List<String> children;
	
	public CharSequence line1;
	public CharSequence line2;
	public CharSequence line3;
	public boolean progress;
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
		subreddit = parcel.readString();
		author = parcel.readString();
		url = parcel.readString();
		permaLink = parcel.readString();
		isSelf = parcel.readInt() == 1;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(type);
		dest.writeString(name);
		dest.writeString(title);
		dest.writeString(subreddit);
		dest.writeString(author);
		dest.writeString(url);
		dest.writeString(permaLink);
		dest.writeInt(isSelf ? 1 : 0);
	}
	
	public String getId() {
		int sepIndex = name.indexOf('_');
		return name.substring(sepIndex + 1);
	}

	public int describeContents() {
		return 0;
	}
}
