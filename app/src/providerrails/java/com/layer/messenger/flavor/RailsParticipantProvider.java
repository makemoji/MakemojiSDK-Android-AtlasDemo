package com.layer.messenger.flavor;

import android.content.Context;
import android.os.AsyncTask;

import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.layer.messenger.util.Util.streamToString;

public class RailsParticipantProvider implements ParticipantProvider {
    private final Context mContext;
    private final Queue<ParticipantListener> mParticipantListeners = new ConcurrentLinkedQueue<>();
    private final Map<String, RailsParticipant> mParticipantMap = new HashMap<String, RailsParticipant>();
    private final AtomicBoolean mFetching = new AtomicBoolean(false);

    private RailsAuthenticationProvider mAuthenticationProvider;

    public RailsParticipantProvider(Context context) {
        mContext = context.getApplicationContext();
        load();
        fetchParticipants();
    }

    public RailsParticipantProvider setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        mAuthenticationProvider = (RailsAuthenticationProvider) authenticationProvider;
        fetchParticipants();
        return this;
    }

    //==============================================================================================
    // Atlas ParticipantProvider
    //==============================================================================================

    @Override
    public Map<String, Participant> getMatchingParticipants(String filter, Map<String, Participant> result) {
        if (result == null) {
            result = new HashMap<String, Participant>();
        }

        synchronized (mParticipantMap) {
            // With no filter, return all Participants
            if (filter == null) {
                result.putAll(mParticipantMap);
                return result;
            }

            // Filter participants by substring matching first- and last- names
            filter = filter.toLowerCase();
            for (RailsParticipant p : mParticipantMap.values()) {
                boolean matches = false;
                if (p.getName() != null && p.getName().toLowerCase().contains(filter))
                    matches = true;
                if (matches) {
                    result.put(p.getId(), p);
                } else {
                    result.remove(p.getId());
                }
            }
            return result;
        }
    }

    @Override
    public Participant getParticipant(String userId) {
        synchronized (mParticipantMap) {
            RailsParticipant participant = mParticipantMap.get(userId);
            if (participant != null) return participant;
            fetchParticipants();
            return null;
        }
    }

    /**
     * Adds the provided Participants to this ParticipantProvider, saves the participants, and
     * returns the list of added participant IDs.
     */
    private RailsParticipantProvider setParticipants(Collection<RailsParticipant> participants) {
        List<String> newParticipantIds = new ArrayList<>(participants.size());
        synchronized (mParticipantMap) {
            for (RailsParticipant participant : participants) {
                String participantId = participant.getId();
                if (!mParticipantMap.containsKey(participantId)) {
                    newParticipantIds.add(participantId);
                }
                mParticipantMap.put(participantId, participant);
            }
            save();
        }
        alertParticipantsUpdated(newParticipantIds);
        return this;
    }


    //==============================================================================================
    // Persistence
    //==============================================================================================

    /**
     * Loads additional participants from SharedPreferences
     */
    private boolean load() {
        synchronized (mParticipantMap) {
            String jsonString = mContext.getSharedPreferences("participants", Context.MODE_PRIVATE).getString("json", null);
            if (jsonString == null) return false;

            try {
                for (RailsParticipant participant : participantsFromJson(new JSONArray(jsonString))) {
                    mParticipantMap.put(participant.getId(), participant);
                }
                return true;
            } catch (JSONException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
            }
            return false;
        }
    }

    /**
     * Saves the current map of participants to SharedPreferences
     */
    private boolean save() {
        synchronized (mParticipantMap) {
            try {
                mContext.getSharedPreferences("participants", Context.MODE_PRIVATE).edit()
                        .putString("json", participantsToJson(mParticipantMap.values()).toString())
                        .commit();
                return true;
            } catch (JSONException e) {
                if (Log.isLoggable(Log.ERROR)) {
                    Log.e(e.getMessage(), e);
                }
            }
        }
        return false;
    }


    //==============================================================================================
    // Network operations
    //==============================================================================================
    private RailsParticipantProvider fetchParticipants() {
        if (mAuthenticationProvider == null) return this;
        RailsAuthenticationProvider.Credentials credentials = mAuthenticationProvider.getCredentials();
        if (credentials == null) return this;
        if (credentials.getAuthToken() == null) return this;

        if (!mFetching.compareAndSet(false, true)) return this;
        new AsyncTask<RailsAuthenticationProvider.Credentials, Void, Void>() {
            protected Void doInBackground(RailsAuthenticationProvider.Credentials... params) {
                try {
                    // Post request
                    RailsAuthenticationProvider.Credentials credentials = params[0];
                    String url = "http://layer-identity-provider.herokuapp.com/users.json";
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setDoInput(true);
                    connection.setDoOutput(false);
                    connection.setRequestMethod("GET");
                    connection.addRequestProperty("Content-Type", "application/json");
                    connection.addRequestProperty("Accept", "application/json");
                    connection.addRequestProperty("X_LAYER_APP_ID", credentials.getLayerAppId());
                    if (credentials.getEmail() != null) {
                        connection.addRequestProperty("X_AUTH_EMAIL", credentials.getEmail());
                    }
                    if (credentials.getAuthToken() != null) {
                        connection.addRequestProperty("X_AUTH_TOKEN", credentials.getAuthToken());
                    }

                    // Handle failure
                    int statusCode = connection.getResponseCode();
                    if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                        if (Log.isLoggable(Log.ERROR)) {
                            Log.e(String.format("Got status %d when fetching participants", statusCode));
                        }
                        return null;
                    }

                    // Parse response
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    String result = streamToString(in);
                    in.close();
                    connection.disconnect();
                    JSONArray json = new JSONArray(result);
                    setParticipants(participantsFromJson(json));
                } catch (Exception e) {
                    if (Log.isLoggable(Log.ERROR)) Log.e(e.getMessage(), e);
                } finally {
                    mFetching.set(false);
                }
                return null;
            }
        }.execute(credentials);
        return this;
    }


    //==============================================================================================
    // Utils
    //==============================================================================================

    private static List<RailsParticipant> participantsFromJson(JSONArray participantArray) throws JSONException {
        List<RailsParticipant> participants = new ArrayList<>(participantArray.length());
        for (int i = 0; i < participantArray.length(); i++) {
            JSONObject participantObject = participantArray.getJSONObject(i);
            RailsParticipant participant = new RailsParticipant();
            participant.setId(participantObject.optString("id", null));
            participant.setFirstName(trimmedValue(participantObject, "first_name", null));
            participant.setLastName(trimmedValue(participantObject, "last_name", null));
            participant.setEmail(trimmedValue(participantObject, "email", null));
            participant.setAvatarUrl(null);
            participants.add(participant);
        }
        return participants;
    }

    private static String trimmedValue(JSONObject o, String name, String fallback) {
        String s = o.optString(name, fallback);
        return (s == null) ? null : s.trim();
    }

    private static JSONArray participantsToJson(Collection<RailsParticipant> participants) throws JSONException {
        JSONArray participantsArray = new JSONArray();
        for (RailsParticipant participant : participants) {
            JSONObject participantObject = new JSONObject();
            participantObject.put("id", participant.getId());
            participantObject.put("first_name", participant.getFirstName());
            participantObject.put("last_name", participant.getLastName());
            participantObject.put("email", participant.getEmail());
            participantsArray.put(participantObject);
        }
        return participantsArray;
    }

    private RailsParticipantProvider registerParticipantListener(ParticipantListener participantListener) {
        if (!mParticipantListeners.contains(participantListener)) {
            mParticipantListeners.add(participantListener);
        }
        return this;
    }

    private RailsParticipantProvider unregisterParticipantListener(ParticipantListener participantListener) {
        mParticipantListeners.remove(participantListener);
        return this;
    }

    private void alertParticipantsUpdated(Collection<String> updatedParticipantIds) {
        for (ParticipantListener listener : mParticipantListeners) {
            listener.onParticipantsUpdated(this, updatedParticipantIds);
        }
    }


    //==============================================================================================
    // Callbacks
    //==============================================================================================

    public interface ParticipantListener {
        void onParticipantsUpdated(RailsParticipantProvider provider, Collection<String> updatedParticipantIds);
    }
}