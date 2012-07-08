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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.RelativeTime;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

public class Thing implements Parcelable {

    public static final int TYPE_THING = 0;
    public static final int TYPE_MORE = 1;

    public int type;
    public String name;
    public String rawTitle;
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
        subreddit = parcel.readString();
        url = parcel.readString();
        permaLink = parcel.readString();
        isSelf = parcel.readInt() == 1;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(name);
        dest.writeString(rawTitle);
        dest.writeString(subreddit);
        dest.writeString(url);
        dest.writeString(permaLink);
        dest.writeInt(isSelf ? 1 : 0);
    }

    public String getId() {
        int sepIndex = name.indexOf('_');
        return name.substring(sepIndex + 1);
    }

    public Thing assureTitle(Context c, Formatter f) {
        if (type == TYPE_MORE || title != null) {
            return this;
        }
        title = f.formatTitle(c, rawTitle);
        return this;
    }

    public Thing assureFormat(Context c, Formatter f, String parentSubreddit, long now) {
        if (type == TYPE_MORE || status != null) {
            return this;
        }
        assureTitle(c, f);
        boolean showSubreddit = parentSubreddit == null
                || !parentSubreddit.equalsIgnoreCase(subreddit);
        int resId = showSubreddit ? R.string.thing_status_subreddit : R.string.thing_status;

        String nsfw = over18 ? c.getString(R.string.thing_nsfw) : "";
        String rt = RelativeTime.format(c, now, createdUtc);
        String comments = c.getResources().getQuantityString(R.plurals.comments, numComments,
                numComments);
        status = c.getString(resId, subreddit, author, rt, comments, nsfw);
        if (!nsfw.isEmpty()) {
            SpannableStringBuilder b = new SpannableStringBuilder(status);
            b.setSpan(new ForegroundColorSpan(Color.RED), 0, nsfw.length(), 0);
            status = b;
        }
        details = c.getString(R.string.thing_details, ups, downs, domain);
        return this;
    }

    public int describeContents() {
        return 0;
    }
}
