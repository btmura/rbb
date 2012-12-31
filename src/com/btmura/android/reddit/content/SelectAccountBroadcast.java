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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class SelectAccountBroadcast {

    private static final String ACTION = "com.btmura.android.reddit.content.SELECT_ACCOUNT";

    public static final String EXTRA_ACCOUNT = "account";

    public static void registerReceiver(Context context, BroadcastReceiver receiver) {
        context.registerReceiver(receiver, new IntentFilter(ACTION), null, null);
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }

    public static void sendBroadcast(Context context, String accountName) {
        Intent intent = new Intent(ACTION);
        intent.putExtra(EXTRA_ACCOUNT, accountName);
        context.sendBroadcast(intent);
    }
}
