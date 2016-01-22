/*
 * Copyright (C) 2013 Brian Muramatsu
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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.provider.ThingProvider;

/**
 * Loader that loads a subreddit's things.
 */
public class SubredditThingLoader extends AbstractSessionLoader
    implements ThingProjection {

  private final String accountName;
  private final String subreddit;
  private final int filter;

  public SubredditThingLoader(
      Context ctx,
      String accountName,
      String subreddit,
      int filter,
      @Nullable String more,
      int count,
      Bundle sessionData) {
    super(ctx,
        ThingProvider.THINGS_URI,
        PROJECTION,
        Things.SELECT_NOT_HIDDEN_BY_SESSION_ID,
        NO_SORT,
        more,
        count,
        sessionData);
    this.accountName = accountName;
    this.subreddit = subreddit;
    this.filter = filter;
  }

  @Override
  protected Bundle getSession(Bundle sessionData, String more, int count) {
    return ThingProvider.getSubredditSession(getContext(),
        accountName,
        subreddit,
        filter,
        more,
        count,
        sessionData);
  }
}
