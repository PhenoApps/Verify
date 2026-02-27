package org.phenoapps.verify.utilities;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;


import androidx.appcompat.app.AlertDialog;

import org.phenoapps.verify.IdEntryContract;
import org.phenoapps.verify.IdEntryDbHelper;
import org.phenoapps.verify.R;
import org.phenoapps.verify.VerifyConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FileExport {

    private final Activity activity;
    final static private String line_separator = System.getProperty("line.separator");
    private String fileName;

    final private IdEntryDbHelper dbHelper;
    public FileExport(Activity activity, String fileName, IdEntryDbHelper dbHelper){
        this.fileName = fileName;
        this.dbHelper = dbHelper;
        this.activity = activity;
    }


    public synchronized void askUserExportFileName(IntentHelper helper) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.export_choose_filename_title);
        final EditText input = new EditText(activity);

        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            fileName = fileName.substring(0, lastDot);
        }
        input.setText("Verify_"+ sdf.format(c.getTime()));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.export_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                String value = input.getText().toString();
                fileName = value;
                final Intent i;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_TITLE, value+".csv");
                    helper.startIntent(i);
                }else{
                    writeToExportPath();
                }
            }
        });
        builder.show();
    }

    public void writeToExportPath() {
        String value = fileName;

        if (!value.isEmpty()) {
            if (isExternalStorageWritable()) {
                try {
                    File verifyDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/Verify");
                    final File output = new File(verifyDirectory, value + ".csv");
                    final FileOutputStream fstream = new FileOutputStream(output);
                    final SQLiteDatabase db = dbHelper.getReadableDatabase();
                    final String table = IdEntryContract.IdEntry.TABLE_NAME;
                    final Cursor cursor = db.query(table, null, null, null, null, null, null);

                    if(cursor.getCount() == 0) {
                        Log.e("FileExport", "No data to export");
                        Toast.makeText(activity, R.string.export_no_data, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // first write header line
                    final String[] headers = cursor.getColumnNames();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != 0) fstream.write(",".getBytes());
                        fstream.write(headers[i].getBytes());
                    }
                    fstream.write(line_separator.getBytes());

                    // populate text file with current database values
                    while (cursor.moveToNext()) {
                        for (int i = 0; i < headers.length; i++) {
                            if (i != 0) fstream.write(",".getBytes());
                            final String val = cursor.getString(
                                    cursor.getColumnIndexOrThrow(headers[i])
                            );
                            if (val == null) fstream.write("null".getBytes());
                            else fstream.write(val.getBytes());
                        }
                        fstream.write(line_separator.getBytes());
                    }

                    cursor.close();
                    fstream.flush();
                    fstream.close();
                    scanFile(activity, output);
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, R.string.export_error_table_empty, Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, R.string.export_error_creating_file, Toast.LENGTH_SHORT).show();
                } catch (IOException io) {
                    io.printStackTrace();
                    Toast.makeText(activity, R.string.export_error_writing_file, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, R.string.export_storage_not_writable, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, R.string.export_must_enter_filename, Toast.LENGTH_SHORT).show();
        }
    }


    public static void scanFile(Context ctx, File filePath) {
        MediaScannerConnection.scanFile(ctx, new String[] { filePath.getAbsolutePath()}, null, null);
    }

    public void writeToExportPath(Uri uri) {
        String value = fileName;

        if (uri == null) {
            Toast.makeText(activity, R.string.export_unable_to_open_file, Toast.LENGTH_LONG).show();
            return;
        }

        if (!value.isEmpty()) {
            if (isExternalStorageWritable()) {
                try {
                    final OutputStream fstream = activity.getContentResolver().openOutputStream(uri);
                    final SQLiteDatabase db = dbHelper.getReadableDatabase();
                    final String table = IdEntryContract.IdEntry.TABLE_NAME;
                    final Cursor cursor = db.query(table, null, null, null, null, null, null);

                    if(cursor.getCount() == 0) {
                        Log.e("FileExport", "No data to export");
                        Toast.makeText(activity, R.string.export_no_data, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // first write header line
                    final String[] headers = cursor.getColumnNames();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != 0) fstream.write(",".getBytes());
                        fstream.write(headers[i].getBytes());
                    }
                    fstream.write(line_separator.getBytes());

                    // populate text file with current database values
                    while (cursor.moveToNext()) {
                        for (int i = 0; i < headers.length; i++) {
                            if (i != 0) fstream.write(",".getBytes());
                            final String val = cursor.getString(
                                    cursor.getColumnIndexOrThrow(headers[i])
                            );
                            if (val == null) fstream.write("null".getBytes());
                            else fstream.write(val.getBytes());
                        }
                        fstream.write(line_separator.getBytes());
                    }

                    cursor.close();
                    fstream.flush();
                    fstream.close();
                    scanFile(activity, new File(uri.getPath()));
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, R.string.export_error_table_empty, Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(activity, R.string.export_error_creating_file, Toast.LENGTH_SHORT).show();
                } catch (IOException io) {
                    io.printStackTrace();
                    Toast.makeText(activity, R.string.export_error_writing_file, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, R.string.export_storage_not_writable, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, R.string.export_must_enter_filename, Toast.LENGTH_SHORT).show();
        }
    }


    static private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}
