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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.RelativeTime;

public class Comment {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_MORE = 2;

    public int type;
    public int nesting;
    public String rawTitle;
    public String rawBody;
    public String author;
    public long createdUtc;
    public int ups;
    public int downs;
    public int numComments;

    public CharSequence title;
    public CharSequence body;
    public CharSequence status;

    public Comment assureFormat(Context c, long now) {
        if (type == Comment.TYPE_MORE || status != null) {
            return this;
        }
        if (rawTitle != null) {
            title = Formatter.formatTitle(c, rawTitle);
        }
        if (rawBody != null) {
            body = Formatter.formatComment(c, rawBody);
        }
        status = getStatus(c, type == Comment.TYPE_HEADER, now);
        rawTitle = rawBody = author = null;
        return this;
    }

    private CharSequence getStatus(Context c, boolean isHeader, long now) {
        int resId = isHeader ? R.string.comment_header_status : R.string.comment_comment_status;
        String rt = RelativeTime.format(c, now, createdUtc);
        return c.getString(resId, author, rt, ups - downs, numComments);
    }
}
