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

package com.btmura.android.reddit.app;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * Class containing logic for inserting and deleting new comments.
 */
public class CommentLogic {

    public interface CommentList {
        /** @return number of comments in this listing */
        int getCommentCount();

        /** @return id of the comment at the given position */
        long getCommentId(int position);

        /** @return nesting of the comment at the given position */
        int getCommentNesting(int position);

        /** @return sequence of the comment at the given position */
        int getCommentSequence(int position);
    }

    public static class CursorCommentList extends CursorWrapper implements CommentList {

        private final int idIndex;
        private final int nestingIndex;
        private final int sequenceIndex;

        public CursorCommentList(Cursor cursor, int idIndex, int nestingIndex, int sequenceIndex) {
            super(cursor);
            this.idIndex = idIndex;
            this.nestingIndex = nestingIndex;
            this.sequenceIndex = sequenceIndex;
        }

        public int getCommentCount() {
            return getCount();
        }

        public long getCommentId(int position) {
            if (moveToPosition(position)) {
                return getLong(idIndex);
            }
            throw new IllegalArgumentException();
        }

        public int getCommentNesting(int position) {
            if (moveToPosition(position)) {
                return getInt(nestingIndex);
            }
            throw new IllegalArgumentException();
        }

        public int getCommentSequence(int position) {
            if (moveToPosition(position)) {
                return getInt(sequenceIndex);
            }
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return nesting for a new comment that is a response to the comment at
     *         the given position
     */
    public static int getInsertNesting(CommentList list, int position) {
        int nesting = list.getCommentNesting(position);
        // Nest an additional level if this is a response to a comment.
        if (position != 0) {
            nesting++;
        }
        return nesting;
    }

    /**
     * @return sequence for a new comment that is a response to the comment at
     *         the given position
     */
    public static int getInsertSequence(CommentList list, int position) {
        int insertPosition = getInsertPosition(list, position);
        return list.getCommentSequence(insertPosition - 1);
    }

    /**
     * @return position to insert this new comment in the list
     */
    public static int getInsertPosition(CommentList list, int position) {
        // Sequence in response to header comment should be the last.
        // Sequence in response to comment should be the last in responses.
        if (position == 0) {
            return list.getCommentCount();
        } else {
            int nesting = list.getCommentNesting(position);
            int count = list.getCommentCount();
            for (int i = position + 1; i < count; i++) {
                int nextNesting = list.getCommentNesting(i);
                if (nesting + 1 == nextNesting) {
                    position = i;
                } else {
                    break;
                }
            }
            return position + 1;
        }
    }

    /**
     * @return whether a comment is completely removable. If it has children,
     *         then we will show [deleted] rather than erasing it completely.
     */
    public static boolean hasChildren(CommentList list, int position) {
        int nesting = list.getCommentNesting(position);
        if (position + 1 < list.getCommentCount()) {
            int nextNesting = list.getCommentNesting(position + 1);

            // If the next comment has the same nesting, then this comment must
            // have no children. It's safe to remove completely.
            if (nesting == nextNesting) {
                return false;
            }

            // If the next comment is nested once more, then it's reply to this
            // comment, so we want to just mark this comment as [deleted].
            if (nesting + 1 == nextNesting) {
                return true;
            }
        }

        // If this is the last comment, then there are no replies. It's safe to
        // remove completely.
        return false;
    }

    /**
     * @return null or non-empty array of ids of children of the comment at
     *         given position
     */
    public static long[] getChildren(CommentList list, int position) {
        // First, figure out how many consecutive children there are to avoid
        // unnecessary array resizing allocations...
        int nesting = list.getCommentNesting(position);
        int numChildren = 0;
        int count = list.getCommentCount();
        for (int i = position + 1; i < count; i++) {
            int nextNesting = list.getCommentNesting(i);
            if (nextNesting > nesting) {
                numChildren++;
                continue;
            }
            break;
        }

        // Now fill up the array with the ids if there are any children.
        long[] childIds = null;
        if (numChildren > 0) {
            childIds = new long[numChildren];
            int j = 0;
            for (int i = position + 1; i < count && j < numChildren; i++, j++) {
                childIds[j] = list.getCommentId(i);
            }
        }
        return childIds;
    }

    private CommentLogic() {
    }
}
