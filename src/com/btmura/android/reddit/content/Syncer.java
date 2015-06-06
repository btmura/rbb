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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

import com.btmura.android.reddit.net.Result;

import java.io.IOException;
import java.util.ArrayList;

/**
 * {@link Syncer} is an internal interface to extract the behavioral pattern of
 * querying for actions and iterating over them. In each iteration, the
 * operation will be synced back to the server over the network and then
 * modifications will be made to the database.
 */
interface Syncer {

  /** Return a tag to identify this Syncer in debug logs. */
  String getTag();

  /** Query the provider for pending actions and return a Cursor of them. */
  Cursor query(ContentProviderClient provider, String accountName)
      throws RemoteException;

  /** Return the number of sync failures excluding rate limiting of this action. */
  int getSyncFailures(Cursor c);

  /** Return how many total db operations will be made to clean up. */
  int getEstimatedOpCount(int count);

  /** Sync the action to the server over the network. */
  Result sync(
      Context context,
      String accountName,
      Cursor c,
      String cookie,
      String modhash)
      throws AuthenticatorException, IOException, OperationCanceledException;

  /**
   * Add a DB action to remove the action because it succeeded or the retrie
   * were exceeded.
   */
  void addDeleteAction(Cursor c, Ops ops);

  /** Add a DB action to increment the action's sync failures. */
  void addUpdateAction(Cursor c, Ops ops, int syncFailures, String syncStatus);
}

/**
 * Container of {@link android.content.ContentProviderOperation}s with some
 * counters.
 */
class Ops extends ArrayList<ContentProviderOperation> {

  int deletes;
  int updates;

  Ops(int capacity) {
    super(capacity);
  }

  void addDelete(ContentProviderOperation delete) {
    add(delete);
    deletes++;
  }

  void addUpdate(ContentProviderOperation update) {
    add(update);
    updates++;
  }
}