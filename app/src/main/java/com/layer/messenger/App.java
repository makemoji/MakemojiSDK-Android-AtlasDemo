package com.layer.messenger;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;

import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageUtils;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.Util;
import com.layer.atlas.util.picasso.requesthandlers.MessagePartRequestHandler;
import com.layer.messenger.flavor.AppAuthenticationProvider;
import com.layer.messenger.flavor.AppParticipantProvider;
import com.layer.sdk.LayerClient;
import com.squareup.picasso.Picasso;

import java.util.Arrays;

public class App extends Application {
    //==============================================================================================
    // Layer App ID Setup
    //==============================================================================================

    // Set your LAYER_APP_ID to bypass the QR-Code scanner or use a different identity provider
    private final static String LAYER_APP_ID = null;
    private final static String GCM_SENDER_ID = "748607264448";


    //==============================================================================================
    // Attributes
    //==============================================================================================

    private static LayerClient sLayerClient;
    private static ParticipantProvider sParticipantProvider;
    private static Picasso sPicasso;
    private static Uri sLayerAppId;
    private static AuthenticationProvider sAuthProvider;
    private static Application sInstance;


    //==============================================================================================
    // Application Overrides
    //==============================================================================================

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // Synchronize Layer, Atlas, and App logging
        com.layer.atlas.util.Log.setAlwaysLoggable(LayerClient.isLoggingEnabled());
        com.layer.messenger.Log.setAlwaysLoggable(LayerClient.isLoggingEnabled());

        // Allow the LayerClient to track app state
        LayerClient.applicationCreated(this);
    }


    //==============================================================================================
    // Identity Provider Methods
    //==============================================================================================

    /**
     * Routes the user to the proper Activity depending on their authenticated state.  Returns
     * `true` if the user has been routed to another Activity, or `false` otherwise.
     *
     * @param from Activity to route from.
     * @return `true` if the user has been routed to another Activity, or `false` otherwise.
     */
    public static boolean routeLogin(Activity from) {
        Uri appId = getLayerAppId();
        return getAuthenticationProvider().routeLogin(getLayerClient(), appId, from);
    }

    /**
     * Authenticates with the AuthenticationProvider and Layer, returning asynchronous results to
     * the provided callback.
     *
     * @param credentials Credentials associated with the current AuthenticationProvider.
     * @param callback    Callback to receive authentication results.
     */
    @SuppressWarnings("unchecked")
    public static void authenticate(Object credentials, AuthenticationProvider.Callback callback) {
        LayerClient client = getLayerClient();
        if (client == null) return;
        Uri layerAppId = getLayerAppId();
        if (layerAppId == null) return;
        getAuthenticationProvider()
                .setCredentials(credentials)
                .setCallback(callback);
        client.authenticate();
    }

    /**
     * Deauthenticates with Layer and clears cached AuthenticationProvider credentials.
     *
     * @param callback Callback to receive deauthentication success and failure.
     */
    public static void deauthenticate(final Util.DeauthenticationCallback callback) {
        Util.deauthenticate(getLayerClient(), new Util.DeauthenticationCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onDeauthenticationSuccess(LayerClient client) {
                getAuthenticationProvider().setCredentials(null);
                callback.onDeauthenticationSuccess(client);
            }

            @Override
            public void onDeauthenticationFailed(LayerClient client, String reason) {
                callback.onDeauthenticationFailed(client, reason);
            }
        });
    }


    //==============================================================================================
    // Getters / Setters
    //==============================================================================================

    /**
     * Gets or creates a LayerClient.  If there is no app ID to create the client with, returns
     * `null`.
     *
     * @return New or existing LayerClient, or `null` if there is no App ID to create a client with.
     */
    public static LayerClient getLayerClient() {
        if (sLayerClient == null) {
            Uri layerAppId = getLayerAppId();
            if (layerAppId == null) return null;

            // Custom options for constructing a LayerClient
            LayerClient.Options options = new LayerClient.Options()
                    /* Set Google Cloud Messaging Sender ID (project number) */
                    .googleCloudMessagingSenderId(GCM_SENDER_ID)

                    /* Fetch the minimum amount per conversation when first authenticated */
                    .historicSyncPolicy(LayerClient.Options.HistoricSyncPolicy.FROM_LAST_MESSAGE)
                    
                    /* Automatically download text and ThreePartImage info/preview */
                    .autoDownloadMimeTypes(Arrays.asList(
                            TextCellFactory.MIME_TYPE,
                            ThreePartImageUtils.MIME_TYPE_INFO,
                            ThreePartImageUtils.MIME_TYPE_PREVIEW));

            // Construct LayerClient with custom options
            sLayerClient = LayerClient.newInstance(sInstance, layerAppId.toString(), options)
                    /* Register AuthenticationProvider for handling authentication challenges */
                    .registerAuthenticationListener(getAuthenticationProvider());
        }
        return sLayerClient;
    }

    public static ParticipantProvider getParticipantProvider() {
        if (sParticipantProvider == null) {
            sParticipantProvider = new AppParticipantProvider(sInstance)
                    .setLayerAppId(getLayerAppId())
                    .setAuthenticationProvider(getAuthenticationProvider());
        }
        return sParticipantProvider;
    }

    public static AuthenticationProvider getAuthenticationProvider() {
        if (sAuthProvider == null) {
            sAuthProvider = new AppAuthenticationProvider(sInstance);
            LayerClient layerClient = getLayerClient();
            if (layerClient != null && sAuthProvider.hasCredentials()) {
                // Use the AuthenticationProvider's cached credentials to authenticate with Layer
                layerClient.authenticate();
            }
        }
        return sAuthProvider;
    }

    public static Picasso getPicasso() {
        if (sPicasso == null) {
            // Picasso with custom RequestHandler for loading from Layer MessageParts.
            sPicasso = new Picasso.Builder(sInstance)
                    .addRequestHandler(new MessagePartRequestHandler(getLayerClient()))
                    .build();
        }
        return sPicasso;
    }

    /**
     * Sets the current Layer App ID, and saves it for use next time (to bypass QR code scanner).
     *
     * @param appId Layer App ID to use when generating a LayerClient.
     */
    public static void setLayerAppId(Uri appId) {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Saving Layer App ID: " + appId);
        sLayerAppId = appId;
        sInstance.getSharedPreferences("layerAppId", MODE_PRIVATE).edit()
                .putString("layerAppId", appId.toString()).commit();
    }

    /**
     * Returns either the constant `LAYER_APP_ID`, or an App ID saved from e.g. the QR code scanner.
     */
    public static Uri getLayerAppId() {
        // In-memory cached App ID?
        if (sLayerAppId != null) {
            return sLayerAppId;
        }

        // Build-time constant?
        if (LAYER_APP_ID != null) {
            if (Log.isLoggable(Log.VERBOSE)) {
                Log.v("Using constant `App.LAYER_APP_ID` App ID: " + LAYER_APP_ID);
            }
            sLayerAppId = Uri.parse(LAYER_APP_ID);
            return sLayerAppId;
        }

        // Saved App ID?
        String saved = sInstance.getSharedPreferences("layerAppId", MODE_PRIVATE).getString("layerAppId", null);
        if (saved == null) return null;
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Loaded Layer App ID: " + saved);
        sLayerAppId = Uri.parse(saved);
        return sLayerAppId;
    }
}
