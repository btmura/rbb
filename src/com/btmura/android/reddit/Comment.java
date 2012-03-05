package com.btmura.android.reddit;

public class Comment {

    static final int TYPE_HEADER = 0;
    static final int TYPE_COMMENT = 1;
    static final int TYPE_MORE = 2;

    public int type;
    public CharSequence title;
    public CharSequence body;
    public CharSequence status;
    public int nesting;

    // Temporary values used to create status
    public String author;
    public int numComments;
    public int ups;
    public int downs;
}
