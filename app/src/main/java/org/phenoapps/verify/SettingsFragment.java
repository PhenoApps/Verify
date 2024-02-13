package org.phenoapps.verify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.prefs.Preferences;

public class SettingsFragment extends PreferenceFragmentCompat {


    public static final CharSequence INTRO_BUTTON = "org.phenoapps.verify.INTRO";
    public static final CharSequence ABOUT_BUTTON = "org.phenoapps.verify.ABOUT";
    public static String FILE_NAME = "org.phenoapps.verify.FILE_NAME";
    public static String SCAN_MODE_LIST = "org.phenoapps.verify.SCAN_MODE";
    public static String AUDIO_ENABLED = "org.phenoapps.verify.AUDIO_ENABLED";
    public static String TUTORIAL_MODE = "org.phenoapps.verify.TUTORIAL_MODE";
    public static String NAME = "org.phenoapps.verify.NAME";
    public static String LIST_KEY_NAME = "org.phenoapps.verify.LIST_KEY_NAME";
    public static String PAIR_NAME = "org.phenoapps.verify.PAIR_NAME";
    public static String DISABLE_PAIR = "org.phenoapps.verify.DISABLE_PAIR";
    public static String AUX_INFO = "org.phenoapps.verify.AUX_INFO";


    private void showChangeLog() {

    }
    private void showAboutDialog(Context ctx)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent i = new Intent(getContext(), AboutActivity.class);
            startActivity(i);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);


        final SharedPreferences sharedPrefs = super.getPreferenceManager().getSharedPreferences();
        ListPreference mode = findPreference(SCAN_MODE_LIST);
        Preference introButton = findPreference(INTRO_BUTTON);
        Preference aboutButton = findPreference(ABOUT_BUTTON);

        aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    showAboutDialog(getContext());
                }
                return true;
            }
        });

        introButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    final Intent intro_intent = new Intent(getContext(), IntroActivity.class);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(intro_intent);
                        }
                    });
                }
                return true;
            }
        });
        mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            //check if Pair mode is chosen, if it's disabled then show a message and switch
            //back to default mode.
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o.equals("4") &&
                        sharedPrefs.getBoolean(DISABLE_PAIR, false)) {
                    ((ListPreference) preference).setValue("0");
                    Toast.makeText(getActivity(),
                            "Pair mode cannot be used without setting a pair ID.",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }
}
