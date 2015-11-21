package com.layer.messenger.flavor.util;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.layer.messenger.App;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CustomEndpoint provides a mechanism for using endpoints besides the default Layer endpoints.
 * This is only useful for enterprise customers with custom endpoints.  Contact support@layer.com
 * for information.
 *
 * @see com.layer.sdk.LayerClient.Options#customEndpoint(String, String, String, String)
 */
public class CustomEndpoint {
    private static Endpoint sEndpoint;
    private static Map<String, Endpoint> sEndpoints;

    public static String getLayerAppId() {
        Endpoint endpoint = getEndpoint();
        return endpoint == null ? null : endpoint.getAppId();
    }

    public static void setLayerClientOptions(LayerClient.Options options) {
        Endpoint endpoint = getEndpoint();
        if (endpoint != null) endpoint.setLayerClientOptions(options);
    }

    public static boolean hasEndpoints() {
        Map<String, Endpoint> endpoints = getEndpoints();
        return endpoints != null && !endpoints.isEmpty();
    }

    public static Spinner createSpinner(Context context) {
        Set<String> endpointNames = getNames();
        if (endpointNames == null || endpointNames.isEmpty()) return null;

        List<String> namesList = new ArrayList<String>(endpointNames);
        Collections.sort(namesList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, namesList);
        Spinner spinner = new Spinner(context);
        spinner.setAdapter(adapter);

        Endpoint endpoint = getEndpoint();
        if (endpoint != null) {
            int position = namesList.indexOf(endpoint.getName());
            if (position != -1) spinner.setSelection(position);
        }
        setEndpointName((String) spinner.getSelectedItem());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setEndpointName((String) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setEndpointName(null);
            }
        });

        return spinner;
    }

    private static Set<String> getNames() {
        Map<String, Endpoint> endpoints = getEndpoints();
        return endpoints == null ? null : endpoints.keySet();
    }

    private static void setEndpointName(String name) {
        App.getInstance().getSharedPreferences("layer_custom_endpoint", Context.MODE_PRIVATE).edit().putString("name", name).commit();
        Map<String, Endpoint> endpoints = getEndpoints();
        sEndpoint = (endpoints == null) ? null : endpoints.get(name);
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Setting custom endpoint to: " + sEndpoint);
    }

    private static Endpoint getEndpoint() {
        if (sEndpoint != null) return sEndpoint;
        String savedEndpointName = App.getInstance().getSharedPreferences("layer_custom_endpoint", Context.MODE_PRIVATE).getString("name", null);
        if (savedEndpointName == null) return null;
        Map<String, Endpoint> endpoints = getEndpoints();
        sEndpoint = (endpoints == null) ? null : endpoints.get(savedEndpointName);
        return sEndpoint;
    }

    private static Map<String, Endpoint> getEndpoints() {
        if (sEndpoints != null) return sEndpoints;
        sEndpoints = new HashMap<String, Endpoint>();

        // Check for endpoints in resources
        Context context = App.getInstance();
        int resId = context.getResources().getIdentifier("layer_endpoints", "raw", context.getPackageName());
        if (resId == 0) return null;

        // Read endpoints from resources
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        InputStream is = context.getResources().openRawResource(resId);
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception e) {
            if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
                }
            }
        }
        String content = writer.toString().trim();
        if (content.isEmpty()) return null;

        // Parse endpoints from JSON
        try {
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                Endpoint endpoint = new Endpoint(array.getJSONObject(i));
                sEndpoints.put(endpoint.getName(), endpoint);
            }
            return sEndpoints;
        } catch (JSONException e) {
            if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
        }
        return null;
    }

    public static class Endpoint {
        final String mName;
        final String mAppId;
        final String mGcmSenderId;
        final String mProviderUrl;

        final String mPlatformUrl;
        final String mPlatformToken;

        final String mEndpointConf;
        final String mEndpointCert;
        final String mEndpointAuth;
        final String mEndpointSync;

        public Endpoint(JSONObject o) throws JSONException {
            mName = o.getString("name");
            mAppId = o.getString("appId");
            mGcmSenderId = o.getString("gcmSenderId");
            mProviderUrl = o.getString("providerUrl");

            JSONObject platform = o.optJSONObject("platform");
            if (platform != null) {
                mPlatformUrl = platform.optString("url");
                mPlatformToken = platform.optString("token");
            } else {
                mPlatformUrl = null;
                mPlatformToken = null;
            }

            JSONObject endpoint = o.optJSONObject("endpoint");
            if (endpoint != null) {
                mEndpointConf = endpoint.getString("conf");
                mEndpointCert = endpoint.getString("cert");
                mEndpointAuth = endpoint.getString("auth");
                mEndpointSync = endpoint.getString("sync");
            } else {
                mEndpointConf = null;
                mEndpointCert = null;
                mEndpointAuth = null;
                mEndpointSync = null;
            }
        }

        public void setLayerClientOptions(LayerClient.Options options) {
            if (mGcmSenderId != null) options.googleCloudMessagingSenderId(mGcmSenderId);

            if (mEndpointAuth != null) {
                options.customEndpoint(mEndpointConf, mEndpointCert, mEndpointAuth, mEndpointSync);
            }
        }

        public String getName() {
            return mName;
        }

        public String getAppId() {
            return mAppId;
        }

        @Override
        public String toString() {
            return "Endpoint{" +
                    "mName='" + mName + '\'' +
                    ", mAppId='" + mAppId + '\'' +
                    ", mGcmSenderId='" + mGcmSenderId + '\'' +
                    ", mProviderUrl='" + mProviderUrl + '\'' +
                    ", mPlatformUrl='" + mPlatformUrl + '\'' +
                    ", mPlatformToken='" + mPlatformToken + '\'' +
                    ", mEndpointConf='" + mEndpointConf + '\'' +
                    ", mEndpointCert='" + mEndpointCert + '\'' +
                    ", mEndpointAuth='" + mEndpointAuth + '\'' +
                    ", mEndpointSync='" + mEndpointSync + '\'' +
                    '}';
        }
    }
}
