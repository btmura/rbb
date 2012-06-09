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

import java.util.ArrayList;
import java.util.Collections;

import android.util.LruCache;

import com.btmura.android.reddit.entity.Subreddit;

class AccountDataCache extends LruCache<Long, ArrayList<Subreddit>> {

    AccountDataCache(int size) {
        super(size);
    }

    public void addSubreddit(long accountId, String name) {
        ArrayList<Subreddit> subreddits = get(accountId);
        if (subreddits == null) {
            return;
        }
        
        synchronized (subreddits) {
            int count = subreddits.size();
            for (int i = 0; i < count; i++) {
                String subreddit = subreddits.get(i).name;
                if (name.equalsIgnoreCase(subreddit)) {
                    return;
                }
            }
            
            subreddits.add(Subreddit.newInstance(name));
            Collections.sort(subreddits);
        }
    }
    
    public void deleteSubreddit(long accountId, String name) {
        ArrayList<Subreddit> subreddits = get(accountId);
        if (subreddits == null) {
            return;
        }
        
        synchronized (subreddits) {
            int count = subreddits.size();
            for (int i = 0; i < count; i++) {
                String subreddit = subreddits.get(i).name;
                if (name.equalsIgnoreCase(subreddit)) {
                    subreddits.remove(i);
                    return;
                }
            }
        }
    }
}
