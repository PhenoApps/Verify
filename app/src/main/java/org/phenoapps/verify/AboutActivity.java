package org.phenoapps.verify;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.phenoapps.verify.utilities.InsetHandler;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.about));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        BottomNavigationView bnv = findViewById(R.id.about_bnv);
        // No item pre-selected in About
        bnv.getMenu().setGroupCheckable(0, false, true);

        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        InsetHandler.INSTANCE.setupInsetsWithBottomNav(rootView, toolbar, bnv);

        bnv.setOnItemSelectedListener(item -> {
            finish();
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


