package com.layer.messenger.flavor;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.GoogleApiAvailability;
import com.layer.messenger.App;
import com.layer.messenger.Log;
import com.layer.messenger.R;

import java.util.concurrent.atomic.AtomicBoolean;

public class AppAtlasIdScannerActivity extends AppCompatActivity {
    AppIdScanner mAppIdScanner;
    private final AtomicBoolean mFoundAppId = new AtomicBoolean(false);

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_app_id_scanner);
        setTitle(R.string.title_app_id_scanner);

        // Check for Google Play
        Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this), 1);
        if (errorDialog != null) errorDialog.show();

        mAppIdScanner = ((AppIdScanner) findViewById(R.id.app_id_scanner))
                .setAppIdCallback(new AppIdScanner.AppIdCallback() {
                    @Override
                    public void onLayerAppIdScanned(AppIdScanner scanner, Uri layerAppId) {
                        if (!mFoundAppId.compareAndSet(false, true)) return;
                        if (Log.isLoggable(Log.VERBOSE)) {
                            Log.v("Found app: " + layerAppId);
                        }
                        App.setLayerAppId(layerAppId);
                        Intent intent = new Intent(AppAtlasIdScannerActivity.this, AppLoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    protected void onResume() {
        super.onResume();
        mAppIdScanner.start();
    }

    protected void onPause() {
        super.onPause();
        mAppIdScanner.stop();
    }

    protected void onDestroy() {
        super.onDestroy();
        mAppIdScanner.release();
    }

}
