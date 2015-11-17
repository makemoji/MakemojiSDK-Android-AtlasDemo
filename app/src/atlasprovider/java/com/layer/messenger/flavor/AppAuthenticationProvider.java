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
        if (credentials == null) {
            mPreferences.edit().clear().commit();
            return;
        }
        mPreferences.edit()
                .putString("appId", credentials.getLayerAppId())
                .putString("name", credentials.getUserName())
                .commit();
        return this;
    }

    @Override
    public boolean hasCredentials() {
        return mPreferences.contains("appId");
    }

    @Override
    public AuthenticationProvider<Credentials> setCallback(Callback callback) {
        mCallback = callback;
        return this;
    }

    private void privateAuthenticate(final String nonce) {
        Credentials credentials = new Credentials(mPreferences.getString("appId", null), mPreferences.getString("name", null));
        if (credentials.getUserName() == null || credentials.getLayerAppId() == null) {
            Log.d(TAG, "No stored credentials to respond to challenge with");
            return;
        }

        try {
            // Post request
            String url = "https://layer-identity-provider.herokuapp.com/apps/" + credentials.getLayerAppId() + "/atlas_identities";
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");
            post.setHeader("X_LAYER_APP_ID", credentials.getLayerAppId());
            StringEntity entity = new StringEntity(new JSONObject().put("nonce", nonce).put("name", credentials.getUserName()).toString(), "UTF-8");
            entity.setContentType("application/json");
            post.setEntity(entity);
            HttpResponse response = new DefaultHttpClient().execute(post);

            // Handle failure
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
                String error = String.format("Got status %d when requesting authentication for '%s' with nonce '%s' from '%s'",
                        statusCode, credentials.getUserName(), nonce, url);
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
            String identityToken = json.getString("identity_token");
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
        if (mCallback != null) {
            mCallback.onSuccess(this, userId);
        }
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
        if (mCallback != null) {
            mCallback.onError(this, error);
        }
    }

    @Override
    public boolean routeLogin(LayerClient layerClient, Uri layerAppId, Activity from) {
        if (layerAppId == null) {
            // No App ID: must scan from QR code.
            Intent intent = new Intent(from, AppAtlasIdScannerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            from.startActivity(intent);
            return true;
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
        private final String mUserName;

        public Credentials(Uri layerAppId, String userName) {
            this(layerAppId == null ? null : layerAppId.getLastPathSegment(), userName);
        }

        public Credentials(String layerAppId, String userName) {
            mLayerAppId = layerAppId == null ? null : (layerAppId.contains("/") ? layerAppId.substring(layerAppId.lastIndexOf("/") + 1) : layerAppId);
            mUserName = userName;
        }

        public String getUserName() {
            return mUserName;
        }

        public String getLayerAppId() {
            return mLayerAppId;
        }
    }
}

