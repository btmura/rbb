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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;

public class AccountSwitcher extends FrameLayout implements OnItemSelectedListener {

    public static final String TAG = "AccountSwitcher";

    public interface OnAccountSwitchListener {
        void onAccountSwitch(AccountSwitcherAdapter adapter, int position);
    }
    
    private TextView title;
    private Spinner spinner;
    private OnAccountSwitchListener listener;

    public AccountSwitcher(Context context) {
        super(context);
        init(context);
    }

    public AccountSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.account_switcher, this);
        title = (TextView) view.findViewById(R.id.title);
        spinner = (Spinner) view.findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        showTitle(true);
    }

    private void showTitle(boolean showTitle) {
        title.setVisibility(showTitle ? View.VISIBLE : View.INVISIBLE);
        spinner.setVisibility(showTitle ? View.INVISIBLE : View.VISIBLE);
    }

    public void setAdapter(AccountSwitcherAdapter adapter) {
        if (adapter != null) {
            adapter.registerDataSetObserver(observer);
        }
        spinner.setAdapter(adapter);        
    }

    final DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            if (Debug.DEBUG_WIDGETS) {
                Log.d(TAG, "onChanged");
            }            
            showTitle(getAdapter().hasNoAccounts());
        }

        @Override
        public void onInvalidated() {
            if (Debug.DEBUG_WIDGETS) {
                Log.d(TAG, "onInvalidated");
            }
            showTitle(true);
        }
    };
    
    public void setSelection(int position) {
        spinner.setSelection(position);
    }

    public void setOnAccountSwitchedListener(OnAccountSwitchListener listener) {
        this.listener = listener;
    }    
    
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (listener != null) {
            listener.onAccountSwitch(getAdapter(), position);
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }
    
    private AccountSwitcherAdapter getAdapter() {
        return (AccountSwitcherAdapter) spinner.getAdapter();
    }
}
