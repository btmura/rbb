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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class AccountSwitcher extends FrameLayout {
    
    public static final String TAG = "AccountSwitcher";

    private TextView title;
    private Spinner spinner;
    
    public AccountSwitcher(Context context) {
        super(context);
        init(context);
    }
    
    public AccountSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.account_switcher, this);        
        title = (TextView) view.findViewById(R.id.title);        
        spinner = (Spinner) view.findViewById(R.id.spinner);        
        updateViews(0);
    }
    
    void updateViews(int numAccounts) {
        title.setVisibility(numAccounts > 0 ? View.GONE : View.VISIBLE);
        spinner.setVisibility(numAccounts > 0 ? View.VISIBLE : View.GONE);        
    }
    
    public void setAdapter(SpinnerAdapter adapter) {
        if (adapter != null) {
            adapter.registerDataSetObserver(observer);
        }
        spinner.setAdapter(adapter);        
    }

    final DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            updateViews(spinner.getCount());
        }
        
        @Override
        public void onInvalidated() {
            updateViews(0);
        }
    };
}
