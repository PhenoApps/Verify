package org.phenoapps.verify;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.danielstone.materialaboutlibrary.MaterialAboutFragment;
import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;

public class AboutFragment extends MaterialAboutFragment {

    @Override
    @NonNull
    public MaterialAboutList getMaterialAboutList(@NonNull Context c) {

        // ── App card ─────────────────────────────────────────────────────────
        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();

        appCardBuilder.addItem(new MaterialAboutTitleItem.Builder()
                .text(R.string.app_name)
                .icon(R.mipmap.ic_launcher)
                .build());

        appCardBuilder.addItem(ConvenienceBuilder.createVersionActionItem(c,
                ContextCompat.getDrawable(c, R.drawable.ic_about),
                getString(R.string.about_version_title),
                false));
        appCardBuilder.addItem(ConvenienceBuilder.createRateActionItem(c,
                getResources().getDrawable(R.drawable.star),
                getString(R.string.about_rate),
                null
        ));

        // ── Project Lead card ─────────────────────────────────────────────────
        MaterialAboutCard.Builder leadCardBuilder = new MaterialAboutCard.Builder();
        leadCardBuilder.title(getString(R.string.about_project_lead_title));

        leadCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_trevor))
                .subText(getString(R.string.clemson))
                .icon(R.drawable.ic_person_profile)
                .build());

        leadCardBuilder.addItem(ConvenienceBuilder.createEmailItem(c,
                ContextCompat.getDrawable(c, R.drawable.email),
                getString(R.string.about_email_title),
                true,
                getString(R.string.dev_trevor_email),
                "Verify Question"));

        // ── Contributors card ─────────────────────────────────────────────────
        MaterialAboutCard.Builder contributorsCardBuilder = new MaterialAboutCard.Builder();
        contributorsCardBuilder.title(getString(R.string.about_contributors_developers_title));

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(c, R.drawable.account_group),
                getString(R.string.about_contributors_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Verify#contributors")));

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(c, R.drawable.currency_usd),
                getString(R.string.about_contributors_funding_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Verify#funding")));

        // ── PhenoApps card ────────────────────────────────────────────────────
        MaterialAboutCard.Builder otherAppsCardBuilder = new MaterialAboutCard.Builder();
        otherAppsCardBuilder.title(getString(R.string.about_title_pheno_apps));

        otherAppsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(c, R.drawable.web),
                "PhenoApps.org",
                false,
                Uri.parse("http://phenoapps.org/")));

        otherAppsCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Field Book")
                .icon(R.drawable.other_ic_field_book)
                .setOnClickAction(openAppOrStore("com.fieldbook.tracker", c))
                .build());

        otherAppsCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Intercross")
                .icon(R.drawable.other_ic_intercross)
                .setOnClickAction(openAppOrStore("org.phenoapps.intercross", c))
                .build());

        otherAppsCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Coordinate")
                .icon(R.drawable.other_ic_coordinate)
                .setOnClickAction(openAppOrStore("org.wheatgenetics.coordinate", c))
                .build());

        // ── Technical card ────────────────────────────────────────────────────
        MaterialAboutCard.Builder technicalCardBuilder = new MaterialAboutCard.Builder();
        technicalCardBuilder.title(getString(R.string.about_technical_title));

        technicalCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.about_github_title)
                .icon(R.drawable.github)
                .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(c,
                        Uri.parse("https://github.com/PhenoApps/Verify")))
                .build());

        return new MaterialAboutList(
                appCardBuilder.build(),
                leadCardBuilder.build(),
                contributorsCardBuilder.build(),
                technicalCardBuilder.build(),
                otherAppsCardBuilder.build());
    }

    private MaterialAboutItemOnClickAction openAppOrStore(final String packageName, Context c) {
        PackageManager pm = c.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
            return () -> {
                Intent launchIntent = c.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) startActivity(launchIntent);
            };
        } catch (PackageManager.NameNotFoundException e) {
            return ConvenienceBuilder.createWebsiteOnClickAction(c,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
        }
    }
}
