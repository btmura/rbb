package com.btmura.android.reddit;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class Subreddit implements Parcelable {
	
	public static final Parcelable.Creator<Subreddit> CREATOR = new Parcelable.Creator<Subreddit>() {
		public Subreddit createFromParcel(Parcel source) {
			return new Subreddit(source);
		}
		
		public Subreddit[] newArray(int size) {
			return new Subreddit[size];
		}
	};
	
	public final String name;
	public final boolean frontPage;
	public final boolean multiSubreddit;
	
	public static Subreddit frontPage(Context context) {
		return new Subreddit(context.getString(R.string.front_page), true, true);
	}
	
	public static Subreddit multiSubreddit(String name) {
		return new Subreddit(name, false, true);
	}
	
	public static Subreddit newInstance(String name) {
		return new Subreddit(name, false, false);
	}
		
	private Subreddit(String name, boolean frontPage, boolean multiSubreddit) {
		this.name = name;
		this.frontPage = frontPage;
		this.multiSubreddit = multiSubreddit;
	}
	
	private Subreddit(Parcel in) {
		this.name = in.readString();
		this.frontPage = in.readInt() == 1;
		this.multiSubreddit = in.readInt() == 1;
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
		dest.writeInt(frontPage ? 1 : 0);
		dest.writeInt(multiSubreddit ? 1 : 0);
	}
}
