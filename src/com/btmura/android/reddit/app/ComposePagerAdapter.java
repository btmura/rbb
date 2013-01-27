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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

import com.btmura.android.reddit.R;

public class ComposePagerAdapter extends FragmentStateItemPagerAdapter {

    private final Context context;
    private final int[] types;
    private final String subredditDestination;
    private final String messageDestination;
    private final String title;
    private final String text;
    private final boolean isReply;

    public ComposePagerAdapter(Context context, FragmentManager fm, int[] types,
            String subredditDestination, String messageDestination, String title, String text,
            boolean isReply) {
        super(fm);
        this.context = context.getApplicationContext();
        this.types = types;
        this.subredditDestination = subredditDestination;
        this.messageDestination = messageDestination;
        this.title = title;
        this.text = text;
        this.isReply = isReply;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (types[position]) {
            case ComposeActivity.TYPE_POST:
            case ComposeActivity.TYPE_EDIT_POST:
                return context.getString(R.string.compose_tab_post);

            case ComposeActivity.TYPE_MESSAGE:
                return context.getString(R.string.compose_tab_message);

            case ComposeActivity.TYPE_MESSAGE_REPLY:
            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_EDIT_COMMENT:
                return context.getString(R.string.compose_tab_comment);

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getCount() {
        return types.length;
    }

    @Override
    public Fragment getItem(int position) {
        return ComposeFormFragment.newInstance(types[position], subredditDestination,
                messageDestination, title, text, isReply, position);
    }

    public int getType(int position) {
        return types[position];
    }
}
