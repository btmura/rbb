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

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;

import com.btmura.android.reddit.app.CommentListFragment;
import com.btmura.android.reddit.app.LinkFragment;
import com.btmura.android.reddit.database.Things;

public class ThingPagerAdapter extends FragmentStatePagerAdapter {

    public static final int TYPE_LINK = 0;
    public static final int TYPE_COMMENTS = 1;

    private final String accountName;
    private final Bundle thingBundle;

    public ThingPagerAdapter(FragmentManager fm, String accountName, Bundle thingBundle) {
        super(fm);
        this.accountName = accountName;
        this.thingBundle = thingBundle;
    }

    @Override
    public int getCount() {
        switch (Things.getKind(thingBundle)) {
            case Things.KIND_LINK:
                return Things.isSelf(thingBundle) ? 1 : 2;

            case Things.KIND_COMMENT:
                return 1;

            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public Fragment getItem(int position) {
        switch (getType(thingBundle, position)) {
            case TYPE_LINK:
                return LinkFragment.newInstance(Things.getUrl(thingBundle));

            case TYPE_COMMENTS:
                return CommentListFragment.newInstance(accountName, Things.getThingId(thingBundle),
                        Things.getLinkId(thingBundle));

            default:
                throw new IllegalStateException();
        }
    }

    public static int getType(Bundle thingBundle, int position) {
        switch (position) {
            case 0:
                return getFirstPageType(thingBundle);

            case 1:
                return TYPE_COMMENTS;

            default:
                throw new IllegalStateException();
        }
    }

    private static int getFirstPageType(Bundle thingBundle) {
        switch (Things.getKind(thingBundle)) {
            case Things.KIND_LINK:
                return Things.isSelf(thingBundle) ? TYPE_COMMENTS : TYPE_LINK;

            case Things.KIND_COMMENT:
                return TYPE_COMMENTS;

            default:
                throw new IllegalStateException();
        }
    }
}
