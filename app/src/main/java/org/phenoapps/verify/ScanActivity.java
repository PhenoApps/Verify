package org.phenoapps.verify;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import org.phenoapps.verify.utilities.InsetHandler;

import com.google.zxing.ResultPoint;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class ScanActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeScannerView;
    private String lastText;

    private BarcodeCallback callback = new BarcodeCallback() {

        @Override
        public void barcodeResult(BarcodeResult result) {

            if (result.getText() == null || result.getText().equals(lastText)) return;

            lastText = result.getText();
            barcodeScannerView.setStatusText(result.getText());

            ImageView imageView = (ImageView) findViewById(org.phenoapps.verify.R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.GREEN));

            final Intent i = new Intent();
            i.putExtra(VerifyConstants.CAMERA_RETURN_ID, result.getText());

            setResult(RESULT_OK, i);
            finish();

        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        setContentView(org.phenoapps.verify.R.layout.activity_capture);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View rootView = findViewById(android.R.id.content);
        InsetHandler.INSTANCE.setupStandardInsets(rootView, toolbar);

        barcodeScannerView = (DecoratedBarcodeView)
                findViewById(org.phenoapps.verify.R.id.zxing_barcode_scanner);
        barcodeScannerView.getBarcodeView().getCameraSettings().setContinuousFocusEnabled(true);
        barcodeScannerView.getBarcodeView().getCameraSettings().setBarcodeSceneModeEnabled(true);
        barcodeScannerView.decodeContinuous(callback);

        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(null);
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeScannerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeScannerView.pause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}