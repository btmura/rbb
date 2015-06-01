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

package com.btmura.android.reddit.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.JsonParser;

public class Result extends JsonParser {

    /** Error for too much user activity. */
    private static final String ERROR_RATELIMIT = "RATELIMIT";

    /** Error for missing or incorrect captcha guess. */
    private static final String ERROR_BAD_CAPTCHA = "BAD_CAPTCHA";

    /** Error when necessary credentials are missing fromJson a request. */
    private static final String ERROR_USER_REQUIRED = "USER_REQUIRED";

    public double rateLimit;

    /**
     * Example: [[BAD_CAPTCHA, care to try these again?, captcha], [RATELIMIT, you are doing
     * that too much. try again in 6 minutes., ratelimit]]
     */
    public String[][] errors;

    /** Example: D5GggaXa0GWshObkjzzEPzzrK8zpQfeB */
    public String captcha;

    /** Captcha id returned when asking for a new captcha. */
    public String iden;

    /** Example: http://www.reddit.com/r/rbb/comments/w5mhh/test/ */
    public String url;

    /** Example: t3_w5mhh */
    public String name;

    public CharSequence getErrorMessage(Context context) {
        if (Array.isEmpty(errors)) {
            return "";
        }
        StringBuilder b = new StringBuilder();

        // Append a newline if there are multiple errors.
        if (errors.length > 1) {
            b.append("\n");
        }

        for (int i = 0; i < errors.length; i++) {
            b.append(context.getString(R.string.reddit_error_line,
                    errors[i][0], errors[i][1]));
            if (i + 1 < errors.length) {
                b.append("\n");
            }
        }

        return context.getString(R.string.reddit_error, b);
    }

    public CharSequence getErrorCodeMessage() {
        if (Array.isEmpty(errors)) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < errors.length; i++) {
            b.append(errors[i][0]);
            if (i + 1 < errors.length) {
                b.append(",");
            }
        }
        return b;
    }

    public boolean hasErrors() {
        return !Array.isEmpty(errors);
    }

    public boolean hasRateLimitError() {
        return hasError(ERROR_RATELIMIT);
    }

    public boolean hasBadCaptchaError() {
        return hasError(ERROR_BAD_CAPTCHA);
    }

    public boolean hasUserRequiredError() {
        return hasError(ERROR_USER_REQUIRED);
    }

    private boolean hasError(String errorCode) {
        if (!Array.isEmpty(errors)) {
            for (int i = 0; i < errors.length; i++) {
                if (errorCode.equals(errors[i][0])) {
                    return true;
                }
            }
        }
        return false;
    }

    public void logAnyErrors(String tag, CharSequence prefix) {
        if (!Array.isEmpty(errors)) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < errors.length; i++) {
                line.delete(0, line.length()).append(prefix).append(": ");
                for (int j = 0; j < errors[i].length; j++) {
                    line.append(errors[i][j]);
                    if (j + 1 < errors[i].length) {
                        line.append(" ");
                    }
                }
                Log.d(tag, line.toString());
            }
        }
    }

    static Result fromJson(InputStream in) throws IOException {
        JsonReader r = newReader(in);
        try {
            return fromJsonReader(r);
        } finally {
            r.close();
        }
    }

    static Result fromJsonReader(JsonReader reader) throws IOException {
        Result result = new Result();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("json".equals(name)) {
                    parseJson(reader, result);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
        return result;
    }

    private static void parseJson(JsonReader reader, Result result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("errors".equals(name)) {
                result.errors = parseErrorsArray(reader);
            } else if ("ratelimit".equals(name)) {
                result.rateLimit = reader.nextDouble();
            } else if ("captcha".equals(name)) {
                result.captcha = reader.nextString();
            } else if ("data".equals(name)) {
                parseData(reader, result);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static String[][] parseErrorsArray(JsonReader reader) throws IOException {
        String[][] errors = null;
        reader.beginArray();
        for (int i = 0; reader.hasNext(); i++) {
            // There should only be 1 error but permit expansion in emergency.
            if (errors == null) {
                errors = new String[1][];
            } else {
                errors = Array.ensureLength(errors, i + 1);
            }
            errors[i] = parseSingleErrorArray(reader);
        }
        reader.endArray();
        return errors;
    }

    private static String[] parseSingleErrorArray(JsonReader reader) throws IOException {
        // There should only be 3 elements per error but permit expansion.
        // Some parts of the array can be null.
        String[] error = new String[3];
        reader.beginArray();
        for (int i = 0; reader.hasNext(); i++) {
            error = Array.ensureLength(error, i + 1);
            if (reader.peek() == JsonToken.STRING) {
                error[i] = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endArray();
        return error;
    }

    private static void parseData(JsonReader reader, Result result) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("iden".equals(name)) {
                result.iden = reader.nextString();
            } else if ("url".equals(name)) {
                result.url = reader.nextString();
            } else if ("name".equals(name)) {
                result.name = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}