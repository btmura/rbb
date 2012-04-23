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

    public static String getName(Subreddit subreddit) {
        return subreddit != null ? subreddit.name : null;
    }
}
