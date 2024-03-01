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
import android.os.Environment;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_toolbar);;
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
    }


    @Override
    final public void onPause() {
        super.onPause();
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    public static void scanFile(Context ctx, File filePath) {
        MediaScannerConnection.scanFile(ctx, new String[] { filePath.getAbsolutePath()}, null, null);
    }

    private void copyRawToVerify(File verifyDirectory, String fileName, int rawId) {

        String fieldSampleName = verifyDirectory.getAbsolutePath() + "/" + fileName;
        File fieldSampleFile = new File(fieldSampleName);
        if (!Arrays.asList(verifyDirectory.listFiles()).contains(fieldSampleFile)) {
            try {
                InputStream inputStream = getResources().openRawResource(rawId);
                FileOutputStream foStream =  new FileOutputStream(fieldSampleName);
                byte[] buff = new byte[1024];
                int read = 0;
                try {
                    while ((read = inputStream.read(buff)) > 0) {
                        foStream.write(buff, 0, read);
                    }
                    scanFile(this, fieldSampleFile);
                } finally {
                    inputStream.close();
                    foStream.close();
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    static private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }


    @Override
    public void onRequestPermissionsResult(int resultCode, String[] permissions, int[] granted) {
        super.onRequestPermissionsResult(resultCode, permissions, granted);
        boolean externalWriteAccept = false;
        if (resultCode == VerifyConstants.PERM_REQ) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals("android.permission.WRITE_EXTERNAL_STORAGE")) {
                    externalWriteAccept = true;
                }
            }
        }
        if (externalWriteAccept && isExternalStorageWritable()) {

            File verifyDirectory = new File(getExternalFilesDir(null), "/Verify");

            if (!verifyDirectory.isDirectory()) {
                final boolean makeDirsSuccess = verifyDirectory.mkdirs();
                if (!makeDirsSuccess) Log.d("Verify Make Directory", "failed");
            }
            copyRawToVerify(verifyDirectory, "field_sample.csv", R.raw.field_sample);
            copyRawToVerify(verifyDirectory, "verify_pair_sample.csv", R.raw.verify_pair_sample);
        }
    }
}
