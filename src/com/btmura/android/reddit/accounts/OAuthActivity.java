/*
 * Copyright (C) 2015 Brian Muramatsu
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

package com.btmura.android.reddit.accounts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.net.AccessTokenResult;
import com.btmura.android.reddit.net.Urls;

import java.io.IOException;

public class OAuthActivity extends Activity {

    TextView tv;
    StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.oauth);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        tv = (TextView) findViewById(R.id.textView);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            addText(uri.toString());

            CharSequence clientId = getString(R.string.key_reddit_client_id);
            String code = uri.getQueryParameter("code");

            try {
                AccessTokenResult result = AccessTokenResult.getAccessToken(clientId, code, Urls.OAUTH_REDIRECT_URL);
                addText("at: " + result.accessToken);
                addText("tt: " + result.tokenType);
                addText("ei: " + result.expiresIn);
                addText("sc: " + result.scope);
                addText("rt: " + result.refreshToken);
            } catch (IOException e) {
                addText(e.getMessage());
            }
        }
    }

    private void addText(CharSequence text) {
        tv.setText(sb.append(text).append("\n"));
    }
}
