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
import org.phenoapps.verify.utilities.IdEntryRepository;

import java.util.HashSet;

public class HomeViewModel extends ViewModel {
//    private final IdEntryDbHelper mDbHelper;

    private final IdEntryRepository dbRepo;
    private String mListId;

//    private final Activity activity;

    private SparseArray<String> mIds;

    private String mPairCol;

    private String mNextPairVal;




    public HomeViewModel(Activity activity){
        dbRepo = new IdEntryRepository(activity);
        this.mNextPairVal = null;
    }

    public String getmNextPairVal() {
        return mNextPairVal;
    }

    public IdEntryDbHelper getmDbHelper() {
        return dbRepo.getmDbHelper();
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
        return dbRepo.getSqlUpdateUserAndDate();
    }

    public synchronized HashSet<String> updateCheckedItems() {


        //list of ideas to populate and update the view with
        final HashSet<String> ids = new HashSet<>();

        final String table = IdEntryContract.IdEntry.TABLE_NAME;
        final String[] columns = new String[] { mListId };
        final String selection = "color = 1";
        dbRepo.helperUpdateItems(table, columns, selection, mListId);
        return ids;
    }

    public synchronized void loadSQLToLocal(Activity activity) {

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

        String table = IdEntryContract.IdEntry.TABLE_NAME;
        String[] selectionArgs = new String[]{scannedId};

        return dbRepo.fetchEntries(table, mListId, selectionArgs);
    }

    private void loadBarcodes() {
       this.mIds = dbRepo.loadBarcodes(mListId);
    }

    public void executeScan(String id){
        String table = IdEntryContract.IdEntry.TABLE_NAME;
        String[] columnsNames = new String[] { mPairCol };
        String selection = mListId + "=?";
        String[] selectionArgs = { id };
        mNextPairVal = dbRepo.scanDb(table, columnsNames, selection, selectionArgs, mPairCol);

    }

    public String getmListId() {
        return mListId;
    }

    public void executeDelete(String id){
        dbRepo.delete(id);

    }

    public void executeUserUpdate(String name, String date, String id){
        dbRepo.executeUserUpdate(name, date, id);
    }

    public void executeUpdate(String id){
        dbRepo.executeUpdate(id);
    }

    public Boolean checkIdExists(String id) {

        final String table = IdEntryContract.IdEntry.TABLE_NAME;
        final String[] selectionArgs = new String[] { id };

        return dbRepo.checkExists(table, mListId, selectionArgs);
    }

    public void updateDb(String value, String id){

       dbRepo.updateDb(value, id);
    }

    public void prepareStatements() {
        dbRepo.prepareStatements(mListId);
    }
}
