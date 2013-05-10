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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.util.StringUtil;

// TODO: Split this into an interface and 2 adapters.
public class ThingListAdapter extends BaseLoaderAdapter {

    public static final String TAG = "ThingListAdapter";

    private static final String[] THING_PROJECTION = {
            Things._ID,
            Things.COLUMN_AUTHOR,
            Things.COLUMN_BODY,
            Things.COLUMN_CREATED_UTC,
            Things.COLUMN_DOMAIN,
            Things.COLUMN_DOWNS,
            Things.COLUMN_HIDDEN,
            Things.COLUMN_KIND,
            Things.COLUMN_LIKES,
            Things.COLUMN_LINK_ID,
            Things.COLUMN_LINK_TITLE,
            Things.COLUMN_NUM_COMMENTS,
            Things.COLUMN_OVER_18,
            Things.COLUMN_PERMA_LINK,
            Things.COLUMN_SAVED,
            Things.COLUMN_SCORE,
            Things.COLUMN_SELF,
            Things.COLUMN_SUBREDDIT,
            Things.COLUMN_TITLE,
            Things.TABLE_NAME + "." + Things.COLUMN_THING_ID,
            Things.COLUMN_THUMBNAIL_URL,
            Things.COLUMN_UPS,
            Things.COLUMN_URL,

            // Following columns are from joined tables at the end.
            SharedColumns.COLUMN_LOCAL_SAVED,
            SharedColumns.COLUMN_LOCAL_VOTE,
            SharedColumns.COLUMN_LOCAL_HIDDEN,
    };

    private static final int THING_AUTHOR = 1;
    private static final int THING_BODY = 2;
    private static final int THING_CREATED_UTC = 3;
    private static final int THING_DOMAIN = 4;
    private static final int THING_DOWNS = 5;
    private static final int THING_HIDDEN = 6;
    private static final int THING_KIND = 7;
    private static final int THING_LIKES = 8;
    private static final int THING_LINK_ID = 9;
    private static final int THING_LINK_TITLE = 10;
    private static final int THING_NUM_COMMENTS = 11;
    private static final int THING_OVER_18 = 12;
    private static final int THING_PERMA_LINK = 13;
    private static final int THING_SAVED = 14;
    private static final int THING_SCORE = 15;
    private static final int THING_SELF = 16;
    private static final int THING_SUBREDDIT = 17;
    private static final int THING_TITLE = 18;
    private static final int THING_THING_ID = 19;
    private static final int THING_THUMBNAIL_URL = 20;
    private static final int THING_UPS = 21;
    private static final int THING_URL = 22;

    // Following columns are from joined tables at the end.
    private static final int THING_SAVE_ACTION = 22;
    private static final int THING_VOTE = 23;
    private static final int THING_LOCAL_HIDDEN = 24;

    private static final String[] MESSAGE_PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CONTEXT,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_DESTINATION,
            Messages.COLUMN_KIND,
            Messages.COLUMN_NEW,
            Messages.COLUMN_SUBJECT,
            Messages.COLUMN_SUBREDDIT,
            Messages.COLUMN_THING_ID,
            Messages.COLUMN_WAS_COMMENT,

            // Following columns are from joined tables.
            MessageActions.COLUMN_ACTION,
    };

    private static final int MESSAGE_AUTHOR = 1;
    private static final int MESSAGE_BODY = 2;
    private static final int MESSAGE_CONTEXT = 3;
    private static final int MESSAGE_CREATED_UTC = 4;
    private static final int MESSAGE_DESTINATION = 5;
    private static final int MESSAGE_KIND = 6;
    private static final int MESSAGE_NEW = 7;
    private static final int MESSAGE_SUBJECT = 8;
    private static final int MESSAGE_SUBREDDIT = 9;
    private static final int MESSAGE_THING_ID = 10;
    private static final int MESSAGE_WAS_COMMENT = 11;
    private static final int MESSAGE_ACTION = 12;

    private static final int[] LINK_DETAILS = {
            ThingView.DETAIL_UP_VOTES,
            ThingView.DETAIL_DOWN_VOTES,
            ThingView.DETAIL_DOMAIN,
    };

    private static final int[] COMMENT_DETAILS = {
            ThingView.DETAIL_UP_VOTES,
            ThingView.DETAIL_DOWN_VOTES,
            ThingView.DETAIL_SUBREDDIT,
    };

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

    private long sessionId = -1;
    private String accountName;
    private String parentSubreddit;
    private String subreddit;
    private final String query;
    private final String profileUser;
    private final String messageUser;
    private final int filter;
    private String more;

    private final Formatter formatter = new Formatter();
    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final OnVoteListener listener;
    private final boolean singleChoice;
    private int thingBodyWidth;
    private String selectedThingId;
    private String selectedLinkId;
    private long nowTimeMs;

    public ThingListAdapter(Context context, String subreddit, String query, String profileUser,
            String messageUser, int filter, OnVoteListener listener, boolean singleChoice) {
        super(context, null, 0);
        this.subreddit = subreddit;
        this.query = query;
        this.profileUser = profileUser;
        this.messageUser = messageUser;
        this.filter = filter;
        this.listener = listener;
        this.singleChoice = singleChoice;
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    @Override
    public boolean isLoadable() {
        return accountName != null &&
                (subreddit != null || query != null || profileUser != null || messageUser != null);
    }

    @Override
    protected Uri getLoaderUri() {
        if (isProfileActivity()) {
            return ThingProvider.profileUri(sessionId, accountName, profileUser, filter, more);
        } else if (isMessageActivity()) {
            return ThingProvider.messageUri(sessionId, accountName, filter, more);
        } else if (isSearchActivity()) {
            return ThingProvider.searchUri(sessionId, accountName, subreddit, query);
        } else if (isBrowserActivity()) {
            return ThingProvider.subredditUri(sessionId, accountName, subreddit, filter, more);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected String[] getProjection() {
        return isMessageActivity() ? MESSAGE_PROJECTION : THING_PROJECTION;
    }

    @Override
    protected String getSelection() {
        if (isMessageActivity()) {
            return null;
        } else if (isProfileHiddenTab()) {
            return HideActions.SELECT_HIDDEN_BY_JOIN;
        } else {
            return HideActions.SELECT_UNHIDDEN_BY_JOIN;
        }
    }

    @Override
    public void deleteSessionData(Context context) {
        Uri uri = isMessageActivity() ? ThingProvider.MESSAGES_URI : ThingProvider.THINGS_URI;
        Provider.deleteSessionAsync(context, uri, sessionId);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        switch (getKind(position)) {
            case Kinds.KIND_MORE:
                return 0;

            default:
                return 1;
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        nowTimeMs = System.currentTimeMillis();
        return super.swapCursor(newCursor);
    }

    @Override
    public boolean isEnabled(int position) {
        return getKind(position) != Kinds.KIND_MORE;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        switch (getKind(cursor.getPosition())) {
            case Kinds.KIND_MORE:
                LayoutInflater inflater = LayoutInflater.from(context);
                return inflater.inflate(R.layout.thing_more_row, parent, false);

            default:
                return new ThingView(context);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof ThingView) {
            if (isMessageActivity()) {
                bindMessageThingView(view, context, cursor);
            } else {
                bindThingView(view, context, cursor);
            }
        }
    }

    private void bindMessageThingView(View view, Context context, Cursor cursor) {
        final String author = cursor.getString(MESSAGE_AUTHOR);
        final String body = cursor.getString(MESSAGE_BODY);
        final long createdUtc = cursor.getLong(MESSAGE_CREATED_UTC);
        final String destination = cursor.getString(MESSAGE_DESTINATION);
        final String domain = null; // No domain for messages.
        final int downs = 0; // No downs for messages.
        final boolean expanded = true; // Messages are always expanded.
        final int kind = cursor.getInt(MESSAGE_KIND);
        final int likes = 0; // No likes for messages.
        final boolean isNew = isNew(cursor.getPosition());
        final int nesting = 0; // No nesting for messages.
        final int numComments = 0; // No comments for messages.
        final boolean over18 = false; // No over18 for messages.
        final int score = 0; // No score for messages.
        final String subject = cursor.getString(MESSAGE_SUBJECT);
        final String subreddit = cursor.getString(MESSAGE_SUBREDDIT);
        final String thingId = cursor.getString(MESSAGE_THING_ID);
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
        tv.setOnVoteListener(listener);
        setThingDetails(tv, kind);
    }

    private void bindThingView(View view, Context context, Cursor cursor) {
        final String author = cursor.getString(THING_AUTHOR);
        final String body = cursor.getString(THING_BODY);
        final long createdUtc = cursor.getLong(THING_CREATED_UTC);
        final String destination = null; // Only messages have destinations.
        final String domain = cursor.getString(THING_DOMAIN);
        final int downs = cursor.getInt(THING_DOWNS);
        final boolean expanded = true; // Expanded only for comments handled by different adapter.
        final int kind = cursor.getInt(THING_KIND);
        final String linkId = cursor.getString(THING_LINK_ID);
        final String linkTitle = cursor.getString(THING_LINK_TITLE);
        final int nesting = 0; // Nesting only for comments handled by different adapter.
        final int numComments = cursor.getInt(THING_NUM_COMMENTS);
        final boolean over18 = cursor.getInt(THING_OVER_18) == 1;
        final String subreddit = cursor.getString(THING_SUBREDDIT);
        final String thingId = cursor.getString(THING_THING_ID);
        final String thumbnailUrl = cursor.getString(THING_THUMBNAIL_URL);
        final String title = cursor.getString(THING_TITLE);
        final int ups = cursor.getInt(THING_UPS);

        // Comments don't have a score so calculate our own.
        int score = cursor.getInt(THING_SCORE);
        if (kind == Kinds.KIND_COMMENT) {
            score = ups - downs;
        }

        // Reconcile local and remote votes.
        int likes = cursor.getInt(THING_LIKES);
        if (!cursor.isNull(THING_VOTE)) {
            // Local votes take precedence over those from reddit.
            likes = cursor.getInt(THING_VOTE);

            // Modify the score since the vote is still pending and don't go
            // below 0 since reddit doesn't seem to do that.
            score = Math.max(0, score + likes);
        }

        final boolean drawVotingArrows = AccountUtils.isAccount(accountName)
                && kind != Kinds.KIND_MESSAGE;
        final boolean showThumbnail = !TextUtils.isEmpty(thumbnailUrl);
        final boolean showStatusPoints = !AccountUtils.isAccount(accountName)
                || kind == Kinds.KIND_COMMENT;

        ThingView tv = (ThingView) view;
        tv.setBody(body, false, formatter);
        tv.setData(accountName,
                author,
                createdUtc,
                destination,
                domain,
                downs,
                expanded,
                kind,
                likes,
                linkTitle,
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
        tv.setChosen(singleChoice
                && Objects.equals(selectedThingId, thingId)
                && Objects.equals(selectedLinkId, linkId));
        tv.setOnVoteListener(listener);
        setThingDetails(tv, kind);
        thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
    }

    private void setThingDetails(ThingView tv, int kind) {
        switch (kind) {
            case Kinds.KIND_LINK:
                tv.setDetails(LINK_DETAILS);
                break;

            case Kinds.KIND_COMMENT:
                tv.setDetails(isMessageActivity() ? MESSAGE_COMMENT_DETAILS : COMMENT_DETAILS);
                break;

            case Kinds.KIND_MESSAGE:
                tv.setDetails(MESSAGE_DETAILS);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public Bundle getThingBundle(Context context, int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            if (isMessageActivity()) {
                return makeMessageThingBundle(context, c);
            } else {
                return makeThingBundle(context, c);
            }
        }
        return null;
    }

    private Bundle makeMessageThingBundle(Context context, Cursor c) {
        Bundle b = new Bundle(7);
        ThingBundle.putAuthor(b, c.getString(MESSAGE_AUTHOR));
        ThingBundle.putSubreddit(b, c.getString(MESSAGE_SUBREDDIT));
        ThingBundle.putKind(b, c.getInt(MESSAGE_KIND));

        // Messages don't have titles so use the body for both.
        String body = c.getString(MESSAGE_BODY);
        ThingBundle.putTitle(b, body);

        ThingBundle.putThingId(b, c.getString(MESSAGE_THING_ID));

        String contextUrl = c.getString(MESSAGE_CONTEXT);
        if (!TextUtils.isEmpty(contextUrl)) {
            // If there is a context url, then we have to parse that url to grab
            // the link id embedded inside of it like:
            //
            // /r/rbb/comments/13ejyf/testing_from_laptop/c738opg?context=3
            String[] parts = contextUrl.split("/");
            if (parts != null && parts.length >= 5) {
                ThingBundle.putLinkId(b, parts[4]);
            }
        }

        // If this message isn't a comment, then it's simply a message with no
        // comments or links.
        ThingBundle.putNoComments(b, c.getInt(MESSAGE_WAS_COMMENT) == 0);

        return b;
    }

    private Bundle makeThingBundle(Context context, Cursor c) {
        Bundle b = new Bundle(8);
        ThingBundle.putAuthor(b, c.getString(THING_AUTHOR));
        ThingBundle.putSubreddit(b, c.getString(THING_SUBREDDIT));
        ThingBundle.putKind(b, c.getInt(THING_KIND));

        String title = c.getString(THING_TITLE);
        ThingBundle.putTitle(b, !TextUtils.isEmpty(title)
                ? format(context, title)
                : format(context, c.getString(THING_LINK_TITLE)));

        String thingId = c.getString(THING_THING_ID);
        ThingBundle.putThingId(b, thingId);

        String linkId = c.getString(THING_LINK_ID);
        ThingBundle.putLinkId(b, linkId);

        boolean isSelf = c.getInt(THING_SELF) == 1;
        if (!isSelf) {
            ThingBundle.putLinkUrl(b, c.getString(THING_URL));
        }

        String permaLink = c.getString(THING_PERMA_LINK);
        if (!TextUtils.isEmpty(permaLink)) {
            ThingBundle.putCommentUrl(b, Urls.perma(permaLink, null));
        }

        return b;
    }

    private String format(Context context, String text) {
        return StringUtil.safeString(formatter.formatAll(context, text));
    }

    public boolean isHidable(Context context, int position, boolean hide) {
        return (isBrowserActivity() || isSearchActivity() || isProfileHiddenTab())
                && AccountUtils.isAccount(accountName)
                && getKind(position) == Kinds.KIND_LINK
                && (hide && isUnhidden(position) || !hide && isHidden(position));
    }

    private boolean isHidden(int position) {
        return !isNull(position, THING_LOCAL_HIDDEN)
                && getInt(position, THING_LOCAL_HIDDEN) == HideActions.ACTION_HIDE
                || getBoolean(position, THING_HIDDEN);
    }

    private boolean isUnhidden(int position) {
        return !isNull(position, THING_LOCAL_HIDDEN)
                && getInt(position, THING_LOCAL_HIDDEN) == HideActions.ACTION_UNHIDE
                || !getBoolean(position, THING_HIDDEN);
    }

    public void hide(Context context, int position, boolean hide) {
        if (isMessageActivity()) {
            throw new IllegalStateException();
        }

        String thingId = getThingId(position);
        if (hide) {
            Provider.hideAsync(context, accountName, thingId,
                    getString(position, THING_AUTHOR),
                    getLong(position, THING_CREATED_UTC),
                    getString(position, THING_DOMAIN),
                    getInt(position, THING_DOWNS),
                    getInt(position, THING_LIKES),
                    getInt(position, THING_NUM_COMMENTS),
                    getBoolean(position, THING_OVER_18),
                    getString(position, THING_PERMA_LINK),
                    getInt(position, THING_SCORE),
                    getBoolean(position, THING_SELF),
                    getString(position, THING_SUBREDDIT),
                    getString(position, THING_TITLE),
                    getString(position, THING_THUMBNAIL_URL),
                    getInt(position, THING_UPS),
                    getString(position, THING_URL));
        } else {
            Provider.unhideAsync(context, accountName, thingId);
        }
    }

    public void save(Context context, int position) {
        if (isMessageActivity()) {
            throw new IllegalStateException();
        }

        Provider.saveAsync(context, accountName, getThingId(position),
                getString(position, THING_AUTHOR),
                getLong(position, THING_CREATED_UTC),
                getString(position, THING_DOMAIN),
                getInt(position, THING_DOWNS),
                getInt(position, THING_LIKES),
                getInt(position, THING_NUM_COMMENTS),
                getBoolean(position, THING_OVER_18),
                getString(position, THING_PERMA_LINK),
                getInt(position, THING_SCORE),
                getBoolean(position, THING_SELF),
                getString(position, THING_SUBREDDIT),
                getString(position, THING_TITLE),
                getString(position, THING_THUMBNAIL_URL),
                getInt(position, THING_UPS),
                getString(position, THING_URL));
    }

    public void unsave(Context context, int position) {
        if (isMessageActivity()) {
            throw new IllegalStateException();
        }

        Provider.unsaveAsync(context, accountName, getThingId(position));
    }

    public void vote(Context context, int position, int action) {
        if (isMessageActivity()) {
            throw new IllegalStateException();
        }

        Provider.voteAsync(context, accountName, action,
                getString(position, THING_AUTHOR),
                getLong(position, THING_CREATED_UTC),
                getString(position, THING_DOMAIN),
                getInt(position, THING_DOWNS),
                getInt(position, THING_LIKES),
                getInt(position, THING_NUM_COMMENTS),
                getBoolean(position, THING_OVER_18),
                getString(position, THING_PERMA_LINK),
                getInt(position, THING_SCORE),
                getBoolean(position, THING_SELF),
                getString(position, THING_SUBREDDIT),
                getThingId(position),
                getString(position, THING_TITLE),
                getString(position, THING_THUMBNAIL_URL),
                getInt(position, THING_UPS),
                getString(position, THING_URL));
    }

    public String getAuthor(int position) {
        return getString(position, getAuthorIndex());
    }

    public int getKind(int position) {
        return getInt(position, getKindIndex());
    }

    public Bundle getReplyExtras(int position) {
        return null;
    }

    public String getThingId(int position) {
        return getString(position, getThingIdIndex());
    }

    public String getLinkId(int position) {
        if (isMessageActivity()) {
            return null; // Messages don't have link ids.
        } else {
            return getString(position, THING_LINK_ID);
        }
    }

    public String getNextMore() {
        return findNextMore(getKindIndex(), getThingIdIndex());
    }

    private String findNextMore(int kindIndex, int thingIdIndex) {
        Cursor c = getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(kindIndex) == Kinds.KIND_MORE) {
                return c.getString(thingIdIndex);
            }
        }
        return null;
    }

    public boolean isNew(int position) {
        // Only messages can't be marked as new.
        if (!isMessageActivity()) {
            return false;
        }

        // If no local read actions are pending, then rely on what reddit
        // thinks.
        if (isNull(position, MESSAGE_ACTION)) {
            return getBoolean(position, MESSAGE_NEW);
        }

        // We have a local pending action so use that to indicate if it's new.
        return getInt(position, MESSAGE_ACTION) == ReadActions.ACTION_UNREAD;
    }

    public int getNumComments(int position) {
        return isMessageActivity() ? -1 : getInt(position, THING_NUM_COMMENTS);
    }

    public boolean isSaved(int position) {
        // Messages can't be saved.
        if (isMessageActivity()) {
            return false;
        }

        // If no local save actions are pending, then rely on server info.
        if (isNull(position, THING_SAVE_ACTION)) {
            return getBoolean(position, THING_SAVED);
        }

        // We have a local pending action so use that to indicate if it's read.
        return getInt(position, THING_SAVE_ACTION) == SaveActions.ACTION_SAVE;
    }

    public String getSubreddit(int position) {
        return getString(position, getSubredditIndex());
    }

    public String getTitle(int position) {
        if (isMessageActivity()) {
            return getString(position, MESSAGE_SUBJECT);
        } else {
            // Link and comment posts have a title.
            String title = getString(position, THING_TITLE);
            if (!TextUtils.isEmpty(title)) {
                return title;
            }

            // CommentActions don't have titles so use the body.
            return getString(position, THING_BODY);
        }
    }

    public CharSequence getUrl(int position) {
        if (isMessageActivity()) {
            return getMessageUrl(position);
        } else {
            return getThingUrl(position);
        }
    }

    private CharSequence getMessageUrl(int position) {
        // Comment reply messages have a context url we can use.
        String context = getString(position, MESSAGE_CONTEXT);
        if (!TextUtils.isEmpty(context)) {
            return Urls.perma(context, null);
        }

        // Assume this is a raw message.
        return Urls.messageThread(getThingId(position), Urls.TYPE_HTML);
    }

    private CharSequence getThingUrl(int position) {
        // Most things and comments have the url attribute set.
        String url = getString(position, THING_URL);
        if (!TextUtils.isEmpty(url)) {
            return url;
        }

        // Comment references just provide a thing and link id.
        String thingId = getThingId(position);
        String linkId = getLinkId(position);
        return Urls.commentListing(thingId, linkId, -1, Urls.TYPE_HTML);
    }

    private int getAuthorIndex() {
        return isMessageActivity() ? MESSAGE_AUTHOR : THING_AUTHOR;
    }

    private int getKindIndex() {
        return isMessageActivity() ? MESSAGE_KIND : THING_KIND;
    }

    private int getSubredditIndex() {
        return isMessageActivity() ? MESSAGE_SUBREDDIT : THING_SUBREDDIT;
    }

    private int getThingIdIndex() {
        return isMessageActivity() ? MESSAGE_THING_ID : THING_THING_ID;
    }

    public String getSelectedThingId() {
        return selectedThingId;
    }

    public String getSelectedLinkId() {
        return selectedLinkId;
    }

    public void setSelectedThing(String thingId, String linkId) {
        if (!Objects.equals(selectedThingId, thingId)
                || !Objects.equals(selectedLinkId, linkId)) {
            selectedThingId = thingId;
            selectedLinkId = linkId;
            notifyDataSetChanged();
        }
    }

    public void setSelectedPosition(int position) {
        setSelectedThing(getThingId(position), getLinkId(position));
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getParentSubreddit() {
        return parentSubreddit;
    }

    public void setParentSubreddit(String parentSubreddit) {
        this.parentSubreddit = parentSubreddit;
    }

    public String getQuery() {
        return query;
    }

    public int getFilterValue() {
        return filter;
    }

    public String getMore() {
        return more;
    }

    public void setMore(String more) {
        this.more = more;
    }

    public boolean isSingleChoice() {
        return singleChoice;
    }

    private boolean isProfileActivity() {
        return !TextUtils.isEmpty(profileUser);
    }

    private boolean isProfileHiddenTab() {
        return isProfileActivity() && filter == FilterAdapter.PROFILE_HIDDEN;
    }

    private boolean isMessageActivity() {
        return !TextUtils.isEmpty(messageUser);
    }

    private boolean isSearchActivity() {
        return !TextUtils.isEmpty(query);
    }

    private boolean isBrowserActivity() {
        return subreddit != null; // Empty but non-null subreddit means front page.
    }

}
