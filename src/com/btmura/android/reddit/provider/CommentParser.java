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

package com.btmura.android.reddit.provider;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.util.JsonReader;

import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.entity.Comment;

class CommentParser extends JsonParser {

    private final Context context;

    CommentParser(Context context) {
        this.context = context;
    }

    private final Formatter formatter = new Formatter();
    private final long now = System.currentTimeMillis();

    final ArrayList<Comment> comments = new ArrayList<Comment>(360);

    @Override
    public boolean shouldParseReplies() {
        return true;
    }

    @Override
    public void onEntityStart(int index) {
        comments.add(new Comment());
    }

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        Comment c = comments.get(index);
        String kind = reader.nextString();
        if (index == 0) {
            c.type = Comment.TYPE_HEADER;
        } else if ("more".equalsIgnoreCase(kind)) {
            c.type = Comment.TYPE_MORE;
        } else {
            c.type = Comment.TYPE_COMMENT;
        }
        c.nesting = replyNesting;
    }

    @Override
    public void onTitle(JsonReader reader, int index) throws IOException {
        comments.get(index).rawTitle = readTrimmedString(reader, "");
    }

    @Override
    public void onSelfText(JsonReader reader, int index) throws IOException {
        comments.get(index).rawBody = readTrimmedString(reader, "");
    }

    @Override
    public void onBody(JsonReader reader, int index) throws IOException {
        comments.get(index).rawBody = readTrimmedString(reader, "");
    }

    @Override
    public void onAuthor(JsonReader reader, int index) throws IOException {
        comments.get(index).author = readTrimmedString(reader, "");
    }

    @Override
    public void onCreatedUtc(JsonReader reader, int index) throws IOException {
        comments.get(index).createdUtc = reader.nextLong();
    }

    @Override
    public void onNumComments(JsonReader reader, int index) throws IOException {
        comments.get(index).numComments = reader.nextInt();
    }

    @Override
    public void onUps(JsonReader reader, int index) throws IOException {
        comments.get(index).ups = reader.nextInt();
    }

    @Override
    public void onDowns(JsonReader reader, int index) throws IOException {
        comments.get(index).downs = reader.nextInt();
    }

    @Override
    public void onParseEnd() {
        int size = comments.size();
        for (int i = 0; i < size;) {
            Comment c = comments.get(i);
            if (c.type == Comment.TYPE_MORE) {
                comments.remove(i);
                size--;
            } else {
                c.assureFormat(context, formatter, now);
                i++;
            }
        }
    }
}