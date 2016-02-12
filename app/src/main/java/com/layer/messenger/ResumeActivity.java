package com.layer.messenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;

import java.util.concurrent.atomic.AtomicReference;

public class ResumeActivity extends AppCompatActivity implements LayerAuthenticationListener {
    public static final String EXTRA_LOGGED_IN_ACTIVITY_CLASS_NAME = "loggedInActivity";
    public static final String EXTRA_LOGGED_OUT_ACTIVITY_CLASS_NAME = "loggedOutActivity";

    private AtomicReference<Class<? extends Activity>> mLoggedInActivity = new AtomicReference<Class<? extends Activity>>(null);
    private AtomicReference<Class<? extends Activity>> mLoggedOutActivity = new AtomicReference<Class<? extends Activity>>(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onResume() {
        super.onResume();
        App.getLayerClient().registerAuthenticationListener(this).authenticate();
        try {
            mLoggedInActivity.set((Class<? extends Activity>) Class.forName(getIntent().getStringExtra(EXTRA_LOGGED_IN_ACTIVITY_CLASS_NAME)));
            mLoggedOutActivity.set((Class<? extends Activity>) Class.forName(getIntent().getStringExtra(EXTRA_LOGGED_OUT_ACTIVITY_CLASS_NAME)));
        } catch (ClassNotFoundException e) {
            if (Log.isLoggable(Log.ERROR)) {
                Log.e("Could not find class: " + e.getCause(), e);
            }
        }
    }

    @Override
    protected void onPause() {
        App.getLayerClient().unregisterAuthenticationListener(this);
        super.onPause();
    }

    @Override
    public void onAuthenticated(LayerClient layerClient, String s) {
        startActivity(mLoggedInActivity.get());
    }

    @Override
    public void onDeauthenticated(LayerClient layerClient) {
        startActivity(mLoggedOutActivity.get());
    }

    @Override
    public void onAuthenticationChallenge(LayerClient layerClient, String s) {

    }

    @Override
    public void onAuthenticationError(LayerClient layerClient, LayerException e) {
        startActivity(mLoggedOutActivity.get());
    }

    private void startActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}