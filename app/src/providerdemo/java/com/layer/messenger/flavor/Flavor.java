package com.layer.messenger.flavor;

import android.content.Context;

import com.layer.atlas.provider.ParticipantProvider;
import com.layer.messenger.App;
import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;

public class Flavor implements App.Flavor {
    // Set your Layer App ID from your Layer developer dashboard to bypass the QR-Code scanner.
    private final static String LAYER_APP_ID = null;
    private final static String GCM_SENDER_ID = "748607264448";

    private String mLayerAppId;


    //==============================================================================================
    // Layer App ID (from LAYER_APP_ID constant or set by QR-Code scanning AppIdScanner Activity
    //==============================================================================================

    @Override
    public String getLayerAppId() {
        // In-memory cached App ID?
        if (mLayerAppId != null) {
            return mLayerAppId;
        }

        // Constant App ID?
        if (LAYER_APP_ID != null) {
            if (Log.isLoggable(Log.VERBOSE)) {
                Log.v("Using constant `App.LAYER_APP_ID` App ID: " + LAYER_APP_ID);
            }
            mLayerAppId = LAYER_APP_ID;
            return mLayerAppId;
        }

        // Saved App ID?
        String saved = App.getInstance()
                .getSharedPreferences("layerAppId", Context.MODE_PRIVATE)
                .getString("layerAppId", null);
        if (saved == null) return null;
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Loaded Layer App ID: " + saved);
        mLayerAppId = saved;
        return mLayerAppId;
    }

    /**
     * Sets the current Layer App ID, and saves it for use next time (to bypass QR code scanner).
     *
     * @param appId Layer App ID to use when generating a LayerClient.
     */
    protected static void setLayerAppId(String appId) {
        appId = appId.trim();
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Saving Layer App ID: " + appId);
        App.getInstance().getSharedPreferences("layerAppId", Context.MODE_PRIVATE).edit()
                .putString("layerAppId", appId).commit();
    }


    //==============================================================================================
    // Generators
    //==============================================================================================

    @Override
    public LayerClient generateLayerClient(Context context, LayerClient.Options options) {
        // If no App ID is set yet, return `null`; we'll launch the AppIdScanner to get one.
        String appId = getLayerAppId();
        if (appId == null) return null;

        options.googleCloudMessagingSenderId(GCM_SENDER_ID);
        return LayerClient.newInstance(context, appId, options);
    }

    @Override
    public ParticipantProvider generateParticipantProvider(Context context, AuthenticationProvider authenticationProvider) {
        return new DemoParticipantProvider(context).setLayerAppId(getLayerAppId());
    }

    @Override
    public AuthenticationProvider generateAuthenticationProvider(Context context) {
        return new DemoAuthenticationProvider(context);
    }
}