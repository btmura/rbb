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

package com.btmura.android.reddit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.provider.Provider.Subreddits;

public class BackupAgent extends android.app.backup.BackupAgent {

    private static final String KEY_SUBREDDITS = "subreddits";

    private static final String[] PROJECTION = {
            Subreddits.COLUMN_NAME,
    };

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {
        backupSubreddits(data);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {
        while (data.readNextHeader()) {
            String key = data.getKey();
            if (KEY_SUBREDDITS.equals(key)) {
                restoreSubreddits(data);
            } else {
                data.skipEntityData();
            }
        }
    }

    private void backupSubreddits(BackupDataOutput data) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        Cursor c = getContentResolver().query(Subreddits.CONTENT_URI,
                PROJECTION,
                Subreddits.SELECTION_ACCOUNT,
                Subreddits.SELECTION_ARGS_NO_ACCOUNT,
                Subreddits.SORT_NAME);
        try {
            out.writeInt(c.getCount());
            while (c.moveToNext()) {
                out.writeUTF(c.getString(0));
            }
        } finally {
            c.close();
        }

        byte[] buffer = b.toByteArray();
        int len = buffer.length;
        data.writeEntityHeader(KEY_SUBREDDITS, len);
        data.writeEntityData(buffer, len);
        b.close();
    }

    private void restoreSubreddits(BackupDataInput data) throws IOException {
        int size = data.getDataSize();
        byte[] b = new byte[size];
        data.readEntityData(b, 0, size);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        int count = in.readInt();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(count + 1);
        ops.add(ContentProviderOperation.newDelete(Subreddits.CONTENT_URI).build());
        for (int i = 0; i < count; i++) {
            ops.add(ContentProviderOperation.newInsert(Subreddits.CONTENT_URI)
                    .withValue(Subreddits.COLUMN_NAME, in.readUTF()).build());
        }
        try {
            getContentResolver().applyBatch(Provider.AUTHORITY, ops);
        } catch (RemoteException e) {
            throw new IOException(e);
        } catch (OperationApplicationException e) {
            throw new IOException(e);
        }
    }
}
