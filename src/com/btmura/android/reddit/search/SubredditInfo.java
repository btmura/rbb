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

package com.btmura.android.reddit.search;

import android.os.Parcel;
import android.os.Parcelable;

class SubredditInfo implements Parcelable {

    public static final Parcelable.Creator<SubredditInfo> CREATOR = new Parcelable.Creator<SubredditInfo>() {
        public SubredditInfo createFromParcel(Parcel source) {
            return new SubredditInfo(source);
        }

        public SubredditInfo[] newArray(int size) {
            return new SubredditInfo[size];
        }
    };

    public String displayName;
    public CharSequence title;
    public String description;
    public int subscribers;
    public String status;

    SubredditInfo() {
    }

    SubredditInfo(Parcel parcel) {
        displayName = parcel.readString();
        description = parcel.readString();
        status = parcel.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeString(description);
        dest.writeString(status);
    }

    public int describeContents() {
        return 0;
    }
}
