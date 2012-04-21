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

package com.btmura.android.reddit.fragment;

import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.Thing;
import com.btmura.android.reddit.data.Urls;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARGS_THING = "thing";

    public interface ThingMenuListener {

        void onLinkSelected();

        void onCommentsSelected();

        boolean isShowingLink();
    }
    
    private Thing thing;
    private ShareActionProvider shareProvider;

    public static ThingMenuFragment newInstance(Thing thing) {
        ThingMenuFragment f = new ThingMenuFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(ARGS_THING, thing);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        thing = getArguments().getParcelable(ARGS_THING);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu, menu);
        shareProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean showingLink = getListener().isShowingLink();
        boolean showLink = !thing.isSelf && !showingLink;
        boolean showComments = !thing.isSelf && showingLink;

        menu.findItem(R.id.menu_link).setVisible(showLink);
        menu.findItem(R.id.menu_comments).setVisible(showComments);
        
        updateShareProvider();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_link:
                handleLink();
                return true;

            case R.id.menu_comments:
                handleComments();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl();
                return true;
                
            case R.id.menu_open:
                handleOpen();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void updateShareProvider() {
        CharSequence title = thing.assureTitle(getActivity()).title;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, getLink());
        shareProvider.setShareIntent(intent);
    }

    private void handleLink() {
        getListener().onLinkSelected();
    }

    private void handleComments() {
        getListener().onCommentsSelected();
    }

    private void handleCopyUrl() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        CharSequence label = thing.assureTitle(getActivity()).title;
        CharSequence text = getLink();
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }
    
    private void handleOpen() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getLink().toString()));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_open)));
    }

    private CharSequence getLink() {
        return getListener().isShowingLink() ? thing.url : Urls.permaUrl(thing);
    }

    private ThingMenuListener getListener() {
        return (ThingMenuListener) getActivity();
    }
}
