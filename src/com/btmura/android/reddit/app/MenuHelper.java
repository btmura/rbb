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

package com.btmura.android.reddit.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.SubredditProvider;

public class MenuHelper {

    private static final String[] AUTHORITIES = {
            SubredditProvider.AUTHORITY,
    };

    public static String getSubredditTitle(Context context, String subreddit) {
        return context.getString(R.string.menu_subreddit, subreddit);
    }

    public static String getUserTitle(Context context, String user) {
        return context.getString(R.string.menu_user, user);
    }

    public static boolean isUserItemVisible(String user) {
        return !TextUtils.isEmpty(user) && !Things.DELETED_AUTHOR.equals(user);
    }

    public static void openUrl(Context context, CharSequence url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        context.startActivity(intent);
    }

    /**
     * Sets a plain text {@link ClipData} with the provided label and text to the clipboard and
     * shows a toast with the text.
     */
    public static void setClipAndToast(Context context, CharSequence label, CharSequence text) {
        context = context.getApplicationContext();
        ClipboardManager cb = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void setShareProvider(MenuItem shareItem, String label, CharSequence text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, label);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        ShareActionProvider provider = (ShareActionProvider) shareItem.getActionProvider();
        provider.setShareIntent(intent);
    }

    public static void showAddSubredditDialog(FragmentManager fm, String subreddit) {
        AddSubredditFragment.newInstance(subreddit).show(fm, AddSubredditFragment.TAG);
    }

    public static void showAddSubredditDialog(FragmentManager fm, String[] subreddits) {
        AddSubredditFragment.newInstance(subreddits).show(fm, AddSubredditFragment.TAG);
    }

    public static void startAccountListActivity(Context context) {
        context.startActivity(new Intent(context, AccountListActivity.class));
    }

    public static void startAddAccountActivity(Context context) {
        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, AUTHORITIES);
        context.startActivity(intent);
    }

    public static void startContentBrowserActivity(Context context) {
        context.startActivity(new Intent(context, ContentBrowserActivity.class));
    }

    public static void startIntentChooser(Context context, CharSequence url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_open)));
    }

    public static void startProfileActivity(Context context, String user, int filter) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.setData(Uri.parse(Urls.user(user, filter, null, Urls.TYPE_HTML).toString()));
        context.startActivity(intent);
    }

    public static void startSidebarActivity(Context context, String subreddit) {
        Intent intent = new Intent(context, SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, subreddit);
        context.startActivity(intent);
    }

    public static void startSubredditActivity(Context context, String subreddit) {
        // TODO: Remove duplication with SubredditSpan.
        Intent intent = new Intent(context, BrowserActivity.class);
        intent.setData(Uri.parse(Urls.subreddit(subreddit, -1, null, Urls.TYPE_HTML).toString()));
        context.startActivity(intent);
    }

    // Helper methods to start composer activities

    public static void startCommentReplyComposer(Context context,
            String accountName,
            String messageDestination,
            String title,
            String parentThingId,
            String thingId) {
        Bundle extras = new Bundle(2);
        extras.putString(ComposeActivity.EXTRA_COMMENT_PARENT_THING_ID, parentThingId);
        extras.putString(ComposeActivity.EXTRA_COMMENT_THING_ID, thingId);
        startComposeActivity(context,
                accountName,
                ComposeActivity.COMMENT_REPLY_TYPE_SET,
                null,
                messageDestination,
                title,
                null,
                extras,
                true);
    }

    public static void startMessageReplyComposer(Context context,
            String accountName,
            String messageDestination,
            String title,
            String parentThingId,
            String thingId,
            boolean isReply) {
        Bundle extras = new Bundle(1);
        extras.putString(ComposeActivity.EXTRA_MESSAGE_PARENT_THING_ID, parentThingId);
        extras.putString(ComposeActivity.EXTRA_MESSAGE_THING_ID, thingId);
        startComposeActivity(context,
                accountName,
                ComposeActivity.MESSAGE_REPLY_TYPE_SET,
                null,
                messageDestination,
                title,
                null,
                extras,
                isReply);
    }

    public static void startEditCommentComposer(Context context,
            String accountName,
            String title,
            String text,
            String parentThingId,
            String thingId) {
        Bundle extras = new Bundle(2);
        extras.putString(ComposeActivity.EXTRA_EDIT_PARENT_THING_ID, parentThingId);
        extras.putString(ComposeActivity.EXTRA_EDIT_THING_ID, thingId);
        startComposeActivity(context,
                accountName,
                ComposeActivity.EDIT_COMMENT_TYPE_SET,
                null,
                null,
                title,
                text,
                extras,
                false);
    }

    public static void startEditPostComposer(Context context,
            String accountName,
            String title,
            String text,
            String parentThingId,
            String thingId) {
        Bundle extras = new Bundle(2);
        extras.putString(ComposeActivity.EXTRA_EDIT_PARENT_THING_ID, parentThingId);
        extras.putString(ComposeActivity.EXTRA_EDIT_THING_ID, thingId);
        startComposeActivity(context,
                accountName,
                ComposeActivity.EDIT_POST_TYPE_SET,
                null,
                null,
                title,
                text,
                extras,
                false);
    }

    public static void startNewMessageComposer(Context context,
            String accountName,
            String messageDestination) {
        startComposeActivity(context,
                accountName,
                ComposeActivity.MESSAGE_TYPE_SET,
                null,
                messageDestination,
                null,
                null,
                null,
                false);
    }

    public static void startNewPostComposer(Context context,
            String accountName,
            String subreddit) {
        startComposeActivity(context,
                accountName,
                ComposeActivity.DEFAULT_TYPE_SET,
                subreddit,
                null,
                null,
                null,
                null,
                false);
    }

    private static void startComposeActivity(Context context,
            String accountName,
            int[] types,
            String subredditDestination,
            String messageDestination,
            String title,
            String text,
            Bundle extras,
            boolean isReply) {
        Intent intent = new Intent(context, ComposeActivity.class);
        intent.putExtra(ComposeActivity.EXTRA_ACCOUNT_NAME, accountName);
        intent.putExtra(ComposeActivity.EXTRA_TYPES, types);
        intent.putExtra(ComposeActivity.EXTRA_SUBREDDIT_DESTINATION, subredditDestination);
        intent.putExtra(ComposeActivity.EXTRA_MESSAGE_DESTINATION, messageDestination);
        intent.putExtra(ComposeActivity.EXTRA_TITLE, title);
        intent.putExtra(ComposeActivity.EXTRA_TEXT, text);
        intent.putExtra(ComposeActivity.EXTRA_IS_REPLY, isReply);
        intent.putExtra(ComposeActivity.EXTRA_EXTRAS, extras);
        context.startActivity(intent);
    }

}
