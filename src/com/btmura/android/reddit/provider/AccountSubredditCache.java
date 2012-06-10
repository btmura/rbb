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
import java.util.Collections;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import com.btmura.android.reddit.entity.Subreddit;

class AccountSubredditCache {

    public static final String TAG = "AccountSubredditCache";

    private static final long EXPIRE_DURATION = 60 * 1000;

    private final InternalCache cache = new InternalCache(2);

    public ArrayList<Subreddit> query(long accountId, String cookie) {
        Entry entry = null;

        synchronized (cache) {
            entry = cache.get(accountId);
            if (entry == null) {
                entry = new Entry();
                cache.put(accountId, entry);
            }
        }

        synchronized (entry) {
            if (entry.hasData() && !entry.isExpired()) {
                return entry.subreddits;
            }

            entry.subreddits = querySubreddits(cookie);
            if (entry.subreddits != null) {
                entry.modTime = System.currentTimeMillis();
                return entry.subreddits;
            } else {
                entry.resetData();
                return null;
            }
        }
    }

    static ArrayList<Subreddit> querySubreddits(String cookie) {
        try {
            return NetApi.query(cookie);
        } catch (IOException e) {
            Log.e(TAG, "querySubreddits", e);
        }
        return null;
    }

    public boolean insert(Context context, long accountId, String name) {
        Entry entry = cache.get(accountId);
        if (entry == null) {
            return false;
        }

        synchronized (entry) {
            ArrayList<Subreddit> subreddits = entry.subreddits;
            int count = subreddits.size();
            for (int i = 0; i < count; i++) {
                String subreddit = subreddits.get(i).name;
                if (name.equalsIgnoreCase(subreddit)) {
                    return false;
                }
            }

            subreddits.add(Subreddit.newInstance(name));
            Collections.sort(subreddits);
            return true;
        }
    }

    public boolean delete(long accountId, String name) {
        Entry entry = cache.get(accountId);
        if (entry == null) {
            return false;
        }

        synchronized (entry) {
            ArrayList<Subreddit> subreddits = entry.subreddits;
            int count = subreddits.size();
            for (int i = 0; i < count; i++) {
                String subreddit = subreddits.get(i).name;
                if (name.equalsIgnoreCase(subreddit)) {
                    subreddits.remove(i);
                    return true;
                }
            }
        }

        return false;
    }

    static class InternalCache extends LruCache<Long, Entry> {
        InternalCache(int size) {
            super(size);
        }
    }

    static class Entry {
        long modTime;
        ArrayList<Subreddit> subreddits;

        boolean hasData() {
            return modTime != 0;
        }

        void resetData() {
            modTime = 0;
            subreddits.clear();
        }

        boolean isExpired() {
            long now = System.currentTimeMillis();
            return now - modTime > EXPIRE_DURATION;
        }
    }

}
