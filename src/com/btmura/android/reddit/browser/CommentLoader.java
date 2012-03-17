package com.btmura.android.reddit.browser;

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
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.JsonParser;

public class CommentLoader extends AsyncTaskLoader<List<Comment>> {

    private static final String TAG = "CommentLoader";

    private final CharSequence url;
    private List<Comment> comments;

    public CommentLoader(Context context, CharSequence url) {
        super(context);
        this.url = url;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (comments != null) {
            deliverResult(comments);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(List<Comment> comments) {
        this.comments = comments;
        super.deliverResult(comments);
    }

    @Override
    public List<Comment> loadInBackground() {
        try {
            long t1 = SystemClock.currentThreadTimeMillis();
            URL u = new URL(url.toString());
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.connect();

            InputStream stream = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            CommentParser parser = new CommentParser();
            parser.parseListingArray(reader);
            stream.close();
            conn.disconnect();

            long t2 = SystemClock.currentThreadTimeMillis();
            Log.v(TAG, String.valueOf(t2 - t1));

            return parser.comments;

        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    class CommentParser extends JsonParser {

        private final List<Comment> comments = new ArrayList<Comment>(360);

        @Override
        public boolean shouldParseReplies() {
            return true;
        }

        @Override
        public void onEntityStart(int index) {
            comments.add(new Comment());
        }

        @Override
        public void onKind(JsonReader reader, int index) throws IOException {
            Comment c = comments.get(index);
            String kind = reader.nextString();
            if (index == 0) {
                c.type = Comment.TYPE_HEADER;
            } else if ("more".equalsIgnoreCase(kind)) {
                c.type = Comment.TYPE_MORE;
            } else {
                c.type = Comment.TYPE_COMMENT;
            }
            c.nesting = replyNesting;
        }

        @Override
        public void onTitle(JsonReader reader, int index) throws IOException {
            comments.get(index).rawTitle = readTrimmedString(reader, "");
        }

        @Override
        public void onSelfText(JsonReader reader, int index) throws IOException {
            comments.get(index).rawBody = readTrimmedString(reader, "");
        }

        @Override
        public void onBody(JsonReader reader, int index) throws IOException {
            comments.get(index).rawBody = readTrimmedString(reader, "");
        }

        @Override
        public void onAuthor(JsonReader reader, int index) throws IOException {
            comments.get(index).author = readTrimmedString(reader, "");
        }

        @Override
        public void onCreatedUtc(JsonReader reader, int index) throws IOException {
            comments.get(index).createdUtc = reader.nextLong();
        }

        @Override
        public void onNumComments(JsonReader reader, int index) throws IOException {
            comments.get(index).numComments = reader.nextInt();
        }

        @Override
        public void onUps(JsonReader reader, int index) throws IOException {
            comments.get(index).ups = reader.nextInt();
        }

        @Override
        public void onDowns(JsonReader reader, int index) throws IOException {
            comments.get(index).downs = reader.nextInt();
        }

        @Override
        public void onParseEnd() {
            int size = comments.size();
            for (int i = 0; i < size;) {
                Comment c = comments.get(i);
                if (c.type == Comment.TYPE_MORE) {
                    comments.remove(i);
                    size--;
                } else {
                    i++;
                }
            }
        }
    }
}
