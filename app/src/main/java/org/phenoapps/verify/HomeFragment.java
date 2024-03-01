package org.phenoapps.verify;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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

import org.phenoapps.verify.utilities.FileExport;
import org.phenoapps.verify.utilities.RingUtility;

import java.io.File;
import java.text.SimpleDateFormat;
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

    private RingUtility notification;
    private FileExport exportUtility;

    private View view;
    private Context context;
    private Activity activity;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIds = new SparseArray<>();

        this.view = view;
        context = getContext();
        activity = getActivity();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        final View auxInfo = view.findViewById(R.id.auxScrollView);
        final View auxValue = view.findViewById(R.id.auxValueView);

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

        notification = new RingUtility(context, view, activity.getPackageName(), getResources());

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

        ActivityCompat.requestPermissions(activity, VerifyConstants.permissions, VerifyConstants.PERM_REQ);

        mNextPairVal = null;
        mMatchingOrder = 0;
        mPairCol = null;

        initializeUIVariables();

        mDbHelper = new IdEntryDbHelper(context);

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
        if (activity instanceof AppCompatActivity) {
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

        final EditText scannerTextView = ((EditText) view.findViewById(R.id.scannerTextView));
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

        ListView idTable = ((ListView) view.findViewById(R.id.idTable));
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

        TextView valueView = (TextView) view.findViewById(R.id.valueView);
        valueView.setMovementMethod(new ScrollingMovementMethod());

        view.findViewById(org.phenoapps.verify.R.id.clearButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerTextView.setText("");
            }
        });
    }

    private synchronized void checkScannedItem() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));
        boolean displayAux = sharedPref.getBoolean(SettingsFragment.AUX_INFO, true);

        String scannedId = ((EditText) view.findViewById(org.phenoapps.verify.R.id.scannerTextView))
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
                ((TextView) view.findViewById(org.phenoapps.verify.R.id.valueView)).setText(values.toString());
                ((TextView) view.findViewById(R.id.auxValueView)).setText(auxValues.toString());
                ((EditText) view.findViewById(R.id.scannerTextView)).setText("");
            } else {
                if (scanMode != 2) {
                    notification.ringNotification(false);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter a note for the given item.");
        final EditText input = new EditText(context);
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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        if (scanMode == 0 ) { //default mode
            mMatchingOrder = 0;
            notification.ringNotification(checkIdExists(id));

        } else if (scanMode == 1) { //order mode
            final int tableIndex = getTableIndexById(id);

            if (tableIndex != -1) {
                if (mMatchingOrder == tableIndex) {
                    mMatchingOrder++;
                    Toast.makeText(context, "Order matches id: " + id + " at index: " + tableIndex, Toast.LENGTH_SHORT).show();
                    notification.ringNotification(true);
                } else {
                    Toast.makeText(context, "Scanning out of order!", Toast.LENGTH_SHORT).show();
                    notification.ringNotification(false);
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
                        notification.ringNotification(true);
                        Toast.makeText(context, "Scanned paired item: " + id, Toast.LENGTH_SHORT).show();
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
        ListView idTable = (ListView) view.findViewById(org.phenoapps.verify.R.id.idTable);
        for (int position = 0; position < idTable.getCount(); position++) {

            final String id = (idTable.getItemAtPosition(position)).toString();

            if (ids.contains(id)) {
                idTable.setItemChecked(position, true);
            } else idTable.setItemChecked(position, false);
        }
    }

    private synchronized void loadSQLToLocal() {

        mIds = new SparseArray<>();

        mDbHelper = new IdEntryDbHelper(context);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
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

    //returns index of table with identifier = id, returns -1 if not found
    private int getTableIndexById(String id) {

        ListView idTable = (ListView) view.findViewById(org.phenoapps.verify.R.id.idTable);
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

        ListView idTable = (ListView) view.findViewById(org.phenoapps.verify.R.id.idTable);
        //update id table array adapter
        final ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(context, org.phenoapps.verify.R.layout.row);
        final int oldSize = idTable.getAdapter().getCount();

        for (int i = 0; i < oldSize; i++) {
            final String temp = (String) idTable.getAdapter().getItem(i);
            if (!temp.equals(id)) updatedAdapter.add(temp);
        }
        idTable.setAdapter(updatedAdapter);
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
        int actionExport = R.id.action_export;

        if (item.getItemId() == actionImport){
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            final int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));
            final Intent i;
            File verifyDirectory = new File(context.getExternalFilesDir(null), "/Verify");

            File[] files = verifyDirectory.listFiles();


            AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

                            AlertDialog.Builder fileBuilder = new AlertDialog.Builder(context);
                            fileBuilder.setTitle("Select the sample file");
                            final int[] checkedItem = {-1};
                            String[] listItems = verifyDirectory.list();
                            Log.d("listItems", "onClick: "+listItems);
                            fileBuilder.setSingleChoiceItems(listItems, checkedItem[0],(fileDialog, which) -> {
                                checkedItem[0] = which;

                                Intent i = new Intent(context, LoaderDBActivity.class);
                                i.setData(Uri.fromFile(files[which]));
                                startActivityForResult(i, VerifyConstants.LOADER_INTENT_REQ);
                                fileDialog.dismiss();
                            });

                            fileBuilder.show();

                        }
                    });
            builder.show();
        } else if(item.getItemId() == actionCamera){
            final Intent cameraIntent = new Intent(context, ScanActivity.class);
            startActivityForResult(cameraIntent, VerifyConstants.CAMERA_INTENT_REQ);
        } else if (item.getItemId() == actionExport) {
            exportUtility = new FileExport(this.context, this.activity,this.mFileName, this.mDbHelper);
            exportUtility.askUserExportFileName();
        } else{
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    final public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK) {

            if (intent != null) {
                switch (requestCode) {
                    case VerifyConstants.PICK_CUSTOM_DEST:
                        exportUtility.writeToExportPath(intent.getData());
                        break;
                    case VerifyConstants.DEFAULT_CONTENT_REQ:
                        Intent i = new Intent(context, LoaderDBActivity.class);
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

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
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
                            Toast.makeText(context,
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
                    ((EditText) view.findViewById(org.phenoapps.verify.R.id.scannerTextView))
                            .setText(intent.getStringExtra(VerifyConstants.CAMERA_RETURN_ID));
                    checkScannedItem();
                }
            }
        }
    }

    private void buildListView() {

        ListView idTable = (ListView) view.findViewById(org.phenoapps.verify.R.id.idTable);
        ArrayAdapter<String> idAdapter =
                new ArrayAdapter<>(context, org.phenoapps.verify.R.layout.row);
        int size = mIds.size();
        for (int i = 0; i < size; i++) {
            idAdapter.add(this.mIds.get(this.mIds.keyAt(i)));
        }
        idTable.setAdapter(idAdapter);
    }

    private void clearListView() {

        ListView idTable = (ListView) view.findViewById(org.phenoapps.verify.R.id.idTable);
        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(context, org.phenoapps.verify.R.layout.row);

        idTable.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void showPairDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pair column selected, would you like to switch to Pair mode?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
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
                final Intent i = new Intent(context, IntroActivity.class);

                activity.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        startActivity(i);
                    }
                });


            }
        }).start();
    }

    /* Checks if external storage is available for read and write */
//    static private boolean isExternalStorageWritable() {
//        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
//    }

    @Override
    final public void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

}