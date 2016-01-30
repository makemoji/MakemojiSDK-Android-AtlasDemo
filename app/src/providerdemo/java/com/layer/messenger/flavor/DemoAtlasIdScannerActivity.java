package com.layer.messenger.flavor;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.common.GoogleApiAvailability;
import com.layer.messenger.R;
import com.layer.messenger.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class DemoAtlasIdScannerActivity extends AppCompatActivity {
    private static final String PERMISSION = Manifest.permission.CAMERA;
    public static final int PERMISSION_REQUEST_CODE = 21;

    AppIdScanner mAppIdScanner;
    private final AtomicBoolean mFoundAppId = new AtomicBoolean(false);

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_app_id_scanner);
        setTitle(R.string.title_app_id_scanner);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermission()) {
            startScanner();
        } else {
            requestPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (hasPermission()) getAppIdScanner().stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hasPermission()) getAppIdScanner().release();
    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Dynamically add AppIdScanner to layout because dynamic permissions seem to break when added
     * ahead of time (onRequestPermissionsResult is never called).
     */
    private AppIdScanner getAppIdScanner() {
        if (mAppIdScanner == null) {
            AppIdScanner scanner = new AppIdScanner(this);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            scanner.setAppIdCallback(new AppIdScanner.AppIdCallback() {
                @Override
                public void onLayerAppIdScanned(AppIdScanner scanner, String layerAppId) {
                    if (!mFoundAppId.compareAndSet(false, true)) return;
                    if (Log.isLoggable(Log.VERBOSE)) {
                        Log.v("Found App ID: " + layerAppId);
                    }
                    Flavor.setLayerAppId(layerAppId);
                    Intent intent = new Intent(DemoAtlasIdScannerActivity.this, DemoLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (!isFinishing()) finish();
                }
            });
            ((FrameLayout) findViewById(R.id.app_id_scanner_layout)).addView(scanner, 0, layoutParams);
            mAppIdScanner = scanner;
        }
        return mAppIdScanner;
    }

    private void requestPermission() {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Requesting camera permission.");
        ActivityCompat.requestPermissions(this, new String[]{PERMISSION}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Got permission result for: " + requestCode);
        if (grantResults.length == 0 || requestCode != PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Camera permission granted.");
            startScanner();
        } else {
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Camera permission denied.");
        }
    }

    private void startScanner() {
        // Check for Google Play
        Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this), 1);
        if (errorDialog != null) {
            errorDialog.show();
        } else {
            getAppIdScanner().start();
        }
    }
}
