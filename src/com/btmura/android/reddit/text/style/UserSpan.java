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

package com.btmura.android.reddit.text.style;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

import com.btmura.android.reddit.app.UserProfileActivity;
import com.btmura.android.reddit.content.Contexts;
import com.btmura.android.reddit.net.Urls2;

public class UserSpan extends ClickableSpan {

  public final String user;

  public UserSpan(String user) {
    this.user = user;
  }

  @Override
  public void onClick(View widget) {
    Context context = widget.getContext();
    Intent intent = new Intent(context, UserProfileActivity.class);
    intent.setData(Uri.parse(Urls2.profileLink(user).toString()));
    Contexts.startActivity(context, intent);
  }
}
