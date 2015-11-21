package com.layer.messenger.flavor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.layer.messenger.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AppIdScanner extends ViewGroup {
    private SurfaceView mSurfaceView;
    private BarcodeDetector mBarcodeDetector;
    private Detector.Processor<Barcode> mAppIdProcessor;
    private CameraSource.Builder mCameraBuilder;

    private AppIdCallback mAppIdCallback;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;

    public AppIdScanner(Context context) {
        super(context);
        init();
    }

    public AppIdScanner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppIdScanner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mStartRequested = false;
        mSurfaceAvailable = false;

        mAppIdProcessor = new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                for (int i = 0; i < barcodes.size(); i++) {
                    Barcode barcode = barcodes.valueAt(i);
                    String value = barcode.displayValue;
                    try {
                        Uri appId = Uri.parse(value);
                        if (!appId.getScheme().equals("layer")) {
                            throw new IllegalArgumentException("URI is not an App ID");
                        }
                        if (!appId.getAuthority().equals("")) {
                            throw new IllegalArgumentException("URI is not an App ID");
                        }
                        List<String> segments = appId.getPathSegments();
                        if (segments.size() != 3) {
                            throw new IllegalArgumentException("URI is not an App ID");
                        }
                        if (!segments.get(0).equals("apps")) {
                            throw new IllegalArgumentException("URI is not an App ID");
                        }
                        if (!segments.get(1).equals("staging") && !segments.get(1).equals("production")) {
                            throw new IllegalArgumentException("URI is not an App ID");
                        }
                        UUID uuid = UUID.fromString(segments.get(2));
                        if (Log.isLoggable(Log.VERBOSE)) {
                            Log.v("Captured Layer App ID: " + appId + ", UUID: " + uuid);
                        }
                        if (mAppIdCallback == null) return;
                        mAppIdCallback.onLayerAppIdScanned(AppIdScanner.this, appId.toString());
                    } catch (Exception e) {
                        // Not this barcode...                        
                        if (Log.isLoggable(Log.ERROR)) {
                            Log.e("Barcode does not contain an App ID URI: " + value, e);
                        }
                    }
                }
            }
        };

        mBarcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        mBarcodeDetector.setProcessor(mAppIdProcessor);

        mCameraBuilder = new CameraSource.Builder(getContext(), mBarcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30.0f);

        mSurfaceView = new SurfaceView(getContext());
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);
    }

    public AppIdScanner setAppIdCallback(AppIdCallback appIdCallback) {
        mAppIdCallback = appIdCallback;
        return this;
    }

    public void start() {
        mStartRequested = true;
        startIfReady();
    }

    public void stop() {
        if (mCameraSource != null) mCameraSource.stop();
        mSurfaceView.setVisibility(GONE);
    }

    public void release() {
        if (mCameraSource != null) mCameraSource.release();
        mBarcodeDetector.release();
        mAppIdProcessor.release();
    }

    private void startIfReady() {
        if (!mStartRequested || !mSurfaceAvailable || mCameraSource == null) return;
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e("Required permission `" + android.Manifest.permission.CAMERA + "` not granted.");
            }
            return;
        }
        try {
            mCameraSource.start(mSurfaceView.getHolder());
            mStartRequested = false;
        } catch (IOException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e(e.getMessage(), e);
            }
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            startIfReady();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

    @SuppressWarnings("DrawAllocation")
    @Override
    protected void onLayout(boolean isChange, int left, int top, int right, int bottom) {
        if (!isChange) return;
        boolean isPortrait = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int parentWidth = right - left;
        int parentHeight = bottom - top;
        mSurfaceView.layout(0, 0, 1, 1);

        int requestWidth = isPortrait ? parentHeight : parentWidth;
        int requestHeight = isPortrait ? parentWidth : parentHeight;

        // Request camera preview
        if (mCameraSource != null) {
            mCameraSource.stop();
            mCameraSource.release();
        }
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Requesting camera preview: " + requestWidth + "x" + requestHeight);
        }
        mCameraSource = mCameraBuilder.setRequestedPreviewSize(requestWidth, requestHeight).build();
        startIfReady();

        post(new Runnable() {
            @Override
            public void run() {
                double parentWidth = getWidth();
                double parentHeight = getHeight();

                Size previewSize = mCameraSource.getPreviewSize();
                while (previewSize == null) {
                    previewSize = mCameraSource.getPreviewSize();
                    try {
                        TimeUnit.MILLISECONDS.sleep(15);
                    } catch (InterruptedException e) {
                        // OK
                    }
                }
                if (Log.isLoggable(Log.VERBOSE)) {
                    Log.v("Actual camera preview is: " + previewSize.getWidth() + "x" + previewSize.getHeight());
                }

                boolean isPortrait = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                double previewWidth = isPortrait ? previewSize.getHeight() : previewSize.getWidth();
                double previewHeight = isPortrait ? previewSize.getWidth() : previewSize.getHeight();

                double widthRatio = previewWidth / parentWidth;
                double heightRatio = previewHeight / parentHeight;
                double surfaceWidth;
                double surfaceHeight;
                if (heightRatio < widthRatio) {
                    surfaceWidth = parentHeight * previewWidth / previewHeight;
                    surfaceHeight = parentHeight;
                } else {
                    surfaceWidth = parentWidth;
                    surfaceHeight = parentWidth * previewHeight / previewWidth;
                }

                double centerLeft = (parentWidth - surfaceWidth) / 2.0;
                double centerTop = (parentHeight - surfaceHeight) / 2.0;
                mSurfaceView.layout((int) Math.round(centerLeft), (int) Math.round(centerTop), (int) Math.round(surfaceWidth + centerLeft), (int) Math.round(surfaceHeight + centerTop));
                if (Log.isLoggable(Log.VERBOSE)) {
                    Log.v("Resized preview layout to: " + (isPortrait ? mSurfaceView.getHeight() : mSurfaceView.getWidth()) + "x" + (isPortrait ? mSurfaceView.getWidth() : mSurfaceView.getHeight()));
                }
            }
        });
    }

    public interface AppIdCallback {
        void onLayerAppIdScanned(AppIdScanner scanner, String layerAppId);
    }
}
