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

package com.btmura.android.reddit.provider;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

public class SessionCleanerService extends IntentService {

  private static final String EXTRA_SESSION_TYPE = "sessionType";

  public static void startService(Context context, int sessionType) {
    Intent intent = new Intent(context, SessionCleanerService.class);
    intent.putExtra(EXTRA_SESSION_TYPE, sessionType);
    context.startService(intent);
  }

  public SessionCleanerService() {
    super("SessionCleanerService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    int sessionType = intent.getIntExtra(EXTRA_SESSION_TYPE, 0);
    ThingProvider.cleanSessions(this, sessionType);
  }
}
