package org.phenoapps.verify;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.phenoapps.verify.ViewModel.HomeViewModel;
import org.phenoapps.verify.utilities.FileExport;
import org.phenoapps.verify.utilities.IntentHelper;
import org.phenoapps.verify.utilities.RingUtility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

public class HomeFragment extends Fragment implements RingUtility, IntentHelper {

    final static public String line_separator = System.getProperty("line.separator");

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    private int mMatchingOrder;
    private String mFileName = "";

    private Toolbar navigationToolBar;
    private RecyclerView valueView;

    private int selectedIndex = -1;
    private ArrayAdapter<String> idAdapter;

    private CustomAdapter valuesAdapter;
    private FileExport exportUtility;

    private HomeViewModel homeViewModel;

    private View view;
    private Context context;
    private Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        context = getContext();
        activity = getActivity();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        valuesAdapter = new CustomAdapter(new ArrayList<ValueModel>());
        valuesAdapter.setAuxValues(sharedPref.getBoolean(SettingsFragment.AUX_INFO, false));

        mPrefListener = (sharedPreferences, key) -> {
            if (SettingsFragment.AUX_INFO.equals(key)) {
                valuesAdapter.setAuxValues(sharedPreferences.getBoolean(SettingsFragment.AUX_INFO, false));
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(mPrefListener);

        mFileName = sharedPref.getString(SettingsFragment.FILE_NAME, "");

        ActivityCompat.requestPermissions(activity, VerifyConstants.permissions, VerifyConstants.PERM_REQ);
        mMatchingOrder = 0;

        initializeUIVariables();

        homeViewModel = new HomeViewModel(activity);
        homeViewModel.loadSQLToLocal(activity);
        buildListView();

        if (homeViewModel.getmListId() != null) {
            HashSet<String> ids = homeViewModel.updateCheckedItems();
            updateTable(ids);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Nullable
    private ActionBar getSupportActionBar() {
        ActionBar actionBar = null;
        if (activity instanceof AppCompatActivity) {
            actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        }
        return actionBar;
    }

    private void initializeUIVariables() {

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        final EditText scannerTextView = view.findViewById(R.id.scannerTextView);
        scannerTextView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                checkScannedItem();
            }
            return false;
        });

        ListView idTable = view.findViewById(R.id.idTable);
        idTable.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        idTable.setOnItemClickListener((parent, v, position, id) -> {
            selectedIndex = position;
            idAdapter.notifyDataSetChanged();
            String text = ((TextView) v).getText().toString();
            Log.d("EditText", "onItemClick: " + text);
            scannerTextView.setText(text);
            scannerTextView.setSelection(scannerTextView.getText().length());
            scannerTextView.requestFocus();
            scannerTextView.selectAll();
            checkScannedItem();
        });

        idTable.setOnItemLongClickListener((parent, v, position, id) -> {
            insertNoteIntoDb(((TextView) v).getText().toString());
            return true;
        });

        valueView = view.findViewById(R.id.valueView);
        valueView.setLayoutManager(new LinearLayoutManager(context));
        valueView.setAdapter(valuesAdapter);

        // Search end button: shows barcode icon when empty, clear icon when text is present
        ImageButton searchEndButton = view.findViewById(R.id.searchEndButton);
        scannerTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchEndButton.setImageResource(R.drawable.ic_clear);
                } else {
                    searchEndButton.setImageResource(R.drawable.barcode_scan);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchEndButton.setOnClickListener(v -> {
            if (scannerTextView.getText().length() > 0) {
                scannerTextView.setText("");
            } else {
                final Intent cameraIntent = new Intent(context, ScanActivity.class);
                startActivityForResult(cameraIntent, VerifyConstants.CAMERA_INTENT_REQ);
            }
        });

        // Import FAB — goes straight to SAF
        FloatingActionButton importFab = view.findViewById(R.id.importFab);
        importFab.setOnClickListener(v -> launchSafImport());
    }

    /** Open the system file picker (SAF) directly — no intermediate dialog. */
    private void launchSafImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        startActivityForResult(
                Intent.createChooser(i, getString(R.string.choose_file_to_import)),
                VerifyConstants.DEFAULT_CONTENT_REQ);
    }

    /** Kick off a SAF export directly — no in-app filename dialog. */
    private void launchSafExport() {
        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String base = mFileName;
        int lastDot = base.lastIndexOf('.');
        if (lastDot != -1) base = base.substring(0, lastDot);
        if (base.isEmpty()) base = "Verify";
        final String exportName = "Verify_" + sdf.format(c.getTime()) + ".csv";

        exportUtility = new FileExport(this.activity, base, this.homeViewModel.getmDbHelper());

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_TITLE, exportName);
        startIntent(i);
    }

    private synchronized void checkScannedItem() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));

        String scannedId = ((EditText) view.findViewById(R.id.scannerTextView)).getText().toString();

        if (!scannedId.isEmpty() && homeViewModel.getmIds() != null && homeViewModel.getmIds().size() > 0) {
            exertModeFunction(scannedId);
            ArrayList<ValueModel> values = homeViewModel.getData(scannedId);

            if (values.size() > 0) {
                valuesAdapter.values = values;
                valuesAdapter.notifyDataSetChanged();
            } else {
                clearRecyclerViews();
                if (scanMode != 2) {
                    this.ringNotification(false);
                }
            }
        } else {
            clearRecyclerViews();
        }
    }

    private void clearRecyclerViews() {
        valuesAdapter.values.clear();
        valuesAdapter.notifyDataSetChanged();
    }

    private synchronized void insertNoteIntoDb(@NonNull final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.insert_note_title);
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String value = input.getText().toString();
            if (!value.isEmpty()) homeViewModel.updateDb(value, id);
        });
        builder.show();
    }

    private synchronized void exertModeFunction(@NonNull String id) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));

        if (scanMode == 0) {
            mMatchingOrder = 0;
            this.ringNotification(homeViewModel.checkIdExists(id));
        } else if (scanMode == 1) {
            final int tableIndex = getTableIndexById(id);
            if (tableIndex != -1) {
                if (mMatchingOrder == tableIndex) {
                    mMatchingOrder++;
                    Toast.makeText(context, getString(R.string.order_matches, id, tableIndex), Toast.LENGTH_SHORT).show();
                    this.ringNotification(true);
                } else {
                    Toast.makeText(context, R.string.scanning_out_of_order, Toast.LENGTH_SHORT).show();
                    this.ringNotification(false);
                }
            }
        } else if (scanMode == 2) {
            mMatchingOrder = 0;
            homeViewModel.executeDelete(id);
            updateFilteredArrayAdapter(id);
        } else if (scanMode == 3) {
            mMatchingOrder = 0;
            homeViewModel.executeUpdate(id);
        } else if (scanMode == 4) {
            mMatchingOrder = 0;
            String mPairCol = homeViewModel.getmPairCol();
            if (mPairCol != null) {
                String mNextPairVal = homeViewModel.getmNextPairVal();
                if (mNextPairVal != null) {
                    if (mNextPairVal.equals(id)) {
                        this.ringNotification(true);
                        Toast.makeText(context, getString(R.string.scanned_paired_item, id), Toast.LENGTH_SHORT).show();
                    }
                    homeViewModel.setmNextPairVal(null);
                } else {
                    homeViewModel.executeScan(id);
                }
            }
        }

        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        if (homeViewModel.getSqlUpdateUserAndDate() != null) {
            String name = sharedPref.getString(SettingsFragment.NAME, "");
            homeViewModel.executeUserUpdate(name, sdf.format(c.getTime()), id);
        }

        HashSet<String> ids = homeViewModel.updateCheckedItems();
        updateTable(ids);
    }

    private void updateTable(HashSet<String> ids) {
        ListView idTable = view.findViewById(R.id.idTable);
        for (int position = 0; position < idTable.getCount(); position++) {
            final String id = idTable.getItemAtPosition(position).toString();
            idTable.setItemChecked(position, ids.contains(id));
        }
    }

    private int getTableIndexById(String id) {
        ListView idTable = view.findViewById(R.id.idTable);
        final int size = idTable.getAdapter().getCount();
        for (int i = 0; i < size; i++) {
            if (((String) idTable.getAdapter().getItem(i)).equals(id)) return i;
        }
        return -1;
    }

    private void updateFilteredArrayAdapter(String id) {
        ListView idTable = view.findViewById(R.id.idTable);
        int index = idTable.getFirstVisiblePosition();
        View v = idTable.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - idTable.getPaddingTop());

        ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(context, R.layout.row);
        int oldSize = idTable.getAdapter().getCount();
        for (int i = 0; i < oldSize; i++) {
            String temp = (String) idTable.getAdapter().getItem(i);
            if (!temp.equals(id)) updatedAdapter.add(temp);
        }
        idTable.setAdapter(updatedAdapter);
        idTable.setSelectionFromTop(index, top);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_main_toolbar, menu);
        // Hide the import item — import is now handled by the FAB
        MenuItem importItem = menu.findItem(R.id.action_import);
        if (importItem != null) importItem.setVisible(false);
    }

    @Override
    final public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            launchSafExport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    final public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK && intent != null) {
            switch (requestCode) {
                case VerifyConstants.PICK_CUSTOM_DEST:
                    if (exportUtility != null) exportUtility.writeToExportPath(intent.getData());
                    break;
                case VerifyConstants.DEFAULT_CONTENT_REQ:
                    Intent i = new Intent(context, LoaderDBActivity.class);
                    i.setData(intent.getData());
                    startActivityForResult(i, VerifyConstants.LOADER_INTENT_REQ);
                    break;
                case VerifyConstants.LOADER_INTENT_REQ:
                    homeViewModel.setmListId(null);
                    homeViewModel.setmPairCol(null);
                    mFileName = "";

                    if (intent.hasExtra(VerifyConstants.FILE_NAME))
                        mFileName = intent.getStringExtra(VerifyConstants.FILE_NAME);
                    if (intent.hasExtra(VerifyConstants.LIST_ID_EXTRA))
                        homeViewModel.setmListId(intent.getStringExtra(VerifyConstants.LIST_ID_EXTRA));
                    if (intent.hasExtra(VerifyConstants.PAIR_COL_EXTRA))
                        homeViewModel.setmPairCol(intent.getStringExtra(VerifyConstants.PAIR_COL_EXTRA));

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    final SharedPreferences.Editor editor = sharedPref.edit();
                    int scanMode = Integer.valueOf(sharedPref.getString(SettingsFragment.SCAN_MODE_LIST, "-1"));
                    String mPairCol = homeViewModel.getmPairCol();

                    if (mPairCol != null) {
                        editor.putBoolean(SettingsFragment.DISABLE_PAIR, false);
                        if (scanMode != 4) showPairDialog();
                    } else {
                        editor.putBoolean(SettingsFragment.DISABLE_PAIR, true);
                    }
                    if (mPairCol == null && scanMode == 4) {
                        editor.putString(SettingsFragment.SCAN_MODE_LIST, "0");
                        Toast.makeText(context, R.string.switching_to_default_mode, Toast.LENGTH_SHORT).show();
                    }
                    editor.putString(SettingsFragment.FILE_NAME, mFileName);
                    editor.putString(SettingsFragment.PAIR_NAME, mPairCol);
                    editor.putString(SettingsFragment.LIST_KEY_NAME, homeViewModel.getmListId());
                    editor.apply();

                    clearListView();
                    homeViewModel.loadSQLToLocal(activity);
                    HashSet<String> ids = homeViewModel.updateCheckedItems();
                    updateTable(ids);
                    refreshData();
                    break;
            }

            if (intent.hasExtra(VerifyConstants.CAMERA_RETURN_ID)) {
                ((EditText) view.findViewById(R.id.scannerTextView))
                        .setText(intent.getStringExtra(VerifyConstants.CAMERA_RETURN_ID));
                checkScannedItem();
            }
        }
    }

    private void buildListView() {
        ListView idTable = view.findViewById(R.id.idTable);

        idAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, android.R.id.text1) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView textView = v.findViewById(android.R.id.text1);
                if (position == selectedIndex) {
                    textView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                } else {
                    textView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
                return v;
            }
        };

        SparseArray<String> mIds = homeViewModel.getmIds();
        int size = mIds.size();
        for (int i = 0; i < size; i++) {
            idAdapter.add(mIds.get(mIds.keyAt(i)));
        }
        idTable.setAdapter(idAdapter);
    }

    private void clearListView() {
        ListView idTable = view.findViewById(R.id.idTable);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.row);
        idTable.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void showPairDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.pair_mode_dialog_title);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPref.edit().putString(SettingsFragment.SCAN_MODE_LIST, "4").apply();
        });
        builder.setNegativeButton(R.string.no_thanks, null);
        builder.show();
    }

    @Override
    final public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    final public void onDestroy() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        homeViewModel.getmDbHelper().close();
        super.onDestroy();
    }

    @Override
    public void ringNotification(boolean success) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean audioEnabled = sharedPref.getBoolean(SettingsFragment.AUDIO_ENABLED, true);
        if (success) {
            if (audioEnabled) playSound("plonk");
        } else {
            if (audioEnabled) playSound("error");
            else Toast.makeText(context, R.string.scanned_id_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void playSound(String soundResourceName) {
        try {
            int resID = getResources().getIdentifier(soundResourceName, "raw", activity.getPackageName());
            MediaPlayer chimePlayer = MediaPlayer.create(context, resID);
            chimePlayer.start();
            chimePlayer.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception e) {
            Log.e("HomeFragment", "Error playing sound", e);
        }
    }

    private void refreshData() {
        homeViewModel.loadSQLToLocal(activity);
        buildListView();
        if (homeViewModel.getmListId() != null) {
            HashSet<String> ids = homeViewModel.updateCheckedItems();
            updateTable(ids);
        }
    }

    @Override
    public void startIntent(Intent i) {
        startActivityForResult(Intent.createChooser(i, getString(R.string.choose_folder_to_export)),
                VerifyConstants.PICK_CUSTOM_DEST, null);
    }
}