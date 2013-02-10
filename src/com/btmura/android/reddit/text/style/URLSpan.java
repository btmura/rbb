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

package com.btmura.android.reddit.text.style;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.style.ClickableSpan;
import android.view.View;

import com.btmura.android.reddit.app.BrowserActivity;
import com.btmura.android.reddit.app.UserProfileActivity;
import com.btmura.android.reddit.net.UriHelper;

/**
 * Span that fires a {@link Intent#ACTION_VIEW} intent with a URI.
 */
public class URLSpan extends ClickableSpan {

    // This is pretty much the same as the platform URLSpan class but with
    // additional checking of the uri at click time and handling of
    // ActivityNotFoundExceptions.

    private final String url;

    public URLSpan(String url) {
        this.url = url;
    }

    public String getURL() {
        return url;
    }

    @Override
    public void onClick(View widget) {
        Context context = widget.getContext();
        Uri uri = Uri.parse(url);
        if (UriHelper.hasSubreddit(uri)) {
            startSubredditActivity(context, uri);
        } else if (UriHelper.hasUser(uri)) {
            startUserActivity(context, uri);
        } else {
            startBrowserActivity(context, uri);
        }
    }

    private void startSubredditActivity(Context context, Uri uri) {
        Intent intent = new Intent(context, BrowserActivity.class);
        intent.setData(uri);
        startActivity(context, intent);
    }

    private void startUserActivity(Context context, Uri uri) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.setData(uri);
        startActivity(context, intent);
    }

    private void startBrowserActivity(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        if (!startActivity(context, intent)) {
            // There was no activity found so try adding an http scheme if it
            // was missing and some authority is present.
            if (uri.getScheme() == null) {
                uri = uri.buildUpon().scheme("http").build();
                intent.setData(uri);
                startActivity(context, intent);
            }
        }
    }

    /**
     * Start an activity for the intent and return true on success or false if
     * no activity to handle the intent was found.
     */
    private boolean startActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
