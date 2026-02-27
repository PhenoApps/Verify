package org.phenoapps.verify;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Bundle;

import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.core.content.ContextCompat;

public class AboutActivity extends MaterialAboutActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply bottom/side insets for edge-to-edge (MaterialAboutActivity handles the toolbar top inset)
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars() |
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(rootView);
    }

    @NonNull
    @Override
    protected MaterialAboutList getMaterialAboutList(@NonNull Context context) {

        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();

        appCardBuilder.addItem(new MaterialAboutTitleItem.Builder()
                .text(getString(R.string.app_name))
                .icon(R.mipmap.ic_launcher)
                .build());

        appCardBuilder.addItem(ConvenienceBuilder.createVersionActionItem(this,
                ContextCompat.getDrawable(this, R.drawable.ic_about),
                "Version",
                false));

        appCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(context,
                ContextCompat.getDrawable(this, R.drawable.ic_about),
                "GitHub",
                false,
                Uri.parse("https://github.com/PhenoApps/Verify")));

        MaterialAboutCard.Builder authorCardBuilder = new MaterialAboutCard.Builder();
        authorCardBuilder.title("Developers");

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_chaney))
                .subText(getString(R.string.ksu))
                .icon(R.drawable.ic_person_profile)
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_trevor))
                .subText(getString(R.string.ksu) + "\n" + getString(R.string.dev_trevor_email))
                .icon(R.drawable.ic_person_profile)
                .build());

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_jesse))
                .subText(getString(R.string.ksu) + "\n" + getString(R.string.dev_jesse_email))
                .icon(R.drawable.ic_person_profile)
                .build());

        MaterialAboutCard.Builder descriptionCard = new MaterialAboutCard.Builder();
        descriptionCard.title("Description");
        descriptionCard.addItem(new MaterialAboutActionItem.Builder()
                .text("Verify imports a list of entries, scans barcodes, and identifies whether " +
                        "each entry exists in the list with audio/visual notifications.")
                .build());

        MaterialAboutCard.Builder otherAppsCard = new MaterialAboutCard.Builder();
        otherAppsCard.title("PhenoApps");
        otherAppsCard.addItem(ConvenienceBuilder.createWebsiteActionItem(context,
                ContextCompat.getDrawable(this, R.drawable.ic_about),
                "PhenoApps.org",
                false,
                Uri.parse("http://phenoapps.org/")));

        return new MaterialAboutList(
                appCardBuilder.build(),
                authorCardBuilder.build(),
                descriptionCard.build(),
                otherAppsCard.build());
    }

    @Nullable
    @Override
    protected CharSequence getActivityTitle() {
        return getString(R.string.about);
    }
}

