package org.phenoapps.verify;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

public class HomeFragment extends Fragment {


    final static private String line_separator = System.getProperty("line.separator");

    private IdEntryDbHelper mDbHelper;

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    //database prepared statements
    private SQLiteStatement sqlUpdateNote;
    private SQLiteStatement sqlDeleteId;
    private SQLiteStatement sqlUpdateChecked;
    private SQLiteStatement sqlUpdateUserAndDate;

    private SparseArray<String> mIds;

    //global variable to track matching order
    private int mMatchingOrder;

    private String mListId;

    //pair mode vars
    private String mPairCol;
    private String mNextPairVal;

    private String mFileName = "";

    private Toolbar navigationToolBar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIds = new SparseArray<>();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        final View auxInfo = getView().findViewById(R.id.auxScrollView);
        final View auxValue = getView().findViewById(R.id.auxValueView);

        if (sharedPref.getBoolean(SettingsFragment.AUX_INFO, false)) {
            auxInfo.setVisibility(View.VISIBLE);
            auxValue.setVisibility(View.VISIBLE);

        } else {
            auxInfo.setVisibility(View.GONE);
            auxValue.setVisibility(View.GONE);
        }

        mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

                if (sharedPreferences.getBoolean(SettingsFragment.AUX_INFO, false)) {
                    auxInfo.setVisibility(View.VISIBLE);
                    auxValue.setVisibility(View.VISIBLE);
                } else {
                    auxInfo.setVisibility(View.GONE);
                    auxValue.setVisibility(View.GONE);
                }
            }
        };

        sharedPref.registerOnSharedPreferenceChangeListener(mPrefListener);

        if (!sharedPref.getBoolean("onlyLoadTutorialOnce", false)) {
            launchIntro();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("onlyLoadTutorialOnce", true);
            editor.apply();
        } else {
            boolean tutorialMode = sharedPref.getBoolean(SettingsFragment.TUTORIAL_MODE, false);

            if (tutorialMode)
                launchIntro();
        }

        mFileName = sharedPref.getString(SettingsFragment.FILE_NAME, "");

        ActivityCompat.requestPermissions(getActivity(), VerifyConstants.permissions, VerifyConstants.PERM_REQ);

        mNextPairVal = null;
        mMatchingOrder = 0;
        mPairCol = null;

        initializeUIVariables();

        mDbHelper = new IdEntryDbHelper(getContext());

        loadSQLToLocal();

        if (mListId != null)
            updateCheckedItems();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_home, container, false);
    }




    private void prepareStatements() {

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

    @Nullable
    private ActionBar getSupportActionBar() {
        ActionBar actionBar = null;
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            actionBar = activity.getSupportActionBar();
        }
        return actionBar;
    }

    private void initializeUIVariables() {

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("CheckList");
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        final EditText scannerTextView = ((EditText) getView().findViewById(R.id.scannerTextView));
        scannerTextView.setSelectAllOnFocus(true);
        scannerTextView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        checkScannedItem();
                    }
                }
                return false;
            }
        });

        ListView idTable = ((ListView) getView().findViewById(R.id.idTable));
        idTable.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        idTable.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                scannerTextView.setText(((TextView) view).getText().toString());
                scannerTextView.setSelection(scannerTextView.getText().length());
                scannerTextView.requestFocus();
                scannerTextView.selectAll();
                checkScannedItem();
            }
        });

        idTable.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//get app settings
                insertNoteIntoDb(((TextView) view).getText().toString());
                return true;
            }
        });

        TextView valueView = (TextView) getView().findViewById(R.id.valueView);
        valueView.setMovementMethod(new ScrollingMovementMethod());

        getView().findViewById(org.phenoapps.verify.R.id.clearButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerTextView.setText("");
            }
        });
    }

    private synchronized void checkScannedItem() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));
        boolean displayAux = sharedPref.getBoolean(SettingsFragment.AUX_INFO, true);

        String scannedId = ((EditText) getView().findViewById(org.phenoapps.verify.R.id.scannerTextView))
                .getText().toString();

        if (mIds != null && mIds.size() > 0) {
            //update database
            exertModeFunction(scannedId);

            //view updated database
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
                            auxValues.append(line_separator);
                        } else {
                            values.append(header);
                            values.append(" : ");
                            if (val != null) values.append(val);
                            values.append(line_separator);
                        }
                    }
                }
                cursor.close();
                ((TextView) getView().findViewById(org.phenoapps.verify.R.id.valueView)).setText(values.toString());
                ((TextView) getView().findViewById(R.id.auxValueView)).setText(auxValues.toString());
                ((EditText) getView().findViewById(R.id.scannerTextView)).setText("");
            } else {
                if (scanMode != 2) {
                    ringNotification(false);
                }
            }
        }
    }

    private Boolean checkIdExists(String id) {
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

    private synchronized void insertNoteIntoDb(@NonNull final String id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter a note for the given item.");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = input.getText().toString();
                if (!value.isEmpty()) {

                    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    if (sqlUpdateNote != null) {
                        sqlUpdateNote.bindAllArgsAsStrings(new String[]{
                                value, id
                        });
                        sqlUpdateNote.executeUpdateDelete();
                    }
                }
            }
        });

        builder.show();
    }

    private synchronized void exertModeFunction(@NonNull String id) {

        //get app settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        if (scanMode == 0 ) { //default mode
            mMatchingOrder = 0;
            ringNotification(checkIdExists(id));

        } else if (scanMode == 1) { //order mode
            final int tableIndex = getTableIndexById(id);

            if (tableIndex != -1) {
                if (mMatchingOrder == tableIndex) {
                    mMatchingOrder++;
                    Toast.makeText(getContext(), "Order matches id: " + id + " at index: " + tableIndex, Toast.LENGTH_SHORT).show();
                    ringNotification(true);
                } else {
                    Toast.makeText(getContext(), "Scanning out of order!", Toast.LENGTH_SHORT).show();
                    ringNotification(false);
                }
            }
        } else if (scanMode == 2) { //filter mode, delete rows with given id

            mMatchingOrder = 0;
            if (sqlDeleteId != null) {
                sqlDeleteId.bindAllArgsAsStrings(new String[]{id});
                sqlDeleteId.executeUpdateDelete();
            }
            updateFilteredArrayAdapter(id);

        } else if (scanMode == 3) { //if color mode, update the db to highlight the item

            mMatchingOrder = 0;
            if (sqlUpdateChecked != null) {
                sqlUpdateChecked.bindAllArgsAsStrings(new String[]{id});
                sqlUpdateChecked.executeUpdateDelete();
            }
        } else if (scanMode == 4) { //pair mode

            mMatchingOrder = 0;

            if (mPairCol != null) {

                //if next pair id is waiting, check if it matches scanned id and reset mode
                if (mNextPairVal != null) {
                    if (mNextPairVal.equals(id)) {
                        ringNotification(true);
                        Toast.makeText(getContext(), "Scanned paired item: " + id, Toast.LENGTH_SHORT).show();
                    }
                    mNextPairVal = null;
                } else { //otherwise query for the current id's pair
                    String table = IdEntryContract.IdEntry.TABLE_NAME;
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
            }
        }
        //always update user and datetime
        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());

        if (sqlUpdateUserAndDate != null) { //no db yet
            String name = sharedPref.getString(SettingsFragment.NAME, "");
            sqlUpdateUserAndDate.bindAllArgsAsStrings(new String[]{
                    name,
                    sdf.format(c.getTime()),
                    id
            });
            sqlUpdateUserAndDate.executeUpdateDelete();
        }

        updateCheckedItems();
    }

    private synchronized void updateCheckedItems() {

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
        ListView idTable = (ListView) getView().findViewById(org.phenoapps.verify.R.id.idTable);
        for (int position = 0; position < idTable.getCount(); position++) {

            final String id = (idTable.getItemAtPosition(position)).toString();

            if (ids.contains(id)) {
                idTable.setItemChecked(position, true);
            } else idTable.setItemChecked(position, false);
        }
    }

    private synchronized void loadSQLToLocal() {

        mIds = new SparseArray<>();

        mDbHelper = new IdEntryDbHelper(getContext());

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mListId = sharedPref.getString(SettingsFragment.LIST_KEY_NAME, null);
        mPairCol = sharedPref.getString(SettingsFragment.PAIR_NAME, null);

        if (mListId != null) {
            prepareStatements();
            loadBarcodes();
            buildListView();
        }
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

    private synchronized void askUserExportFileName() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose name for exported file.");
        final EditText input = new EditText(getContext());

        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int lastDot = mFileName.lastIndexOf('.');
        if (lastDot != -1) {
            mFileName = mFileName.substring(0, lastDot);
        }
        input.setText("Verify_"+ sdf.format(c.getTime()));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Export", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                String value = input.getText().toString();
                mFileName = value;
                final Intent i;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_TITLE, value+".csv");
                    startActivityForResult(Intent.createChooser(i, "Choose folder to export file."), VerifyConstants.PICK_CUSTOM_DEST);
                }else{
                    writeToExportPath();
                }
            }
        });
        builder.show();
    }

    public void writeToExportPath(){
        String value = mFileName;

        if (!value.isEmpty()) {
            if (isExternalStorageWritable()) {
                try {
                    File verifyDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/Verify");
                    final File output = new File(verifyDirectory, value + ".csv");
                    final FileOutputStream fstream = new FileOutputStream(output);
                    final SQLiteDatabase db = mDbHelper.getReadableDatabase();
                    final String table = IdEntryContract.IdEntry.TABLE_NAME;
                    final Cursor cursor = db.query(table, null, null, null, null, null, null);
                    //final Cursor cursor = db.rawQuery("SElECT * FROM VERIFY", null);

                    //first write header line
                    final String[] headers = cursor.getColumnNames();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != 0) fstream.write(",".getBytes());
                        fstream.write(headers[i].getBytes());
                    }
                    fstream.write(line_separator.getBytes());
                    //populate text file with current database values
                    if (cursor.moveToFirst()) {
                        do {
                            for (int i = 0; i < headers.length; i++) {
                                if (i != 0) fstream.write(",".getBytes());
                                final String val = cursor.getString(
                                        cursor.getColumnIndexOrThrow(headers[i])
                                );
                                if (val == null) fstream.write("null".getBytes());
                                else fstream.write(val.getBytes());
                            }
                            fstream.write(line_separator.getBytes());
                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                    fstream.flush();
                    fstream.close();
                    scanFile(getContext(), output);
                            /*MediaScannerConnection.scanFile(getContext(), new String[] {output.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.v("scan complete", path);
                                }
                            });*/
                }catch (NullPointerException npe){
                    npe.printStackTrace();
                    Toast.makeText(getContext(), "Error in opening the Specified file", Toast.LENGTH_LONG).show();
                }
                catch (SQLiteException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error exporting file, is your table empty?", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException io) {
                    io.printStackTrace();
                }
            } else {
                Toast.makeText(getContext(),
                        "External storage not writable.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(),
                    "Must enter a file name.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void scanFile(Context ctx, File filePath) {
        MediaScannerConnection.scanFile(ctx, new String[] { filePath.getAbsolutePath()}, null, null);
    }

    public void writeToExportPath(Uri uri){

        String value = mFileName;

        if (uri == null){
            Toast.makeText(getContext(), "Unable to open the Specified file", Toast.LENGTH_LONG).show();
            return;
        }

        if (!value.isEmpty()) {
            if (isExternalStorageWritable()) {
                try {
                    final File output = new File(uri.getPath());
                    final OutputStream fstream = getContext().getContentResolver().openOutputStream(uri);
                    final SQLiteDatabase db = mDbHelper.getReadableDatabase();
                    final String table = IdEntryContract.IdEntry.TABLE_NAME;
                    final Cursor cursor = db.query(table, null, null, null, null, null, null);
                    //final Cursor cursor = db.rawQuery("SElECT * FROM VERIFY", null);

                    //first write header line
                    final String[] headers = cursor.getColumnNames();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != 0) fstream.write(",".getBytes());
                        fstream.write(headers[i].getBytes());
                    }
                    fstream.write(line_separator.getBytes());
                    //populate text file with current database values
                    if (cursor.moveToFirst()) {
                        do {
                            for (int i = 0; i < headers.length; i++) {
                                if (i != 0) fstream.write(",".getBytes());
                                final String val = cursor.getString(
                                        cursor.getColumnIndexOrThrow(headers[i])
                                );
                                if (val == null) fstream.write("null".getBytes());
                                else fstream.write(val.getBytes());
                            }
                            fstream.write(line_separator.getBytes());
                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                    fstream.flush();
                    fstream.close();
                    scanFile(getContext(), output);
                            /*MediaScannerConnection.scanFile(getContext(), new String[] {output.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.v("scan complete", path);
                                }
                            });*/
                }catch (NullPointerException npe){
                    npe.printStackTrace();
                    Toast.makeText(getContext(), "Error in opening the Specified file", Toast.LENGTH_LONG).show();
                }
                catch (SQLiteException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error exporting file, is your table empty?", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException io) {
                    io.printStackTrace();
                }
            } else {
                Toast.makeText(getContext(),
                        "External storage not writable.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(),
                    "Must enter a file name.", Toast.LENGTH_SHORT).show();
        }
    }

    //returns index of table with identifier = id, returns -1 if not found
    private int getTableIndexById(String id) {

        ListView idTable = (ListView) getView().findViewById(org.phenoapps.verify.R.id.idTable);
        final int size = idTable.getAdapter().getCount();
        int ret = -1;
        for (int i = 0; i < size; i++) {
            final String temp = (String) idTable.getAdapter().getItem(i);
            if (temp.equals(id)) {
                ret = i;
                break; //break out of for-loop early
            }
        }

        return ret;
    }

    private void updateFilteredArrayAdapter(String id) {

        ListView idTable = (ListView) getView().findViewById(org.phenoapps.verify.R.id.idTable);
        //update id table array adapter
        final ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(getContext(), org.phenoapps.verify.R.layout.row);
        final int oldSize = idTable.getAdapter().getCount();

        for (int i = 0; i < oldSize; i++) {
            final String temp = (String) idTable.getAdapter().getItem(i);
            if (!temp.equals(id)) updatedAdapter.add(temp);
        }
        idTable.setAdapter(updatedAdapter);
    }

    private void ringNotification(boolean success) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        final boolean audioEnabled = sharedPref.getBoolean(SettingsFragment.AUDIO_ENABLED, true);

        if(success) { //ID found
            if(audioEnabled) {
                if (success) {
                    try {
                        int resID = getResources().getIdentifier("plonk", "raw", getActivity().getPackageName());
                        MediaPlayer chimePlayer = MediaPlayer.create(getContext(), resID);
                        chimePlayer.start();

                        chimePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                            }
                        });
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        if(!success) { //ID not found
            ((TextView) getView().findViewById(org.phenoapps.verify.R.id.valueView)).setText("");

            if (audioEnabled) {
                if(!success) {
                    try {
                        int resID = getResources().getIdentifier("error", "raw", getActivity().getPackageName());
                        MediaPlayer chimePlayer = MediaPlayer.create(getContext(), resID);
                        chimePlayer.start();

                        chimePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                            }
                        });
                    } catch (Exception ignore) {
                    }
                }
            } else {
                if (!success) {
                    Toast.makeText(getContext(), "Scanned ID not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(org.phenoapps.verify.R.menu.activity_main_toolbar, menu);
    }

        @Override
    final public boolean onOptionsItemSelected(MenuItem item) {
        int actionCamera = R.id.action_camera;
        int actionImport = R.id.action_import;

        if (item.getItemId() == actionImport){
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            final int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));
            final Intent i;
            File verifyDirectory = new File(getContext().getExternalFilesDir(null), "/Verify");

            File[] files = verifyDirectory.listFiles();


            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select files from?");
            builder.setPositiveButton("Storage",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            Intent i;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            }else{
                                i = new Intent(Intent.ACTION_GET_CONTENT);
                            }
                            i.setType("*/*");
                            startActivityForResult(Intent.createChooser(i, "Choose file to import."), VerifyConstants.DEFAULT_CONTENT_REQ);
                        }
                    });

            builder.setNegativeButton("Verify Directory",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {

                            AlertDialog.Builder fileBuilder = new AlertDialog.Builder(getContext());
                            fileBuilder.setTitle("Select the sample file");
                            final int[] checkedItem = {-1};
                            String[] listItems = verifyDirectory.list();
                            Log.d("listItems", "onClick: "+listItems);
                            fileBuilder.setSingleChoiceItems(listItems, checkedItem[0],(fileDialog, which) -> {
                                checkedItem[0] = which;

                                Intent i = new Intent(getContext(), LoaderDBActivity.class);
                                i.setData(Uri.fromFile(files[which]));
                                startActivityForResult(i, VerifyConstants.LOADER_INTENT_REQ);
                                fileDialog.dismiss();
                            });

                            fileBuilder.show();

                        }
                    });
            builder.show();
        } else if(item.getItemId() == actionCamera){
            final Intent cameraIntent = new Intent(getContext(), ScanActivity.class);
            startActivityForResult(cameraIntent, VerifyConstants.CAMERA_INTENT_REQ);
        }
        else{
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    final public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == getActivity().RESULT_OK) {

            if (intent != null) {
                switch (requestCode) {
                    case VerifyConstants.PICK_CUSTOM_DEST:
                        writeToExportPath(intent.getData());
                        break;
                    case VerifyConstants.DEFAULT_CONTENT_REQ:
                        Intent i = new Intent(getContext(), LoaderDBActivity.class);
                        i.setData(intent.getData());
                        startActivityForResult(i, VerifyConstants.LOADER_INTENT_REQ);
                        break;
                    case VerifyConstants.LOADER_INTENT_REQ:

                        mListId = null;
                        mPairCol = null;
                        mFileName = "";

                        if (intent.hasExtra(VerifyConstants.FILE_NAME))
                            mFileName = intent.getStringExtra(VerifyConstants.FILE_NAME);
                        if (intent.hasExtra(VerifyConstants.LIST_ID_EXTRA))
                            mListId = intent.getStringExtra(VerifyConstants.LIST_ID_EXTRA);
                        if (intent.hasExtra(VerifyConstants.PAIR_COL_EXTRA))
                            mPairCol = intent.getStringExtra(VerifyConstants.PAIR_COL_EXTRA);

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                        final SharedPreferences.Editor editor = sharedPref.edit();

                        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));

                        if (mPairCol != null) {
                            editor.putBoolean(SettingsFragment.DISABLE_PAIR, false);
                            if (scanMode != 4) showPairDialog();
                        } else {
                            editor.putBoolean(SettingsFragment.DISABLE_PAIR, true);
                        }

                        if (mPairCol == null && scanMode == 4) {
                            editor.putString(SettingsFragment.SCAN_MODE_LIST, "0");
                            Toast.makeText(getContext(),
                                    "Switching to default mode, no pair ID found.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        editor.putString(SettingsFragment.FILE_NAME, mFileName);
                        editor.putString(SettingsFragment.PAIR_NAME, mPairCol);
                        editor.putString(SettingsFragment.LIST_KEY_NAME, mListId);
                        editor.apply();

                        clearListView();
                        loadSQLToLocal();
                        updateCheckedItems();
                        break;
                }

                if (intent.hasExtra(VerifyConstants.CAMERA_RETURN_ID)) {
                    ((EditText) getView().findViewById(org.phenoapps.verify.R.id.scannerTextView))
                            .setText(intent.getStringExtra(VerifyConstants.CAMERA_RETURN_ID));
                    checkScannedItem();
                }
            }
        }
    }

    private void buildListView() {

        ListView idTable = (ListView) getView().findViewById(org.phenoapps.verify.R.id.idTable);
        ArrayAdapter<String> idAdapter =
                new ArrayAdapter<>(getContext(), org.phenoapps.verify.R.layout.row);
        int size = mIds.size();
        for (int i = 0; i < size; i++) {
            idAdapter.add(this.mIds.get(this.mIds.keyAt(i)));
        }
        idTable.setAdapter(idAdapter);
    }

    private void clearListView() {

        ListView idTable = (ListView) getView().findViewById(org.phenoapps.verify.R.id.idTable);
        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getContext(), org.phenoapps.verify.R.layout.row);

        idTable.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void showPairDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Pair column selected, would you like to switch to Pair mode?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(SettingsFragment.SCAN_MODE_LIST, "4");
                editor.apply();
            }
        });

        builder.setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }

    @Override
    final public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void launchIntro() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                //  Launch app intro
                final Intent i = new Intent(getContext(), IntroActivity.class);

                getActivity().runOnUiThread(new Runnable() {
                    @Override public void run() {
                        startActivity(i);
                    }
                });


            }
        }).start();
    }

    /* Checks if external storage is available for read and write */
    static private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    @Override
    final public void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

}