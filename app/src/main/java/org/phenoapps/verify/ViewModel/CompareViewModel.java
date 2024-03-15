package org.phenoapps.verify.ViewModel;
import android.util.Log;

import androidx.lifecycle.ViewModel;

public class CompareViewModel extends ViewModel {
    // Define your data here
    private Mode mMode = Mode.Matches;

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        Log.d("CompareViewModel", "Setting mode to: $mode");
        this.mMode = mode;
    }

    // Enum for Mode
    public enum Mode {
        Contains,
        Matches
    }
}