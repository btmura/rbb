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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.util.JsonReader;

import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.JsonParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;

class SubredditResultListing extends JsonParser implements Listing {

  public static final String TAG = "SubredditResultListing";

  private final Context ctx;
  private final String accountName;
  private final String query;

  private final ArrayList<ContentValues> values =
      new ArrayList<ContentValues>(25);

  static SubredditResultListing newInstance(
      Context ctx,
      String accountName,
      String query) {
    return new SubredditResultListing(ctx, accountName, query);
  }

  SubredditResultListing(Context ctx, String accountName, String query) {
    this.ctx = ctx.getApplicationContext();
    this.accountName = accountName;
    this.query = query;
  }

  @Override
  public int getSessionType() {
    return Sessions.TYPE_SUBREDDIT_SEARCH;
  }

  @Override
  public String getSessionThingId() {
    return null;
  }

  @Override
  public ArrayList<ContentValues> getValues() throws
      IOException,
      AuthenticatorException,
      OperationCanceledException {
    CharSequence url = Urls.subredditSearch(accountName, query);
    HttpURLConnection conn = RedditApi.connect(ctx, accountName, url, false);
    InputStream input = null;
    try {
      input = new BufferedInputStream(conn.getInputStream());
      JsonReader reader = new JsonReader(new InputStreamReader(input));
      parseListingObject(reader);
      return values;
    } finally {
      if (input != null) {
        input.close();
      }
      conn.disconnect();
    }
  }

  @Override
  public void performExtraWork(Context context) {
  }

  @Override
  public String getTargetTable() {
    return SubredditResults.TABLE_NAME;
  }

  @Override
  public boolean isAppend() {
    return false;
  }

  @Override
  public void onEntityStart(int i) {
    ContentValues v = new ContentValues(5);
    v.put(Things.COLUMN_ACCOUNT, accountName);
    values.add(v);
  }

  @Override
  public void onDisplayName(JsonReader r, int i) throws IOException {
    values.get(i).put(SubredditResults.COLUMN_NAME, readString(r, ""));
  }

  @Override
  public void onOver18(JsonReader r, int i) throws IOException {
    values.get(i).put(SubredditResults.COLUMN_OVER_18, readBoolean(r, false));
  }

  @Override
  public void onSubscribers(JsonReader r, int i) throws IOException {
    values.get(i).put(SubredditResults.COLUMN_SUBSCRIBERS, readInt(r, 0));
  }
}
