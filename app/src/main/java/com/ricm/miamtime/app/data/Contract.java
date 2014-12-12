package com.ricm.miamtime.app.data;

import android.provider.BaseColumns;
import android.content.ContentUris;
import android.net.Uri;

/**
 * Created by eudes on 12/12/14.
 */
public class Contract {
    /* Inner class that defines the table contents of the weather table */
    public static final class Places_Entry implements BaseColumns {
        public static final String TABLE_NAME = "places";
        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Nom du resto
        public static final String COLUMN_NAME_PLACE ="name";
        // rating
        public static final String COLUMN_RATING_PLACE = "rating";
        // address
        public static final String COLUMN_ADDR = "address";
    }
}
