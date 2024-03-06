package org.phenoapps.verify.ViewModel;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.SparseArray;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModel;

import org.phenoapps.verify.HomeFragment;
import org.phenoapps.verify.IdEntryContract;
import org.phenoapps.verify.IdEntryDbHelper;
import org.phenoapps.verify.R;
import org.phenoapps.verify.SettingsFragment;

import java.util.HashSet;

public class HomeViewModel extends ViewModel {
    private final IdEntryDbHelper mDbHelper;

    private String mListId;

    private final Activity activity;

    private SparseArray<String> mIds;

    private String mPairCol;

    private String mNextPairVal;
    private SQLiteStatement sqlDeleteId;
    private SQLiteStatement sqlUpdateChecked;
    private SQLiteStatement sqlUpdateUserAndDate;


    private SQLiteStatement sqlUpdateNote;

    public HomeViewModel(Activity activity){
        mDbHelper = new IdEntryDbHelper(activity);
        this.activity = activity;
        this.mNextPairVal = null;
    }

    public String getmNextPairVal() {
        return mNextPairVal;
    }

    public IdEntryDbHelper getmDbHelper() {
        return mDbHelper;
    }

    public void setmListId(String mListId) {
        this.mListId = mListId;
    }

    public SparseArray<String> getmIds() {
        return mIds;
    }

    public String getmPairCol() {
        return mPairCol;
    }

    public void setmPairCol(String mPairCol) {
        this.mPairCol = mPairCol;
    }

    public void setmIds(SparseArray<String> mIds) {
        this.mIds = mIds;
    }

    public void setmNextPairVal(String mNextPairVal) {
        this.mNextPairVal = mNextPairVal;
    }

    public SQLiteStatement getSqlUpdateUserAndDate() {
        return sqlUpdateUserAndDate;
    }

    public synchronized HashSet<String> updateCheckedItems() {

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        //list of ideas to populate and update the view with
        final HashSet<String> ids = new HashSet<>();

        final String table = IdEntryContract.IdEntry.TABLE_NAME;
        final String[] columns = new String[] { mListId };
        final String selection = "color = 1";

        try {
            final Cursor cursor = db.query(table, columns, selection, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(
                            cursor.getColumnIndexOrThrow(mListId)
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

    public synchronized void loadSQLToLocal() {

        mIds = new SparseArray<>();


        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        mListId = sharedPref.getString(SettingsFragment.LIST_KEY_NAME, null);
        mPairCol = sharedPref.getString(SettingsFragment.PAIR_NAME, null);

        if (mListId != null) {
            prepareStatements();
            loadBarcodes();
        }
    }

    public StringBuilder[] getData(String scannedId) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String table = IdEntryContract.IdEntry.TABLE_NAME;
        String[] selectionArgs = new String[]{scannedId};
        Cursor cursor = db.query(table, null, mListId + "=?", selectionArgs, null, null, null);

        String[] headerTokens = cursor.getColumnNames();
        StringBuilder values = new StringBuilder();
        StringBuilder auxValues = new StringBuilder();

        if (cursor.moveToFirst()) {
            for (String header : headerTokens) {

                if (!header.equals(mListId)) {

                    final String val = cursor.getString(
                            cursor.getColumnIndexOrThrow(header)
                    );

                    if (header.equals("color") || header.equals("scan_count") || header.equals("date")
                            || header.equals("user") || header.equals("note")) {
                        if (header.equals("color")) continue;
                        else if (header.equals("scan_count")) auxValues.append("Number of scans");
                        else if (header.equals("date")) auxValues.append("Date");
                        else auxValues.append(header);
                        auxValues.append(" : ");
                        if (val != null) auxValues.append(val);
                        auxValues.append(HomeFragment.line_separator);
                    } else {
                        values.append(header);
                        values.append(" : ");
                        if (val != null) values.append(val);
                        values.append(HomeFragment.line_separator);
                    }
                }
            }
            cursor.close();
        }
        return new StringBuilder[]{values, auxValues};
    }

    private void loadBarcodes() {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
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
    }

    public void executeScan(String id){
        String table = IdEntryContract.IdEntry.TABLE_NAME;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String[] columnsNames = new String[] { mPairCol };
        String selection = mListId + "=?";
        String[] selectionArgs = { id };
        Cursor cursor = db.query(table, columnsNames, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            mNextPairVal = cursor.getString(
                    cursor.getColumnIndexOrThrow(mPairCol)
            );
        } else mNextPairVal = null;
        cursor.close();
    }

    public String getmListId() {
        return mListId;
    }

    public void executeDelete(String id){
        if (sqlDeleteId == null){
            return;
        }
        sqlDeleteId.bindAllArgsAsStrings(new String[]{id});
        sqlDeleteId.executeUpdateDelete();
    }

    public void executeUserUpdate(String name, String date, String id){
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

    public void executeUpdate(String id){
        if (sqlUpdateChecked == null){
            return;
        }
        sqlUpdateChecked.bindAllArgsAsStrings(new String[]{id});
        sqlUpdateChecked.executeUpdateDelete();
    }

    public Boolean checkIdExists(String id) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        final String table = IdEntryContract.IdEntry.TABLE_NAME;
        final String[] selectionArgs = new String[] { id };
        final Cursor cursor = db.query(table, null, mListId + "=?", selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public void updateDb(String value, String id){
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        if (sqlUpdateNote != null) {
            sqlUpdateNote.bindAllArgsAsStrings(new String[]{
                    value, id
            });
            sqlUpdateNote.executeUpdateDelete();
        }
    }

    public void prepareStatements() {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
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
}
