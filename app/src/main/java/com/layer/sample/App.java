package com.layer.sample;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.layer.sample.util.AuthenticationProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerAuthenticationListener;

/**
 * App provides static access to a LayerClient. It also provides an AuthenticationProvider and
 * ParticipantProvider to use with the LayerClient.
 *
 * App.Flavor allows build variants to target different environments, such as the standard Demo and the
 * open source Rails Identity Provider.  Switch flavors with the Android Studio `Build Variant` tab.
 * When using a flavor besides the Demo you must manually set your Layer App ID and GCM Sender
 * ID in that flavor's Flavor.java.
 *
 * @see com.layer.sample.App.Flavor
 * @see com.layer.sample.flavor.Flavor
 * @see LayerClient
 * @see ParticipantProvider
 * @see AuthenticationProvider
 */
public class App extends Application {

    private static Application sInstance;
    private static Flavor sFlavor = new com.layer.sample.flavor.Flavor();

    private static LayerClient sLayerClient;
    private static ParticipantProvider sParticipantProvider;
    private static AuthenticationProvider sAuthProvider;


    //==============================================================================================
    // Application Overrides
    //==============================================================================================

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose logging in debug builds
        if (BuildConfig.DEBUG) {
            com.layer.sample.util.Log.setAlwaysLoggable(true);
            LayerClient.setLoggingEnabled(this, true);

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }

        // Allow the LayerClient to track app state
        LayerClient.applicationCreated(this);

        sInstance = this;
    }

    public static Application getInstance() {
        return sInstance;
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
        return getAuthenticationProvider().routeLogin(getLayerClient(), getLayerAppId(), from);
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
        String layerAppId = getLayerAppId();
        if (layerAppId == null) return;
        getAuthenticationProvider()
                .setCredentials(credentials)
                .setCallback(callback);
        client.authenticate();
    }

    public static void deauthenticate(LayerAuthenticationListener deauthenticationListener) {
        LayerClient client = getLayerClient();
        if (client != null) {
            client.registerAuthenticationListener(deauthenticationListener);
            client.deauthenticate();
        }
    }

    //==============================================================================================
    // Getters / Setters
    //==============================================================================================

    /**
     * Gets or creates a LayerClient, using a default set of LayerClient.Options and flavor-specific
     * App ID and Options from the `generateLayerClient` method.  Returns `null` if the flavor was
     * unable to create a LayerClient (due to no App ID, etc.).
     *
     * @return New or existing LayerClient, or `null` if a LayerClient could not be constructed.
     * @see Flavor#generateLayerClient(Context, LayerClient.Options)
     */
    public static LayerClient getLayerClient() {
        if (sLayerClient == null) {
            // Custom options for constructing a LayerClient
            LayerClient.Options options = new LayerClient.Options()
                    /* Fetch the minimum amount per conversation when first authenticated */
                    .historicSyncPolicy(LayerClient.Options.HistoricSyncPolicy.FROM_LAST_MESSAGE);

            // Allow flavor to specify Layer App ID and customize Options.
            sLayerClient = sFlavor.generateLayerClient(sInstance, options);

            // Flavor was unable to generate Layer Client (no App ID, etc.)
            if (sLayerClient == null) return null;

            /* Register AuthenticationProvider for handling authentication challenges */
            sLayerClient.registerAuthenticationListener(getAuthenticationProvider());
        }
        return sLayerClient;
    }

    public static String getLayerAppId() {
        return sFlavor.getLayerAppId();
    }

    public static ParticipantProvider getParticipantProvider() {
        if (sParticipantProvider == null) {
            sParticipantProvider = sFlavor.generateParticipantProvider(sInstance, getAuthenticationProvider());
        }
        return sParticipantProvider;
    }

    public static AuthenticationProvider getAuthenticationProvider() {
        if (sAuthProvider == null) {
            sAuthProvider = sFlavor.generateAuthenticationProvider(sInstance);

            // If we have cached credentials, try authenticating with Layer
            LayerClient layerClient = getLayerClient();
            if (layerClient != null && sAuthProvider.hasCredentials()) layerClient.authenticate();
        }
        return sAuthProvider;
    }

    /**
     * Flavor is used to switch environments.
     *
     * @see com.layer.sample.flavor.Flavor
     */
    public interface Flavor {
        String getLayerAppId();

        LayerClient generateLayerClient(Context context, LayerClient.Options options);

        AuthenticationProvider generateAuthenticationProvider(Context context);

        ParticipantProvider generateParticipantProvider(Context context, AuthenticationProvider authenticationProvider);
    }
}
