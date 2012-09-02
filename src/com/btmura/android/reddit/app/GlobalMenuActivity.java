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

package com.btmura.android.reddit.app;

import android.app.Activity;
import android.view.KeyEvent;


abstract class GlobalMenuActivity extends Activity {

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                getGlobalMenuFragment().handleSearch();
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private GlobalMenuFragment getGlobalMenuFragment() {
        return (GlobalMenuFragment) getFragmentManager().findFragmentByTag(GlobalMenuFragment.TAG);
    }
}
