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

import android.util.JsonReader;
import android.util.JsonToken;

import com.btmura.android.reddit.util.JsonParser;

/**
 * {@link AccountInfoResult} is the result of calling the {@link RedditApi#aboutMe(String)} or
 * {@link RedditApi#aboutUser(String, String)} methods.
 */
public class AccountInfoResult extends JsonParser {

    /** Amount of link karma. */
    public int linkKarma;

    /** Amount of comment karma. */
    public int commentKarma;

    /** True if the account has mail. False otherwise. */
    public boolean hasMail;

    /** Return a new {@link AccountInfoResult} from a {@link JsonReader}. */
    public static AccountInfoResult fromJsonReader(JsonReader reader) throws IOException {
        AccountInfoResult result = new AccountInfoResult();
        result.parseEntity(reader);
        return result;
    }

    private AccountInfoResult() {
        // Use the fromJsonReader method.
    }

    @Override
    public void onLinkKarma(JsonReader reader, int index) throws IOException {
        linkKarma = reader.nextInt();
    }

    @Override
    public void onCommentKarma(JsonReader reader, int index) throws IOException {
        commentKarma = reader.nextInt();
    }

    @Override
    public void onHasMail(JsonReader reader, int index) throws IOException {
        // hasMail is null when we are viewing somebody else's account info.
        hasMail = reader.peek() != JsonToken.NULL && reader.nextBoolean();
    }
}