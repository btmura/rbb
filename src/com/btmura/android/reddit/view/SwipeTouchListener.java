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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.MotionEvent;
import android.view.VelocityTracker;
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

    private final int touchSlop;
    private final int minFlingVelocity;
    private final int maxFlingVelocity;
    private final int animationTime;
    private final ListView listView;
    private final OnSwipeDismissListener dismissListener;

    private boolean disabled;
    private float downX;
    private float downY;
    private View downView;
    private int downPosition;
    private boolean swiping;
    private VelocityTracker velocityTracker;

    public interface OnSwipeDismissListener {
        void onSwipeDismiss(ListView listView, View view, int position);
    }

    public SwipeTouchListener(ListView listView, OnSwipeDismissListener swipeListener) {
        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        this.touchSlop = vc.getScaledTouchSlop();
        this.minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        this.maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        this.animationTime = listView.getResources()
                .getInteger(android.R.integer.config_shortAnimTime);
        this.listView = listView;
        this.dismissListener = swipeListener;
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

    private boolean handleActionDown(MotionEvent event) {
        if (!disabled) {
            View childView = getChildView(event);
            if (childView != null) {
                // Record the view that is probably being sniped and the initial touch location,
                // so we can translate the swiped view by the change in touch location.
                downX = event.getRawX();
                downY = event.getRawY();
                downView = childView;
                downPosition = listView.getPositionForView(childView);

                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);

                // Consume the touch event to indicate that we handle it. However, pass the
                // touch event to the ListView to process as well, because we don't know whether
                // this is just a simple touch or the beginning of a swipe.
                listView.onTouchEvent(event);
                return true;
            }
        }
        return false;
    }

    /** Gets the child corresponding to the coordinates of the {@link MotionEvent}. */
    private View getChildView(MotionEvent e) {
        // This approach works, because we don't shrink view heights.
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

    private boolean handleActionMove(MotionEvent event) {
        if (!disabled && downView != null) {
            velocityTracker.addMovement(event);

            float deltaX = event.getRawX() - downX;
            float deltaY = event.getRawY() - downY;

            // Consider it a swipe if the user is moving enough on the X-axis but is NOT moving
            // enough on the Y-axis. If we don't check the Y-axis movement, then the items will
            // start swiping as we scroll down the list.
            if (Math.abs(deltaX) > touchSlop && Math.abs(deltaY) < touchSlop) {
                swiping = true;

                // Tell ListView not to consume any of our motion events for this touch.
                listView.requestDisallowInterceptTouchEvent(true);

                // Send a cancel event to the parent ListView to make it cancel any select
                // highlighting or pending long presses.
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL
                        | (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                listView.onTouchEvent(cancelEvent);
                cancelEvent.recycle();
            }

            // Keep translating the swiped view even if their delta is no longer that much. They
            // could just be holding the view down for a moment. Return true to consume the event.
            if (swiping) {
                downView.setTranslationX(deltaX);
                downView.setAlpha(1f - (Math.abs(deltaX) / downView.getWidth()));
                return true;
            }
        }
        return false;
    }

    private boolean handleActionUp(MotionEvent event) {
        if (!disabled && swiping) {
            boolean dismiss = false;
            boolean dismissRight = false;

            velocityTracker.addMovement(event);
            velocityTracker.computeCurrentVelocity(1000);
            float vx = velocityTracker.getXVelocity();
            float vy = velocityTracker.getYVelocity();

            float deltaX = event.getRawX() - downX;
            if (Math.abs(deltaX) > downView.getWidth() / 2) {
                dismiss = true;
                dismissRight = deltaX > 0;
            } else if (Math.abs(vx) >= minFlingVelocity
                    && Math.abs(vx) <= maxFlingVelocity
                    && Math.abs(vy) < Math.abs(vx)) {
                dismiss = true;
                dismissRight = vx > 0;
            }

            if (dismiss) {
                final View view = downView;
                final int position = downPosition;
                downView.animate()
                        .setDuration(animationTime)
                        .translationX(downView.getWidth() * (dismissRight ? 1 : -1))
                        .alpha(0)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (dismissListener != null) {
                                    dismissListener.onSwipeDismiss(listView, view, position);
                                }
                            }
                        })
                        .start();
            } else {
                downView.animate()
                        .setDuration(animationTime)
                        .translationX(0)
                        .alpha(1)
                        .start();
            }

            velocityTracker.recycle();
            velocityTracker = null;
            downView = null;
            downX = 0;
            downY = 0;
            downPosition = ListView.INVALID_POSITION;
            swiping = false;
        }

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

    public static void undoAnimation(View view) {
        view.setTranslationX(0);
        view.setAlpha(1);
    }
}
