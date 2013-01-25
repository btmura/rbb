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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.Formatter;

public class MessageThreadAdapter extends BaseLoaderAdapter {

    private static final String[] PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_KIND,
            Messages.COLUMN_NEW,
            Messages.COLUMN_SUBJECT,
            Messages.TABLE_NAME + "." + Messages.COLUMN_THING_ID,
    };

    private static final int INDEX_AUTHOR = 1;
    private static final int INDEX_BODY = 2;
    private static final int INDEX_CREATED_UTC = 3;
    private static final int INDEX_KIND = 4;
    private static final int INDEX_NEW = 5;
    private static final int INDEX_SUBJECT = 6;
    private static final int INDEX_THING_ID = 7;

    private final Formatter formatter = new Formatter();

    private long sessionId = -1;
    private String accountName;
    private String thingId;

    public MessageThreadAdapter(Context context) {
        super(context, null, 0);
    }

    public boolean isLoadable() {
        return accountName != null && thingId != null;
    }

    @Override
    public Uri getLoaderUri() {
        return ThingProvider.messageThreadUri(sessionId, accountName, thingId);
    }

    @Override
    protected String[] getProjection() {
        return PROJECTION;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ThingView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String author = cursor.getString(INDEX_AUTHOR);
        String body = cursor.getString(INDEX_BODY);
        long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
        int kind = cursor.getInt(INDEX_KIND);

        // Only show the subject on the header message.
        String title = cursor.getPosition() == 0 ? cursor.getString(INDEX_SUBJECT) : null;

        ThingView tv = (ThingView) view;
        tv.setType(ThingView.TYPE_MESSAGE_THREAD_LIST);
        tv.setBody(body, false, formatter);
        tv.setData(getAccountName(), author, createdUtc, null, null, 0, true, kind, 0,
                null, 0, System.currentTimeMillis(), 0, false, null, 0, null,
                0, null, null, title, 0);
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public String getSubject(){
        return getString(0, INDEX_SUBJECT);
    }

    public String getUser(int position) {
        return getString(position, INDEX_AUTHOR);
    }

    public String getThingId(int position) {
        return getString(position, INDEX_THING_ID);
    }

    public boolean isNew(int position) {
        return getBoolean(position, INDEX_NEW);
    }
}
