package org.phenoapps.verify;

import android.provider.BaseColumns;

/**
 * Created by Chaney on 7/13/2017.
 */

public final class IdEntryContract {

    private IdEntryContract() {}

    public static class IdEntry implements BaseColumns {
        public static final String TABLE_NAME = "VERIFY";
        static final String COLUMN_NAME_ID = "id";
        static final String COLUMN_NAME_CHECKED = "checked";
        static final String COLUMN_NAME_SCANNED = "scanned";
        static final String COLUMN_NAME_USER = "user";
        static final String COLUMN_NAME_DATE = "date";
        static final String COLUMN_NAME_VALS = "vals";
        static final String COLUMN_NAME_PAIR = "pair";
    }

    static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + IdEntry.TABLE_NAME;

}
