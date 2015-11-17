package com.layer.messenger.flavor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.layer.messenger.App;
import com.layer.messenger.AuthenticationProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class AppAuthenticationProvider implements AuthenticationProvider<AppAuthenticationProvider.Credentials> {
    private static final String TAG = AppAuthenticationProvider.class.getSimpleName();

    private final SharedPreferences mPreferences;
    private Callback mCallback;

    public AppAuthenticationProvider(Context context) {
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

    private void privateAuthenticate(final String nonce) {
        Credentials credentials = getCredentials();
        if (credentials == null || credentials.getEmail() == null || (credentials.getPassword() == null && credentials.getAuthToken() == null) || credentials.getLayerAppId() == null) {
            Log.d(TAG, "No stored credentials to respond to challenge with");
            return;
        }

        try {
            // Post request
            String url = "http://layer-identity-provider.herokuapp.com/users/sign_in.json";
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");
            post.setHeader("X_LAYER_APP_ID", credentials.getLayerAppId());
            if (credentials.getEmail() != null) {
                post.setHeader("X_AUTH_EMAIL", credentials.getEmail());
            }
            if (credentials.getAuthToken() != null) {
                post.setHeader("X_AUTH_TOKEN", credentials.getAuthToken());
            }

            // Credentials
            JSONObject rootObject = new JSONObject();
            JSONObject userObject = new JSONObject();
            rootObject.put("user", userObject);
            userObject.put("email", credentials.getEmail());
            userObject.put("password", credentials.getPassword());
            rootObject.put("nonce", nonce);
            StringEntity entity = new StringEntity(rootObject.toString(), "UTF-8");
            entity.setContentType("application/json");
            post.setEntity(entity);

            HttpResponse response = new DefaultHttpClient().execute(post);

            // Handle failure
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
                String error = String.format("Got status %d when requesting authentication for '%s' with nonce '%s' from '%s'",
                        statusCode, credentials.getEmail(), nonce, url);
                Log.e(TAG, error);
                if (mCallback != null) mCallback.onError(this, error);
                return;
            }

            // Parse response
            JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
            if (json.has("error")) {
                String error = json.getString("error");
                Log.e(TAG, error);
                if (mCallback != null) mCallback.onError(this, error);
                return;
            }

            // Save provider's auth token and remove plain-text password.
            String authToken = json.optString("authentication_token", null);
            Credentials authedCredentials = new Credentials(credentials.getLayerAppId(), credentials.getEmail(), null, authToken);
            replaceCredentials(authedCredentials);

            // Answer authentication challenge.
            String identityToken = json.optString("layer_identity_token", null);
            Log.d(TAG, "Got identity token: " + identityToken);
            App.getLayerClient().answerAuthenticationChallenge(identityToken);
        } catch (Exception e) {
            String error = "Error when authenticating with provider: " + e.getMessage();
            Log.e(TAG, error, e);
            if (mCallback != null) mCallback.onError(this, error);
        }
    }

    @Override
    public void onAuthenticated(LayerClient layerClient, String userId) {
        Log.d(TAG, "Authenticated with Layer, user ID: " + userId);
        layerClient.connect();
        if (mCallback != null) mCallback.onSuccess(this, userId);
    }

    @Override
    public void onDeauthenticated(LayerClient layerClient) {
        Log.d(TAG, "Deauthenticated with Layer");
    }

    @Override
    public void onAuthenticationChallenge(final LayerClient layerClient, String nonce) {
        Log.d(TAG, "Received challenge: " + nonce);
        privateAuthenticate(nonce);
    }

    @Override
    public void onAuthenticationError(LayerClient layerClient, LayerException e) {
        String error = "Failed to authenticate with Layer: " + e.getMessage();
        Log.e(TAG, error, e);
        if (mCallback != null) mCallback.onError(this, error);
    }

    @Override
    public boolean routeLogin(LayerClient layerClient, Uri layerAppId, Activity from) {
        if (layerAppId == null) {
            throw new IllegalArgumentException("You must set `App.LAYER_APP_ID` to your app's App ID in the Layer Developer Dashboard when using the herokuprovider.");
        }
        if (layerClient != null && !layerClient.isAuthenticated()) {
            if (hasCredentials()) {
                // Use the cached AuthenticationProvider credentials to authenticate with Layer.
                layerClient.authenticate();
            } else {
                // App ID, but no user: must authenticate.
                Intent intent = new Intent(from, AppLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                from.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    public static class Credentials {
        private final String mLayerAppId;
        private final String mEmail;
        private final String mPassword;
        private final String mAuthToken;

        public Credentials(Uri layerAppId, String email, String password, String authToken) {
            this(layerAppId == null ? null : layerAppId.getLastPathSegment(), email, password, authToken);
        }

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

