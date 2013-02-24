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
package com.btmura.android.reddit.app;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.TestCase;

import com.btmura.android.reddit.app.CommentLogic.CommentList;

public class CommentLogicTest extends TestCase {

    public void testGetInsertPosition() {
        // mc0
        // mc1
        // +--mc2
        // |  +--mc3
        // +--mc4
        MockComment mc0 = MockComment.of(0, 0, 0);
        MockComment mc1 = MockComment.of(1, 1, 0);
        MockComment mc2 = MockComment.of(2, 2, 1);
        MockComment mc3 = MockComment.of(3, 3, 2);
        MockComment mc4 = MockComment.of(4, 4, 1);
        MockCommentList mcl = MockCommentList.of(mc0, mc1, mc2, mc3, mc4);

        assertEquals(5, CommentLogic.getInsertPosition(mcl, 0));
        assertEquals(5, CommentLogic.getInsertPosition(mcl, 1));
        assertEquals(4, CommentLogic.getInsertPosition(mcl, 2));
        assertEquals(4, CommentLogic.getInsertPosition(mcl, 3));

        assertEquals(5, CommentLogic.getInsertSequence(mcl, 0));
        assertEquals(5, CommentLogic.getInsertSequence(mcl, 1));
        assertEquals(4, CommentLogic.getInsertSequence(mcl, 2));
        assertEquals(4, CommentLogic.getInsertSequence(mcl, 3));

        assertEquals(0, CommentLogic.getInsertNesting(mcl, 0));
        assertEquals(1, CommentLogic.getInsertNesting(mcl, 1));
        assertEquals(2, CommentLogic.getInsertNesting(mcl, 2));
        assertEquals(3, CommentLogic.getInsertNesting(mcl, 3));
    }

    static class MockComment {
        long id;
        int nesting;
        int sequence;

        static MockComment of(long id, int sequence, int nesting) {
            MockComment mc = new MockComment();
            mc.id = id;
            mc.nesting = nesting;
            mc.sequence = sequence;
            return mc;
        }
    }

    static class MockCommentList implements CommentList {

        private final ArrayList<MockComment> comments = new ArrayList<MockComment>();

        static MockCommentList of(MockComment... comments) {
            MockCommentList mcl = new MockCommentList();
            Collections.addAll(mcl.comments, comments);
            return mcl;
        }

        @Override
        public int getCommentCount() {
            return comments.size();
        }

        @Override
        public long getCommentId(int position) {
            return comments.get(position).id;
        }

        @Override
        public int getCommentNesting(int position) {
            return comments.get(position).nesting;
        }

        @Override
        public int getCommentSequence(int position) {
            return comments.get(position).sequence;
        }
    }
}
