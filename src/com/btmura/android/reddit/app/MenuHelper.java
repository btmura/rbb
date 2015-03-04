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

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import com.btmura.android.reddit.content.Contexts;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.SubredditProvider;

public class MenuHelper {

    private static final String[] AUTHORITIES = {
            SubredditProvider.AUTHORITY,
    };

    /**
     * Sets a plain text {@link ClipData} with the provided label and text to the clipboard and
     * shows a toast with the text.
     */
    public static void copyUrl(Context context, CharSequence label, CharSequence text) {
        context = context.getApplicationContext();
        ClipboardManager cb =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void downloadUrl(Context context, String title, String url) {
        context = context.getApplicationContext();
        DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Request request = new Request(Uri.parse(url));
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(title);
        manager.enqueue(request);
    }

    public static void openAuthorizeUrl(Context context) {
        String clientId = context.getString(R.string.key_reddit_client_id);
        StringBuilder state = new StringBuilder("rbb_").append(System.currentTimeMillis());
        CharSequence url = Urls.authorize(clientId, state, Urls.OAUTH_REDIRECT_URL);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        Contexts.startActivity(context, intent);
    }

    public static void openUrl(Context context, CharSequence url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        Contexts.startActivity(context, makeChooser(context, intent, R.string.menu_open));
    }

    // TODO(btmura): add subject and title extras
    public static void share(Context context, CharSequence text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        Contexts.startActivity(context, makeChooser(context, intent, R.string.menu_share));
    }

    public static void shareImageUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
        Contexts.startActivity(context, intent);
    }

    public static String getSubredditTitle(Context context, String subreddit) {
        return context.getString(R.string.menu_subreddit, subreddit);
    }

    public static String getUserTitle(Context context, String user) {
        return context.getString(R.string.menu_user, user);
    }

    public static boolean isUserItemVisible(String user) {
        return !TextUtils.isEmpty(user) && !Things.DELETED_AUTHOR.equals(user);
    }

    public static void showAddSubredditDialog(FragmentManager fm, String subreddit) {
        AddSubredditFragment.newInstance(subreddit).show(fm, AddSubredditFragment.TAG);
    }

    public static void showAddSubredditDialog(FragmentManager fm, String[] subreddits) {
        AddSubredditFragment.newInstance(subreddits).show(fm, AddSubredditFragment.TAG);
    }

    public static void showSortCommentsDialog(Context context, final Filterable filterable) {
        showSortDialog(context, filterable, R.string.menu_sort_comments, R.array.filters_comments);
    }

    public static void showSortSearchThingsDialog(Context context, final Filterable filterable) {
        showSortDialog(context, filterable, R.string.menu_sort_results, R.array.filters_search);
    }

    private static void showSortDialog(Context context,
            final Filterable filterable,
            int titleResId,
            int itemArrayResId) {
        new AlertDialog.Builder(context)
                .setTitle(titleResId)
                .setSingleChoiceItems(itemArrayResId,
                        filterable.getFilter(),
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                filterable.setFilter(which);
                            }
                        })
                .show();
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
        intent.setData(Uri.parse(Urls.subreddit(subreddit, -1, Urls.TYPE_HTML).toString()));
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

    private static Intent makeChooser(Context context, Intent intent, int titleResId) {
        return Intent.createChooser(intent, context.getString(titleResId));
    }
}
