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

public interface RingUtility {

    public void ringNotification(boolean success);

}
