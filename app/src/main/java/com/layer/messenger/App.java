package com.layer.messenger;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageUtils;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.utilities.Utils;
import com.layer.atlas.utilities.picasso.requesthandlers.MessagePartRequestHandler;
import com.layer.messenger.flavor.AppAuthenticationProvider;
import com.layer.messenger.flavor.AppParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.exceptions.LayerObjectException;
import com.layer.sdk.listeners.LayerObjectExceptionListener;
import com.layer.sdk.listeners.LayerSyncListener;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

public class App extends Application {
    private static final String TAG = App.class.getSimpleName();


    //==============================================================================================
    // Layer App ID Setup
    //==============================================================================================
    // Atlas
//    private final static String LAYER_APP_ID = "layer:///apps/staging/93db3886-3b00-11e5-83a8-2d4d00001241";
//    private final static String LAYER_APP_ID = null;
//    private final static String GCM_SENDER_ID = "748607264448";

    // Heroku
    private final static String LAYER_APP_ID = "layer:///apps/staging/9ec30af8-5591-11e4-af9e-f7a201004a3b";
    private final static String GCM_SENDER_ID = "565052870572";


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
    public static void deauthenticate(final Utils.DeauthenticationCallback callback) {
        Utils.deauthenticate(getLayerClient(), new Utils.DeauthenticationCallback() {
            @Override
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

            // When debugging, toast errors.
            if (BuildConfig.DEBUG) {
                sLayerClient.registerObjectExceptionListener(new LayerObjectExceptionListener() {
                    @Override
                    public void onObjectError(LayerClient layerClient, LayerObjectException e) {
                        Log.e(TAG, e.getMessage(), e);
                        Toast.makeText(sInstance, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).registerSyncListener(new LayerSyncListener() {
                    @Override
                    public void onBeforeSync(LayerClient layerClient, SyncType syncType) {

                    }

                    @Override
                    public void onSyncProgress(LayerClient layerClient, SyncType syncType, int i) {

                    }

                    @Override
                    public void onAfterSync(LayerClient layerClient, SyncType syncType) {

                    }

                    @Override
                    public void onSyncError(LayerClient layerClient, List<LayerException> list) {
                        for (LayerException e : list) {
                            Log.e(TAG, e.getMessage(), e);
                            Toast.makeText(sInstance, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

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
            if (sAuthProvider.hasCredentials()) {
                getLayerClient().authenticate();
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
        Log.v(TAG, "Saving Layer App ID: " + appId);
        sLayerAppId = appId;
        sInstance.getSharedPreferences("layerAppId", MODE_PRIVATE)
                .edit()
                .putString("layerAppId", appId.toString())
                .commit();
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
            Log.v(TAG, "Using constant `App.LAYER_APP_ID` App ID: " + LAYER_APP_ID);
            sLayerAppId = Uri.parse(LAYER_APP_ID);
            return sLayerAppId;
        }

        // Saved App ID?
        String saved = sInstance.getSharedPreferences("layerAppId", MODE_PRIVATE).getString("layerAppId", null);
        if (saved == null) return null;
        Log.v(TAG, "Loaded Layer App ID: " + saved);
        sLayerAppId = Uri.parse(saved);
        return sLayerAppId;
    }
}
