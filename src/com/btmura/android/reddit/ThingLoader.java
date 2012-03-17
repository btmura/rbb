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
import com.btmura.android.reddit.data.RelativeTime;

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
        public void onIsSelf(JsonReader reader, int index) throws IOException {
            things.get(index).isSelf = reader.nextBoolean();
        }

        @Override
        public void onUrl(JsonReader reader, int index) throws IOException {
            things.get(index).url = readTrimmedString(reader, "");
        }

        @Override
        public void onPermaLink(JsonReader reader, int index) throws IOException {
            things.get(index).permaLink = readTrimmedString(reader, "");
        }

        @Override
        public void onThumbnail(JsonReader reader, int index) throws IOException {
            things.get(index).thumbnail = readTrimmedString(reader, "");
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
        public void onScore(JsonReader reader, int index) throws IOException {
            things.get(index).score = reader.nextInt();
        }

        @Override
        public void onNumComments(JsonReader reader, int index) throws IOException {
            things.get(index).numComments = reader.nextInt();
        }

        @Override
        public void onEntityEnd(int index) {
            Thing t = things.get(index);
            t.status = getStatus(t);
        }

        private String getStatus(Thing t) {
            boolean matchesSubreddit = subreddit.equalsIgnoreCase(t.subreddit);
            int resId = matchesSubreddit ?R.string.thing_status :  R.string.thing_status_subreddit;
            String rt = RelativeTime.format(getContext(), nowUtc - t.createdUtc);
            return getContext().getString(resId, t.subreddit, t.author, rt, t.score, t.numComments);
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
