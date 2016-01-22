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

package com.btmura.android.reddit.content;

import com.btmura.android.reddit.database.Things;

/**
 * Interface that defines constants for a commonly used projection on the things
 * table.
 */
public interface ThingProjection {

  String[] PROJECTION = {
      Things._ID,
      Things.COLUMN_AUTHOR,
      Things.COLUMN_BODY,
      Things.COLUMN_CREATED_UTC,
      Things.COLUMN_DOMAIN,
      Things.COLUMN_DOWNS,
      Things.COLUMN_HIDDEN,
      Things.COLUMN_KIND,
      Things.COLUMN_LIKES,
      Things.COLUMN_LINK_ID,
      Things.COLUMN_LINK_TITLE,
      Things.COLUMN_NUM_COMMENTS,
      Things.COLUMN_OVER_18,
      Things.COLUMN_PERMA_LINK,
      Things.COLUMN_SAVED,
      Things.COLUMN_SCORE,
      Things.COLUMN_SELF,
      Things.COLUMN_SUBREDDIT,
      Things.COLUMN_TITLE,
      Things.TABLE_NAME + "." + Things.COLUMN_THING_ID,
      Things.COLUMN_THUMBNAIL_URL,
      Things.COLUMN_UPS,
      Things.COLUMN_URL,
  };

  int INDEX_AUTHOR = 1;
  int INDEX_BODY = 2;
  int INDEX_CREATED_UTC = 3;
  int INDEX_DOMAIN = 4;
  int INDEX_DOWNS = 5;
  int INDEX_HIDDEN = 6;
  int INDEX_KIND = 7;
  int INDEX_LIKES = 8;
  int INDEX_LINK_ID = 9;
  int INDEX_LINK_TITLE = 10;
  int INDEX_NUM_COMMENTS = 11;
  int INDEX_OVER_18 = 12;
  int INDEX_PERMA_LINK = 13;
  int INDEX_SAVED = 14;
  int INDEX_SCORE = 15;
  int INDEX_SELF = 16;
  int INDEX_SUBREDDIT = 17;
  int INDEX_TITLE = 18;
  int INDEX_THING_ID = 19;
  int INDEX_THUMBNAIL_URL = 20;
  int INDEX_UPS = 21;
  int INDEX_URL = 22;
}
