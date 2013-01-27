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

import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
        return !Things.DELETED.equals(user);
    }

    /**
     * Sets a plain text {@link ClipData} with the provided label and text to
     * the clipboard and shows a toast with the text.
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

    public static void startAccountListActivity(Context context) {
        Intent intent = new Intent(context, AccountListActivity.class);
        context.startActivity(intent);
    }

    public static void startAddAccountActivity(Context context) {
        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, AUTHORITIES);
        context.startActivity(intent);
    }

    public static void startComposeActivity(Context context, int[] types,
            String subredditDestination, String messageDestination, String title, Bundle extras,
            boolean isReply) {
        Intent intent = new Intent(context, ComposeActivity.class);
        intent.putExtra(ComposeActivity.EXTRA_TYPES, types);
        intent.putExtra(ComposeActivity.EXTRA_SUBREDDIT_DESTINATION, subredditDestination);
        intent.putExtra(ComposeActivity.EXTRA_MESSAGE_DESTINATION, messageDestination);
        intent.putExtra(ComposeActivity.EXTRA_TITLE, title);
        intent.putExtra(ComposeActivity.EXTRA_IS_REPLY, isReply);
        intent.putExtra(ComposeActivity.EXTRA_EXTRAS, extras);
        context.startActivity(intent);
    }

    public static void startIntentChooser(Context context, CharSequence url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_open)));
    }

    public static void startMessageActivity(Context context, String user, int filter) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra(MessageActivity.EXTRA_USER, user);
        if (filter != -1) {
            intent.putExtra(MessageActivity.EXTRA_FILTER, filter);
        }
        context.startActivity(intent);
    }

    public static void startProfileActivity(Context context, String user, int filter) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.setData(Uri.parse(Urls.user(user, filter, null, Urls.TYPE_HTML).toString()));
        context.startActivity(intent);
    }

    public static void startSelfProfileActivity(Context context, String user, int filter) {
        Intent intent = new Intent(context, SelfProfileActivity.class);
        intent.putExtra(MessageActivity.EXTRA_USER, user);
        if (filter != -1) {
            intent.putExtra(MessageActivity.EXTRA_FILTER, filter);
        }
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
}
