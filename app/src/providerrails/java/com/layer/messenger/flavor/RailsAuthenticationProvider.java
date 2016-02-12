package com.layer.messenger.flavor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.layer.messenger.R;
import com.layer.messenger.ResumeActivity;
import com.layer.messenger.flavor.util.CustomEndpoint;
import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.layer.messenger.util.Util.streamToString;

public class RailsAuthenticationProvider implements AuthenticationProvider<RailsAuthenticationProvider.Credentials> {
    private static final String TAG = RailsAuthenticationProvider.class.getSimpleName();

    private final SharedPreferences mPreferences;
    private Callback mCallback;

    public RailsAuthenticationProvider(Context context) {
        mPreferences = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }

    @Override
    public AuthenticationProvider<Credentials> setCredentials(Credentials credentials) {
        replaceCredentials(credentials);
        return this;
    }

    @Override
    public boolean hasCredentials() {
        return getCredentials() != null;
    }

    @Override
    public AuthenticationProvider<Credentials> setCallback(Callback callback) {
        mCallback = callback;
        return this;
    }

    @Override
    public void onAuthenticated(LayerClient layerClient, String userId) {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Authenticated with Layer, user ID: " + userId);
        layerClient.connect();
        if (mCallback != null) mCallback.onSuccess(this, userId);
    }

    @Override
    public void onDeauthenticated(LayerClient layerClient) {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Deauthenticated with Layer");
    }

    @Override
    public void onAuthenticationChallenge(LayerClient layerClient, String nonce) {
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Received challenge: " + nonce);
        respondToChallenge(layerClient, nonce);
    }

    @Override
    public void onAuthenticationError(LayerClient layerClient, LayerException e) {
        String error = "Failed to authenticate with Layer: " + e.getMessage();
        if (Log.isLoggable(Log.ERROR)) Log.e(error, e);
        if (mCallback != null) mCallback.onError(this, error);
    }

    @Override
    public boolean routeLogin(LayerClient layerClient, String layerAppId, Activity from) {
        if ((layerClient != null) && layerClient.isAuthenticated()) {
            // The LayerClient is authenticated: no action required.
            if (Log.isLoggable(Log.VERBOSE)) Log.v("No authentication routing required");
            return false;
        }

        if (layerAppId == null && !CustomEndpoint.hasEndpoints()) {
            // With no Layer App ID (and no CustomEndpoint) we can't authenticate: bail out.
            if (Log.isLoggable(Log.ERROR)) Log.v("No Layer App ID set");
            Toast.makeText(from, R.string.app_id_required, Toast.LENGTH_LONG).show();
            return true;
        }

        if ((layerClient != null) && hasCredentials()) {
            // With a LayerClient and cached provider credentials, we can resume.
            if (Log.isLoggable(Log.VERBOSE)) {
                Log.v("Routing to resume Activity using cached credentials");
            }
            Intent intent = new Intent(from, ResumeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ResumeActivity.EXTRA_LOGGED_IN_ACTIVITY_CLASS_NAME, from.getClass().getName());
            intent.putExtra(ResumeActivity.EXTRA_LOGGED_OUT_ACTIVITY_CLASS_NAME, RailsLoginActivity.class.getName());
            from.startActivity(intent);
            return true;
        }

        // We have a Layer App ID but no cached provider credentials: routing to Login required.
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Routing to login Activity");
        Intent intent = new Intent(from, RailsLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        from.startActivity(intent);
        return true;
    }

    private void replaceCredentials(Credentials credentials) {
        if (credentials == null) {
            mPreferences.edit().clear().commit();
            return;
        }
        mPreferences.edit()
                .putString("appId", credentials.getLayerAppId())
                .putString("email", credentials.getEmail())
                .putString("password", credentials.getPassword())
                .putString("authToken", credentials.getAuthToken())
                .commit();
    }

    protected Credentials getCredentials() {
        if (!mPreferences.contains("appId")) return null;
        return new Credentials(
                mPreferences.getString("appId", null),
                mPreferences.getString("email", null),
                mPreferences.getString("password", null),
                mPreferences.getString("authToken", null));
    }

    private void respondToChallenge(LayerClient layerClient, String nonce) {
        Credentials credentials = getCredentials();
        if (credentials == null || credentials.getEmail() == null || (credentials.getPassword() == null && credentials.getAuthToken() == null) || credentials.getLayerAppId() == null) {
            if (Log.isLoggable(Log.WARN)) {
                Log.w("No stored credentials to respond to challenge with");
            }
            return;
        }

        try {
            // Post request
            String url = "http://layer-identity-provider.herokuapp.com/users/sign_in.json";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X_LAYER_APP_ID", credentials.getLayerAppId());
            if (credentials.getEmail() != null) {
                connection.setRequestProperty("X_AUTH_EMAIL", credentials.getEmail());
            }
            if (credentials.getAuthToken() != null) {
                connection.setRequestProperty("X_AUTH_TOKEN", credentials.getAuthToken());
            }

            // Credentials
            JSONObject rootObject = new JSONObject();
            JSONObject userObject = new JSONObject();
            rootObject.put("user", userObject);
            userObject.put("email", credentials.getEmail());
            userObject.put("password", credentials.getPassword());
            rootObject.put("nonce", nonce);

            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            OutputStream os = connection.getOutputStream();
            os.write(rootObject.toString().getBytes("UTF-8"));
            os.close();

            // Handle failure
            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                String error = String.format("Got status %d when requesting authentication for '%s' with nonce '%s' from '%s'",
                        statusCode, credentials.getEmail(), nonce, url);
                if (Log.isLoggable(Log.ERROR)) Log.e(error);
                if (mCallback != null) mCallback.onError(this, error);
                return;
            }

            // Parse response
            InputStream in = new BufferedInputStream(connection.getInputStream());
            String result = streamToString(in);
            in.close();
            connection.disconnect();
            JSONObject json = new JSONObject(result);
            if (json.has("error")) {
                String error = json.getString("error");
                if (Log.isLoggable(Log.ERROR)) Log.e(error);
                if (mCallback != null) mCallback.onError(this, error);
                return;
            }

            // Save provider's auth token and remove plain-text password.
            String authToken = json.optString("authentication_token", null);
            Credentials authedCredentials = new Credentials(credentials.getLayerAppId(), credentials.getEmail(), null, authToken);
            replaceCredentials(authedCredentials);

            // Answer authentication challenge.
            String identityToken = json.optString("layer_identity_token", null);
            if (Log.isLoggable(Log.VERBOSE)) Log.v("Got identity token: " + identityToken);
            layerClient.answerAuthenticationChallenge(identityToken);
        } catch (Exception e) {
            String error = "Error when authenticating with provider: " + e.getMessage();
            if (Log.isLoggable(Log.ERROR)) Log.e(error, e);
            if (mCallback != null) mCallback.onError(this, error);
        }
    }

    public static class Credentials {
        private final String mLayerAppId;
        private final String mEmail;
        private final String mPassword;
        private final String mAuthToken;

        public Credentials(String layerAppId, String email, String password, String authToken) {
            mLayerAppId = layerAppId == null ? null : (layerAppId.contains("/") ? layerAppId.substring(layerAppId.lastIndexOf("/") + 1) : layerAppId);
            mEmail = email;
            mPassword = password;
            mAuthToken = authToken;
        }

        public String getEmail() {
            return mEmail;
        }

        public String getPassword() {
            return mPassword;
        }

        public String getAuthToken() {
            return mAuthToken;
        }

        public String getLayerAppId() {
            return mLayerAppId;
        }
    }
}

