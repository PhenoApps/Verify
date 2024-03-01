package org.phenoapps.verify.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.phenoapps.verify.SettingsFragment;

public class RingUtility {

    private final Context context;
    private final String packageName;
    private final Resources resources;
    private final View activityView;

    public RingUtility(Context context, View view, String packageName, Resources resources){
        this.context = context;
        this.packageName = packageName;
        this.resources = resources;
        this.activityView = view;
    }

    public void ringNotification(boolean success) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean audioEnabled = sharedPref.getBoolean(SettingsFragment.AUDIO_ENABLED, true);

        if(success) { //ID found
            if(audioEnabled) {
                    try {
                        int resID = resources.getIdentifier("plonk", "raw", packageName);
                        MediaPlayer chimePlayer = MediaPlayer.create(context, resID);
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

        if(!success) { //ID not found
            ((TextView) activityView.findViewById(org.phenoapps.verify.R.id.valueView)).setText("");

            if (audioEnabled) {
                if(!success) {
                    try {
                        int resID = resources.getIdentifier("error", "raw", packageName);
                        MediaPlayer chimePlayer = MediaPlayer.create(context, resID);
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
                    Toast.makeText(context, "Scanned ID not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
