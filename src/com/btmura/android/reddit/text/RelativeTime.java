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

package com.btmura.android.reddit.text;

import android.content.res.Resources;

import com.btmura.android.reddit.R;

import java.util.Formatter;
import java.util.Locale;

public class RelativeTime {

  private static final int MINUTE_SECONDS = 60;
  private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
  private static final int DAY_SECONDS = HOUR_SECONDS * 24;
  private static final int MONTH_SECONDS = DAY_SECONDS * 30;
  private static final int YEAR_SECONDS = MONTH_SECONDS * 12;

  public static Formatter format(
      Resources resources,
      Formatter formatter,
      long nowMs,
      long timeSec) {
    long diff = nowMs / 1000 - timeSec;
    int resId;
    double value;
    if ((value = diff / YEAR_SECONDS) > 0) {
      resId = R.plurals.time_years;
    } else if ((value = diff / MONTH_SECONDS) > 0) {
      resId = R.plurals.time_months;
    } else if ((value = diff / DAY_SECONDS) > 0) {
      resId = R.plurals.time_days;
    } else if ((value = diff / HOUR_SECONDS) > 0) {
      resId = R.plurals.time_hours;
    } else if ((value = diff / MINUTE_SECONDS) > 0) {
      resId = R.plurals.time_minutes;
    } else {
      resId = R.plurals.time_seconds;
      value = Math.max(0, diff);
    }

    Locale locale = resources.getConfiguration().locale;
    int quantity = (int) Math.round(value);
    String format = resources.getQuantityText(resId, quantity).toString();
    return formatter.format(locale, format, quantity);
  }
}
