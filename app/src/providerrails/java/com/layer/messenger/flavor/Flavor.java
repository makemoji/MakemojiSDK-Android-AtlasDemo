package com.layer.messenger.flavor;

import android.content.Context;

import com.layer.atlas.provider.ParticipantProvider;
import com.layer.messenger.App;
import com.layer.messenger.R;
import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;

public class Flavor implements App.Flavor {
    // Set your Layer App ID from your Layer Developer Dashboard.
    private final static String LAYER_APP_ID = null;

    // Set your Google Cloud Messaging Sender ID from your Google Developers Console. 
    private final static String LAYER_GCM_SENDER_ID = null;

    @Override
    public String getLayerAppId() {
        return LAYER_APP_ID;
    }

    @Override
    public LayerClient generateLayerClient(Context context, LayerClient.Options options) {
        if (LAYER_APP_ID == null) {
            if (Log.isLoggable(Log.ERROR)) Log.e(context.getString(R.string.app_id_required));
            return null;
        }

        if (LAYER_GCM_SENDER_ID != null) options.googleCloudMessagingSenderId(LAYER_GCM_SENDER_ID);
        return LayerClient.newInstance(context, LAYER_APP_ID, options);
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
