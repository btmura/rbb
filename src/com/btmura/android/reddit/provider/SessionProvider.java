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

    /** Sessions created before this cutoff time need to be deleted. */
    private static long CREATION_TIME_CUTOFF = -1;

    /**
     * Gets the creation time cutoff. This method is not thread safe.
     *
     * @return cutoff time when all sessions must be created at or after
     */
    static long getCreationTimeCutoff() {
        // Initialize this once to delete all session data that was created
        // before the first sync. This allows to clean up any residue in the
        // database that can no longer be viewed.
        if (CREATION_TIME_CUTOFF == -1) {
            CREATION_TIME_CUTOFF = System.currentTimeMillis();
        }
        return CREATION_TIME_CUTOFF;
    }
}
