package com.btmura.android.reddit;

import com.btmura.android.reddit.data.Formatter;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class Thing implements Parcelable {

    public static final int TYPE_THING = 0;
    public static final int TYPE_MORE = 1;

    public int type;
    public String name;
    public String rawTitle;
    public String subreddit;
    public String author;
    public String url;
    public String thumbnail;
    public String permaLink;
    public boolean isSelf;
    public String selfText;
    public int numComments;
    public int score;
    public String status;
    public String moreKey;
    public long createdUtc;

    public CharSequence title;

    public Thing() {
    }

    public static final Parcelable.Creator<Thing> CREATOR = new Parcelable.Creator<Thing>() {
        public Thing createFromParcel(Parcel source) {
            return new Thing(source);
        }

        public Thing[] newArray(int size) {
            return new Thing[size];
        }
    };

    Thing(Parcel parcel) {
        type = parcel.readInt();
        name = parcel.readString();
        rawTitle = parcel.readString();
        url = parcel.readString();
        permaLink = parcel.readString();
        isSelf = parcel.readInt() == 1;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(name);
        dest.writeString(rawTitle);
        dest.writeString(url);
        dest.writeString(permaLink);
        dest.writeInt(isSelf ? 1 : 0);
    }

    public String getId() {
        int sepIndex = name.indexOf('_');
        return name.substring(sepIndex + 1);
    }

    public boolean hasThumbnail() {
        return thumbnail != null && !thumbnail.isEmpty() && !"default".equals(thumbnail)
                && !"self".equals(thumbnail) && !"nsfw".equals(thumbnail);
    }

    public Thing assureTitle(Context context) {
        if (title == null) {
            title = Formatter.formatTitle(context, rawTitle);
        }
        return this;
    }

    public int describeContents() {
        return 0;
    }
}
