/*
 * Copyright (C) 2012 Brian Muramatsu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmura.android.reddit.entity;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.provider.ThingProvider.Things;

public class Thing implements Parcelable {

    public static final int TYPE_THING = 0;
    public static final int TYPE_MORE = 1;

    public int type;
    public String name;
    public String title;
    public boolean over18;
    public String subreddit;
    public String author;
    public String url;
    public String domain;
    public String thumbnail;
    public String permaLink;
    public boolean isSelf;
    public String selfText;
    public int numComments;
    public int score;
    public int ups;
    public int downs;
    public int likes;
    public CharSequence status;
    public String details;
    public String moreKey;
    public long createdUtc;

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
        title = parcel.readString();
        subreddit = parcel.readString();
        url = parcel.readString();
        permaLink = parcel.readString();
        isSelf = parcel.readInt() == 1;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(name);
        dest.writeString(title);
        dest.writeString(subreddit);
        dest.writeString(url);
        dest.writeString(permaLink);
        dest.writeInt(isSelf ? 1 : 0);
    }

    public static String getId(String name) {
        int sepIndex = name.indexOf('_');
        return name.substring(sepIndex + 1);
    }

    public String getId() {
        int sepIndex = name.indexOf('_');
        return name.substring(sepIndex + 1);
    }

    public Thing assureFormat(Context c, Formatter f, String parentSubreddit, long now) {
        if (type == TYPE_MORE || status != null) {
            return this;
        }
        details = c.getString(R.string.thing_details, ups, downs, domain);
        return this;
    }

    public String getName() {
        return name;
    }

    public int describeContents() {
        return 0;
    }

    public static String getSubreddit(Bundle thingBundle) {
        if (thingBundle != null) {
            return thingBundle.getString(Things.COLUMN_SUBREDDIT);
        } else {
            return null;
        }
    }

    public static String getDomain(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_DOMAIN);
    }

    public static int getLikes(Bundle thingBundle) {
        return thingBundle.getInt(Things.COLUMN_LIKES);
    }

    public static String getPermaLink(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_PERMA_LINK);
    }

    public static int getScore(Bundle thingBundle) {
        return thingBundle.getInt(Things.COLUMN_SCORE);
    }

    public static boolean isSelf(Bundle thingBundle) {
        return thingBundle.getBoolean(Things.COLUMN_SELF);
    }

    public static String getUrl(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_URL);
    }

    public static CharSequence getTitle(Bundle thingBundle) {
        return thingBundle.getCharSequence(Things.COLUMN_TITLE);
    }

    public static String getThumbnail(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_THUMBNAIL_URL);
    }

    public static boolean hasThumbnail(Bundle thingBundle) {
        return !TextUtils.isEmpty(getThumbnail(thingBundle));
    }
}
