package com.layer.sample.flavor;

import android.content.Context;

import com.layer.sample.App;
import com.layer.sample.ParticipantProvider;
import com.layer.sample.R;
import com.layer.sample.flavor.util.CustomEndpoint;
import com.layer.sample.util.AuthenticationProvider;
import com.layer.sample.util.Log;
import com.layer.sdk.LayerClient;

public class Flavor implements App.Flavor {
    // Set your Layer App ID from your Layer Developer Dashboard.
    public final static String LAYER_APP_ID = null;

    // Set your Google Cloud Messaging Sender ID from your Google Developers Console. 
    private final static String GCM_SENDER_ID = null;

    @Override
    public String getLayerAppId() {
        return (LAYER_APP_ID != null) ? LAYER_APP_ID : CustomEndpoint.getLayerAppId();
    }

    @Override
    public LayerClient generateLayerClient(Context context, LayerClient.Options options) {
        String layerAppId = getLayerAppId();
        if (layerAppId == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e(context.getString(R.string.app_id_required));
            return null;
        }
        if (GCM_SENDER_ID != null) options.googleCloudMessagingSenderId(GCM_SENDER_ID);
        CustomEndpoint.setLayerClientOptions(options);
        return LayerClient.newInstance(context, layerAppId, options);
    }

    @Override
    public AuthenticationProvider generateAuthenticationProvider(Context context) {
        return new RailsAuthenticationProvider(context);
    }

    @Override
    public ParticipantProvider generateParticipantProvider(Context context, AuthenticationProvider authenticationProvider) {
        return new RailsParticipantProvider(context).setAuthenticationProvider(authenticationProvider);
    }
}
