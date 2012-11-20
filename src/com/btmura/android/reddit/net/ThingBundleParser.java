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

package com.btmura.android.reddit.net;

import java.io.IOException;

import android.os.Bundle;
import android.util.JsonReader;

import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.util.JsonParser;

/**
 * {@link ThingBundleParser} parses the first entry of a JSON comments listing
 * into a {@link Bundle}. It is meant to be used to get information about a
 * thing, so it ignores the other things in the listing.
 */
class ThingBundleParser extends JsonParser {

    Bundle bundle = new Bundle(5);

    @Override
    public void onKind(JsonReader reader, int index) throws IOException {
        if (index == 0) {
            bundle.putInt(Things.COLUMN_KIND, Things.parseKind(reader.nextString()));
        } else {
            reader.skipValue();
        }
    }

    @Override
    public void onName(JsonReader reader, int index) throws IOException {
        if (index == 0) {
            bundle.putString(Things.COLUMN_THING_ID, reader.nextString());
        } else {
            reader.skipValue();
        }
    }

    @Override
    public void onPermaLink(JsonReader reader, int index) throws IOException {
        if (index == 0) {
            bundle.putString(Things.COLUMN_PERMA_LINK, reader.nextString());
        } else {
            reader.skipValue();
        }
    }

    @Override
    public void onIsSelf(JsonReader reader, int index) throws IOException {
        if (index == 0) {
            bundle.putBoolean(Things.COLUMN_SELF, reader.nextBoolean());
        } else {
            reader.skipValue();
        }
    }

    @Override
    public void onUrl(JsonReader reader, int index) throws IOException {
        if (index == 0) {
            bundle.putString(Things.COLUMN_URL, reader.nextString());
        } else {
            reader.skipValue();
        }
    }
}
