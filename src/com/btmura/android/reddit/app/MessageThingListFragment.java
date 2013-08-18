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

import android.os.Bundle;

import com.btmura.android.reddit.util.ComparableFragments;

public class MessageThingListFragment
        extends ThingListFragment<MessageThingListController, MessageThingActionModeController> {

    public static MessageThingListFragment newInstance(String accountName, String messageUser,
            int filter, boolean singleChoice) {
        Bundle args = new Bundle(4);
        args.putString(MessageThingListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(MessageThingListController.EXTRA_MESSAGE_USER, messageUser);
        args.putInt(MessageThingListController.EXTRA_FILTER, filter);
        args.putBoolean(MessageThingListController.EXTRA_SINGLE_CHOICE, singleChoice);

        MessageThingListFragment frag = new MessageThingListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    protected MessageThingListController createController() {
        return new MessageThingListController(getActivity(), getArguments());
    }

    @Override
    protected MessageThingActionModeController createActionModeController(
            MessageThingListController controller) {
        return new MessageThingActionModeController(getActivity(), controller.getAdapter());
    }

    public String getMessageUser() {
        return controller.getMessageUser();
    }

    @Override
    public boolean fragmentEquals(ComparableFragment o) {
        return ComparableFragments.baseEquals(this, o)
                && ComparableFragments.equalStrings(this, o,
                        MessageThingListController.EXTRA_ACCOUNT_NAME)
                && ComparableFragments.equalStrings(this, o,
                        MessageThingListController.EXTRA_MESSAGE_USER)
                && ComparableFragments.equalInts(this, o,
                        MessageThingListController.EXTRA_FILTER);
    }
}
