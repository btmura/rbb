package com.btmura.android.reddit.subredditsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;

class SubredditInfoLoader extends AsyncTaskLoader<List<SubredditInfo>> {

    private static final String TAG = "SubredditLoader";

    private List<SubredditInfo> results;

    private String query;

    public SubredditInfoLoader(Context context, String query) {
        super(context);
        this.query = query;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (results != null) {
            deliverResult(results);
        } else {
            forceLoad();
        }
    }

    @Override
    public List<SubredditInfo> loadInBackground() {
        try {
            URL subredditUrl = new URL("http://www.reddit.com/reddits/search.json?q="
                    + URLEncoder.encode(query));

            HttpURLConnection connection = (HttpURLConnection) subredditUrl.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            SearchParser parser = new SearchParser();
            parser.parseListingObject(reader);
            stream.close();

            connection.disconnect();

            return parser.results;

        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
    }

    class SearchParser extends JsonParser {

        private List<SubredditInfo> results = new ArrayList<SubredditInfo>();

        @Override
        public void onEntityStart(int index) {
            results.add(new SubredditInfo());
        }

        @Override
        public void onDisplayName(JsonReader reader, int index) throws IOException {
            results.get(index).displayName = reader.nextString();
        }

        @Override
        public void onTitle(JsonReader reader, int index) throws IOException {
            results.get(index).title = Formatter.formatTitle(getContext(),
                    readTrimmedString(reader, ""));
        }

        @Override
        public void onDescription(JsonReader reader, int index) throws IOException {
            results.get(index).description = readTrimmedString(reader, "");
        }

        @Override
        public void onSubscribers(JsonReader reader, int index) throws IOException {
            results.get(index).subscribers = reader.nextInt();
        }

        @Override
        public void onEntityEnd(int index) {
            SubredditInfo srInfo = results.get(index);
            srInfo.status = getContext().getString(R.string.sr_info_status, srInfo.displayName,
                    srInfo.subscribers);
        }
    }
}
