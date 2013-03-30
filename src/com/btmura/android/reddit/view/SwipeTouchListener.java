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

package com.btmura.android.reddit.view;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

/**
 * {@link OnTouchListener} for a {@link ListView} that has swipable views. {@link ListView}
 * processes touch events for its children, so this {@link OnTouchListener} needs to be installed to
 * see all the {@link MotionEvent}s.
 * 
 * This code is adapted from Roman Nurik's gist: https://gist.github.com/romannurik/2980593
 */
public class SwipeTouchListener implements OnTouchListener {

    private final ListView listView;
    private final int touchSlop;

    private boolean disabled;
    private View swipeView;
    private float downX;
    private float downY;
    private boolean swiping;

    public SwipeTouchListener(ListView listView) {
        this.listView = listView;

        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        this.touchSlop = vc.getScaledTouchSlop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return handleActionDown(e);

            case MotionEvent.ACTION_MOVE:
                return handleActionMove(e);

            case MotionEvent.ACTION_UP:
                return handleActionUp(e);
        }
        return false;
    }

    private boolean handleActionDown(MotionEvent e) {
        if (!disabled) {
            View childView = getChildView(e);
            if (childView != null) {
                // Record the view that is probably being sniped and the initial touch location,
                // so we can translate the swiped view by the change in touch location.
                swipeView = childView;
                downX = e.getRawX();
                downY = e.getRawY();

                // Consume the touch event to indicate that we handle it. However, pass the
                // touch event to the ListView to process as well, because we don't know whether
                // this is just a simple touch or the beginning of a swipe.
                listView.onTouchEvent(e);
                return true;
            }
        }
        return false;
    }

    /** Gets the child corresponding to the coordinates of the {@link MotionEvent}. */
    private View getChildView(MotionEvent e) {
        int position = listView.pointToPosition(Math.round(e.getX()), Math.round(e.getY()));
        if (position != ListView.INVALID_POSITION) {
            int firstPosition = listView.getFirstVisiblePosition();
            int lastPosition = listView.getLastVisiblePosition();
            if (position >= firstPosition && position <= lastPosition) {
                return listView.getChildAt(position - firstPosition);
            }
        }
        return null;
    }

    private boolean handleActionMove(MotionEvent e) {
        if (!disabled && swipeView != null) {
            float deltaX = e.getRawX() - downX;
            float deltaY = e.getRawY() - downY;

            // Consider it a swipe if the user is moving enough on the X-axis but is NOT moving
            // enough on the Y-axis. If we don't check the Y-axis movement, then the items will
            // start swiping as we scroll down the list.
            if (Math.abs(deltaX) > touchSlop && Math.abs(deltaY) < touchSlop) {
                swiping = true;

                // Tell ListView not to consume any of our motion events for this touch.
                listView.requestDisallowInterceptTouchEvent(true);

                // Send a cancel event to the parent ListView to make it cancel any select
                // highlighting or pending long presses.
                MotionEvent cancelEvent = MotionEvent.obtain(e);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL
                        | (e.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                listView.onTouchEvent(cancelEvent);
                cancelEvent.recycle();
            }

            // Keep translating the swiped view even if their delta is no longer that much. They
            // could just be holding the view down for a moment. Return true to consume the event.
            if (swiping) {
                swipeView.setTranslationX(deltaX);
                swipeView.setAlpha(1f - (Math.abs(deltaX) / swipeView.getWidth()));
                return true;
            }
        }
        return false;
    }

    private boolean handleActionUp(MotionEvent e) {
        if (swipeView != null) {
            swipeView.animate().translationX(0).alpha(1).start();
        }
        swipeView = null;
        downX = 0;
        downY = 0;
        swiping = false;
        return false;
    }

    /** Create a {@link OnScrollListener} that disables the swipe listener when scrolling. */
    public OnScrollListener makeScrollListener() {
        return new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                disabled = scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
            }
        };
    }
}
