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

import android.os.Parcel;
import android.os.Parcelable;

public class SubredditDetails implements Parcelable, Comparable<SubredditDetails> {

    public static final Parcelable.Creator<SubredditDetails> CREATOR = new Parcelable.Creator<SubredditDetails>() {
        public SubredditDetails createFromParcel(Parcel source) {
            return new SubredditDetails(source);
        }

        public SubredditDetails[] newArray(int size) {
            return new SubredditDetails[size];
        }
    };

    public String displayName;
    public CharSequence title;
    public String description;
    public int subscribers;

    public SubredditDetails() {
    }

    SubredditDetails(Parcel parcel) {
        displayName = parcel.readString();
        description = parcel.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeString(description);
    }

    public int describeContents() {
        return 0;
    }

    public int compareTo(SubredditDetails another) {
        return displayName.compareToIgnoreCase(another.displayName);
    }
}
