package org.phenoapps.verify.ViewModel;
import androidx.lifecycle.ViewModel;

public class CompareViewModel extends ViewModel {
    // Define your data here
    private Mode mMode = Mode.Matches;

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        this.mMode = mode;
    }

    // Enum for Mode
    public enum Mode {
        Contains,
        Matches
    }
}
