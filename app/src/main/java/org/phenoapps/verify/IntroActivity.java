package org.phenoapps.verify;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;


public class IntroActivity extends AppIntro2 {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Customize view
        showStatusBar(false);

        // Add slides
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro_title_1),
                getString(R.string.intro_body_1),
                R.drawable.intro_launcher,
                getResources().getColor(R.color.intro_slide_color_1)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro_title_2),
                getString(R.string.intro_body_2),
                R.drawable.intro_folder,
                getResources().getColor(R.color.intro_slide_color_2)));

        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro_title_3),
                getString(R.string.intro_body_3),
                R.drawable.intro_list,
                getResources().getColor(R.color.intro_slide_color_3)));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
    }
}