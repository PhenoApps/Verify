package org.phenoapps.verify.utilities;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.SparseArray;


import org.apache.poi.ss.formula.functions.Value;
import org.phenoapps.verify.HomeFragment;
import org.phenoapps.verify.IdEntryContract;
import org.phenoapps.verify.IdEntryDbHelper;
import org.phenoapps.verify.ValueModel;

import java.util.ArrayList;
import java.util.HashSet;

public class IdEntryRepository {
    private final IdEntryDbHelper dbHelper;
    private SQLiteStatement sqlDeleteId;
    private SQLiteStatement sqlUpdateChecked;
    private SQLiteStatement sqlUpdateUserAndDate;
    private SQLiteStatement sqlUpdateNote;


    public IdEntryRepository(Context context) {
        dbHelper = new IdEntryDbHelper(context);
    }
    public void delete(String id) {
        if (sqlDeleteId == null){
            return;
        }
        sqlDeleteId.bindAllArgsAsStrings(new String[]{id});
        sqlDeleteId.executeUpdateDelete();

    }
    public HashSet<String> helperUpdateItems(String table, String[] columns, String selection, String listId) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        final HashSet<String> ids = new HashSet<>();
        try {
            final Cursor cursor = db.query(table, columns, selection, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(
                            cursor.getColumnIndexOrThrow(listId)
                    );

                    ids.add(id);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public ArrayList<ValueModel>[] fetchEntries(String table, String listId, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(table, null, listId + "=?", selectionArgs, null, null, null);
        String[] headerTokens = cursor.getColumnNames();
        ArrayList<ValueModel> values = new ArrayList<ValueModel>();
        ArrayList<ValueModel> auxValues = new ArrayList<ValueModel>();

        if (cursor.moveToFirst()) {
            for( String header:headerTokens) {
                if (!header.equals(listId)) {

                    final String val = cursor.getString(
                            cursor.getColumnIndexOrThrow(header)
                    );

                    if (header.equals("color") || header.equals("scan_count") || header.equals("date")
                            || header.equals("user") || header.equals("note")) {
                        ValueModel auxModel = new ValueModel();
                        if (header.equals("color")) continue;
                        else if (header.equals("scan_count")) auxModel.setPrefix("Number of scans");
                        else if (header.equals("date")) auxModel.setPrefix("Date");
                        else auxModel.setPrefix(header);
                        if (val != null) auxModel.setValue(val);
                        auxValues.add(auxModel);
                    } else {
                        ValueModel model = new ValueModel();
                        model.setPrefix(header);
                        if(val != null){
                            model.setValue(val);
                        }
                        values.add(model);
                    }
                }
            }

        }
        cursor.close();
        ArrayList<ValueModel>[] returnResult = new ArrayList[]{values, auxValues};
        return returnResult;

    }

    public String scanDb(String table, String[] columnsNames, String selection, String[] selectionArgs, String mPairCol) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(table, columnsNames, selection, selectionArgs, null, null, null);
        if(cursor.moveToFirst()) {
         String value = cursor.getString(
                    cursor.getColumnIndexOrThrow(mPairCol));
         cursor.close();
         return value;
        }
        return null;
    }

    public void executeUserUpdate(String name, String date, String id) {
        if (sqlUpdateUserAndDate == null){
            return;
        }
        sqlUpdateUserAndDate.bindAllArgsAsStrings(new String[]{
                name,
                date,
                id
        });
        sqlUpdateUserAndDate.executeUpdateDelete();
    }

    public void executeUpdate(String id) {
        if (sqlUpdateChecked == null){
            return;
        }
        sqlUpdateChecked.bindAllArgsAsStrings(new String[]{id});
        sqlUpdateChecked.executeUpdateDelete();
    }

    public Boolean checkExists(String table, String mListId, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        final Cursor cursor = db.query(table, null, mListId + "=?", selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }
    public void updateDb(String value, String id) {
        if (sqlUpdateNote != null) {
            sqlUpdateNote.bindAllArgsAsStrings(new String[]{
                    value, id
            });
            sqlUpdateNote.executeUpdateDelete();
        }
    }

    public void prepareStatements(String mListId) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String updateNoteQuery = "UPDATE VERIFY SET note = ? WHERE " + mListId + " = ?";
            sqlUpdateNote = db.compileStatement(updateNoteQuery);

            String deleteIdQuery = "DELETE FROM VERIFY WHERE " + mListId + " = ?";
            sqlDeleteId = db.compileStatement(deleteIdQuery);

            String updateCheckedQuery = "UPDATE VERIFY SET color = 1 WHERE " + mListId + " = ?";
            sqlUpdateChecked = db.compileStatement(updateCheckedQuery);

            String updateUserAndDateQuery =
                    "UPDATE VERIFY SET user = ?, date = ?, scan_count = scan_count + 1 WHERE " + mListId + " = ?";
            sqlUpdateUserAndDate = db.compileStatement(updateUserAndDateQuery);
        } catch(SQLiteException e) {
            e.printStackTrace();
        }
    }

    public SparseArray<String> loadBarcodes(String mListId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SparseArray<String> mIds = new SparseArray<>();
        try {
            final String table = IdEntryContract.IdEntry.TABLE_NAME;
            final Cursor cursor = db.query(table, null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    final String[] headers = cursor.getColumnNames();
                    for (String header : headers) {

                        final String val = cursor.getString(
                                cursor.getColumnIndexOrThrow(header)
                        );

                        if (header.equals(mListId)) {
                            mIds.append(mIds.size(), val);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();

        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return mIds;
    }

    public SQLiteStatement getSqlUpdateUserAndDate() {
        return  sqlUpdateUserAndDate;
    }

    public IdEntryDbHelper getmDbHelper() {
        return dbHelper;
    }

}
