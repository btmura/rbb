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

package com.btmura.android.reddit.content;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.btmura.android.reddit.R;

public class MenuHelper {

    public static void startIntentChooser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_open)));
    }

    public static void setShareProvider(MenuItem shareItem, String label, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, label);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        ShareActionProvider provider = (ShareActionProvider) shareItem.getActionProvider();
        provider.setShareIntent(intent);
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
}
