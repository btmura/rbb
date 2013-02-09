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

import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;

import com.btmura.android.reddit.net.Result;

/**
 * {@link Syncer} is an internal interface to extract the behavioral pattern of
 * querying for actions and iterating over them. In each iteration, the
 * operation will be synced back to the server over the network and then
 * modifications will be made to the database.
 */
interface Syncer {

    /** Query the provider for pending actions and return a Cursor of them. */
    Cursor query(ContentProviderClient provider, String accountName) throws RemoteException;

    /** Sync the action to the server over the network. */
    Result sync(Context context, Cursor c, String cookie, String modhash) throws IOException;

    /** Return how many total db operations will be made to clean up. */
    int getOpCount(int count);

    /** Add db operations for the current action to the list. */
    void addOps(String accountName, Cursor c, ArrayList<ContentProviderOperation> ops);

    /** Tally up the results of the applied db operations. */
    void tallyOpResults(ContentProviderResult[] results, SyncResult syncResult);
}
