package com.btmura.android.reddit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.JsonParser;

public class ThingLoader extends AsyncTaskLoader<List<Thing>> {

    private static final String TAG = "ThingLoader";

    private final String subreddit;
    private final CharSequence url;
    private List<Thing> things;
    private List<Thing> initThings;

    public ThingLoader(Context context, String subreddit, CharSequence url, List<Thing> initThings) {
        super(context);
        this.subreddit = subreddit;
        this.url = url;
        this.initThings = initThings;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (things != null) {
            deliverResult(things);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(List<Thing> things) {
        this.things = things;
        super.deliverResult(things);
    }

    @Override
    public List<Thing> loadInBackground() {
        try {
            URL u = new URL(url.toString());
            Log.v(TAG, url.toString());

            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.connect();

            InputStream stream = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            ThingParser parser = new ThingParser();
            parser.parseListingObject(reader);
            stream.close();

            conn.disconnect();

            return parser.things;

        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    class ThingParser extends JsonParser {

        private final ArrayList<Thing> things = new ArrayList<Thing>(30);
        private String moreKey;
        private long nowUtc;

        @Override
        public void onParseStart() {
            super.onParseStart();
            nowUtc = System.currentTimeMillis() / 1000;
        }

        @Override
        public void onEntityStart(int index) {
            Thing t = new Thing();
            t.type = Thing.TYPE_THING;
            things.add(t);
        }

        @Override
        public void onName(JsonReader reader, int index) throws IOException {
            things.get(index).name = readTrimmedString(reader, "");
        }

        @Override
        public void onTitle(JsonReader reader, int index) throws IOException {
            things.get(index).rawTitle = readTrimmedString(reader, "");
            things.get(index).assureTitle(getContext());
        }

        @Override
        public void onSubreddit(JsonReader reader, int index) throws IOException {
            things.get(index).subreddit = readTrimmedString(reader, "");
        }

        @Override
        public void onAuthor(JsonReader reader, int index) throws IOException {
            things.get(index).author = readTrimmedString(reader, "");
        }

        @Override
        public void onCreatedUtc(JsonReader reader, int index) throws IOException {
            things.get(index).createdUtc = reader.nextLong();
        }

        @Override
        public void onUrl(JsonReader reader, int index) throws IOException {
            things.get(index).url = readTrimmedString(reader, "");
        }

        @Override
        public void onThumbnail(JsonReader reader, int index) throws IOException {
            things.get(index).thumbnail = readTrimmedString(reader, "");
        }

        @Override
        public void onPermaLink(JsonReader reader, int index) throws IOException {
            things.get(index).permaLink = readTrimmedString(reader, "");
        }

        @Override
        public void onIsSelf(JsonReader reader, int index) throws IOException {
            things.get(index).isSelf = reader.nextBoolean();
        }

        @Override
        public void onNumComments(JsonReader reader, int index) throws IOException {
            things.get(index).numComments = reader.nextInt();
        }

        @Override
        public void onScore(JsonReader reader, int index) throws IOException {
            things.get(index).score = reader.nextInt();
        }

        @Override
        public void onEntityEnd(int index) {
            Thing t = things.get(index);
            t.status = getStatus(t);
            Log.v(TAG, getContext().getString(R.string.thing_status));
            Log.v(TAG, "Status: " + t.status);
        }

        private String getStatus(Thing t) {
            if (subreddit.equalsIgnoreCase(t.subreddit)) {
                return getContext().getString(R.string.thing_status, t.author,
                        getRelativeTimeString(t), t.score, t.numComments);
            } else {
                return getContext().getString(R.string.thing_status_2, t.subreddit, t.author,
                        getRelativeTimeString(t), t.score, t.numComments);
            }
        }
        
        private static final int MINUTE_SECONDS = 60;
        private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
        private static final int DAY_SECONDS = HOUR_SECONDS * 24;
        private static final int MONTH_SECONDS = DAY_SECONDS * 30;
        private static final int YEAR_SECONDS = MONTH_SECONDS * 12;

        private String getRelativeTimeString(Thing t) {
            long ago = nowUtc - t.createdUtc;
            int format;
            int divisor;
            
            if (ago > YEAR_SECONDS * 2) {
                format = R.string.x_years_ago;
                divisor = YEAR_SECONDS;
            } else if (ago > MONTH_SECONDS * 2) {
                format = R.string.x_months_ago;
                divisor = MONTH_SECONDS;
            } else if (ago > DAY_SECONDS * 2) {
                format = R.string.x_days_ago;
                divisor = DAY_SECONDS;
            } else if (ago > HOUR_SECONDS * 2) {
                format = R.string.x_hours_ago;
                divisor = HOUR_SECONDS;
            } else if (ago > MINUTE_SECONDS * 2) {
                format = R.string.x_minutes_ago;
                divisor = MINUTE_SECONDS;
            } else {
                format = R.string.x_seconds_ago;
                divisor = 1;
            }
            
            long value = Math.round(Math.floor((double) ago / divisor));
            return getContext().getString(format, value);
        }

        @Override
        public void onAfter(JsonReader reader) throws IOException {
            moreKey = readTrimmedString(reader, null);
        }

        @Override
        public void onParseEnd() {
            if (moreKey != null) {
                Thing t = new Thing();
                t.type = Thing.TYPE_MORE;
                t.moreKey = moreKey;
                things.add(t);
            }

            if (initThings != null) {
                int size = initThings.size() - 1;
                if (size > 0) {
                    things.ensureCapacity(things.size() + size);
                    things.addAll(0, initThings.subList(0, size));
                }
            }
        }
    }
}
