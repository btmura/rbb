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

package com.btmura.android.reddit.app;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

interface MenuController {

    // TODO(btmura): do we need this method?
    void restoreInstanceState(Bundle savedInstanceState);

    // TODO(btmura): do we need this method?
    void saveInstanceState(Bundle outState);

    // Menu handling methods

    void onCreateOptionsMenu(Menu menu, MenuInflater inflater);

    void onPrepareOptionsMenu(Menu menu);

    boolean onOptionsItemSelected(MenuItem item);
}
