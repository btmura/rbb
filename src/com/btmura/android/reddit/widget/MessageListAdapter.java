/*
 * Copyright (C) 2013 Brian Muramatsu
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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Objects;

public class MessageListAdapter extends AbstractThingListAdapter {

    private static final int[] MESSAGE_DETAILS = {
            ThingView.DETAIL_TIMESTAMP,
            ThingView.DETAIL_AUTHOR,
            ThingView.DETAIL_DESTINATION,
    };

    private static final int[] MESSAGE_COMMENT_DETAILS = {
            ThingView.DETAIL_TIMESTAMP,
            ThingView.DETAIL_AUTHOR,
            ThingView.DETAIL_SUBREDDIT,
    };

    private final Formatter formatter = new Formatter();

    public MessageListAdapter(Context context, String accountName, boolean singleChoice) {
        super(context, accountName, singleChoice);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof ThingView) {
            bindMessageThingView(view, context, cursor);
        }
    }

    private void bindMessageThingView(View view, Context context, Cursor cursor) {
        final String author = cursor.getString(MessageThingLoader.INDEX_AUTHOR);
        final String body = cursor.getString(MessageThingLoader.INDEX_BODY);
        final long createdUtc = cursor.getLong(MessageThingLoader.INDEX_CREATED_UTC);
        final String destination = cursor.getString(MessageThingLoader.INDEX_DESTINATION);
        final String domain = null; // No domain for messages.
        final int downs = 0; // No downs for messages.
        final boolean expanded = true; // Messages are always expanded.
        final int kind = cursor.getInt(MessageThingLoader.INDEX_KIND);
        final int likes = 0; // No likes for messages.
        final boolean isNew = isNew(cursor.getPosition());
        final int nesting = 0; // No nesting for messages.
        final int numComments = 0; // No comments for messages.
        final boolean over18 = false; // No over18 for messages.
        final String parentSubreddit = null; // No need for parentSubreddit for messages.
        final int score = 0; // No score for messages.
        final String subject = cursor.getString(MessageThingLoader.INDEX_SUBJECT);
        final String subreddit = cursor.getString(MessageThingLoader.INDEX_SUBREDDIT);
        final String thingId = cursor.getString(MessageThingLoader.INDEX_THING_ID);
        final String title = null; // No title for messages.
        final int ups = 0; // No upvotes for messages.

        final boolean drawVotingArrows = false; // No arrows for messages.
        final boolean showThumbnail = false; // No arrows for messages.
        final boolean showStatusPoints = false; // No points for messages.

        ThingView tv = (ThingView) view;
        tv.setBody(body, isNew, formatter);
        tv.setData(accountName,
                author,
                createdUtc,
                destination,
                domain,
                downs,
                expanded,
                kind,
                likes,
                subject, // actually linkTitle
                nesting,
                nowTimeMs,
                numComments,
                over18,
                parentSubreddit,
                score,
                subreddit,
                thingBodyWidth,
                thingId,
                title,
                ups,
                drawVotingArrows,
                showThumbnail,
                showStatusPoints);
        tv.setChosen(singleChoice && Objects.equals(selectedThingId, thingId));
        setThingDetails(tv, kind);
    }

    public boolean isNew(int position) {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.moveToPosition(position)) {
            // If no local read actions are pending, then rely on what reddit thinks.
            if (cursor.isNull(MessageThingLoader.INDEX_READ_ACTION)) {
                return cursor.getInt(MessageThingLoader.INDEX_NEW) == 1;
            }

            // We have a local pending action so use that to indicate if it's new.
            return cursor.getInt(MessageThingLoader.INDEX_READ_ACTION) == ReadActions.ACTION_UNREAD;
        }
        return false;
    }

    private void setThingDetails(ThingView tv, int kind) {
        switch (kind) {
            case Kinds.KIND_MESSAGE:
                tv.setDetails(MESSAGE_DETAILS);
                break;

            case Kinds.KIND_COMMENT:
                tv.setDetails(MESSAGE_COMMENT_DETAILS);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public ThingBundle getThingBundle(int position) {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.moveToPosition(position)) {
            switch (getKind(position)) {
                case Kinds.KIND_MESSAGE:
                    return ThingBundle.newMessageInstance(
                            cursor.getString(MessageThingLoader.INDEX_AUTHOR),
                            cursor.getInt(MessageThingLoader.INDEX_KIND),
                            cursor.getString(MessageThingLoader.INDEX_THING_ID));

                case Kinds.KIND_COMMENT:
                    return ThingBundle.newCommentReference(
                            cursor.getString(MessageThingLoader.INDEX_SUBREDDIT),
                            cursor.getString(MessageThingLoader.INDEX_THING_ID),
                            getLinkId(cursor));
            }
        }
        return null;
    }

    private String getLinkId(Cursor cursor) {
        String context = cursor.getString(MessageThingLoader.INDEX_CONTEXT);
        String[] parts = context.split("/");
        if (parts != null && parts.length >= 5) {
            return parts[4];
        }
        throw new IllegalStateException();
    }

    @Override
    int getKindIndex() {
        return MessageThingLoader.INDEX_KIND;
    }

    @Override
    String getLinkId(int position) {
        return null;
    }

    @Override
    String getThingId(int position) {
        return getString(position, MessageThingLoader.INDEX_THING_ID);
    }
}
