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

/**
 * {@link BaseProvider} that has additional methods for handling sessions.
 */
abstract class SessionProvider extends BaseProvider {

    /** Timestamp to apply to all data so we can clean it up later if necessary. */
    private static long SESSION_TIMESTAMP = -1;

    SessionProvider(String logTag) {
        super(logTag);
    }

    /**
     * Return the session timestamp to mark the data.
     */
    static long getSessionTimestamp() {
        // Initialize this once to delete all session data that was created
        // before this time. This allows to clean up any residue in the
        // database that can no longer be viewed.
        if (SESSION_TIMESTAMP == -1) {
            SESSION_TIMESTAMP = System.currentTimeMillis();
        }
        return SESSION_TIMESTAMP;
    }
}
