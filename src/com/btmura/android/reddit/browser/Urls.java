package com.btmura.android.reddit.browser;

public class Urls {

    public static CharSequence subredditUrl(Subreddit sr, int filter, String after) {
        StringBuilder b = new StringBuilder("http://www.reddit.com/");

        if (!sr.isFrontPage()) {
            b.append("r/").append(sr.name);
        }

        switch (filter) {
            case FilterAdapter.FILTER_HOT:
                break;

            case FilterAdapter.FILTER_NEW:
                b.append("/new");
                break;

            case FilterAdapter.FILTER_CONTROVERSIAL:
                b.append("/controversial");
                break;

            case FilterAdapter.FILTER_TOP:
                b.append("/top");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }

        b.append("/.json");

        boolean hasSort = filter == FilterAdapter.FILTER_NEW;
        if (hasSort) {
            b.append("?sort=new");
        }
        if (after != null) {
            b.append(hasSort ? "&" : "?").append("count=25&after=").append(after);
        }
        return b;
    }

    public static CharSequence commentsUrl(String id) {
        return new StringBuilder("http://www.reddit.com/comments/").append(id).append(".json");
    }
}
