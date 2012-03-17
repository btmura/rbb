package com.btmura.android.reddit;

import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.RelativeTime;

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

    public Thing assureTitle(Context c) {
        if (type == TYPE_MORE || title != null) {
            return this;
        }
        title = Formatter.formatTitle(c, rawTitle);
        return this;
    }

    public Thing assureFormat(Context c, String parentSubreddit, long now) {
        if (type == TYPE_MORE || status != null) {
            return this;
        }
        assureTitle(c);
        boolean showSubreddit = !parentSubreddit.equalsIgnoreCase(subreddit);
        int resId = showSubreddit ? R.string.thing_status_subreddit : R.string.thing_status;
        String rt = RelativeTime.format(c, now, createdUtc);
        status = c.getString(resId, subreddit, author, rt, score, numComments);
        return this;
    }

    public int describeContents() {
        return 0;
    }
}
