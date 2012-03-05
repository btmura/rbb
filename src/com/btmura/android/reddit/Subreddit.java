package com.btmura.android.reddit;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

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

    public static Subreddit frontPage() {
        return new Subreddit("");
    }

    public static Subreddit newInstance(String name) {
        return new Subreddit(name);
    }

    private Subreddit(String name) {
        this.name = name;
    }

    private Subreddit(Parcel in) {
        this.name = in.readString();
    }

    public boolean isFrontPage() {
        return TextUtils.isEmpty(name);
    }

    public String getTitle(Context c) {
        return isFrontPage() ? c.getString(R.string.front_page) : name;
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
