package com.layer.messenger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.layer.atlas.utilities.Utils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.Queryable;
import com.layer.sdk.query.SortDescriptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PushNotificationReceiver extends BroadcastReceiver {
    private final static String TAG = PushNotificationReceiver.class.getSimpleName();
    public final static int MESSAGE_ID = 1;
    private final static AtomicInteger sPendingIntentCounter = new AtomicInteger(0);
    private static Notifications sNotifications;

    public final static String ACTION_PUSH = "com.layer.sdk.PUSH";
    public final static String ACTION_CANCEL = "com.layer.messenger.CANCEL_PUSH";

    public final static String LAYER_TEXT_KEY = "layer-push-message";
    public final static String LAYER_CONVERSATION_KEY = "layer-conversation-id";
    public final static String LAYER_MESSAGE_KEY = "layer-message-id";

    /**
     * Parses the `com.layer.sdk.PUSH` Intent.
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        final String text = extras.getString(LAYER_TEXT_KEY, null);
        final Uri conversationId = extras.getParcelable(LAYER_CONVERSATION_KEY);
        final Uri messageId = extras.getParcelable(LAYER_MESSAGE_KEY);

        if (intent.getAction().equals(ACTION_PUSH)) {
            // New push from Layer
            Log.v(TAG, "Received notification for: " + messageId);
            if (messageId == null) {
                Log.e(TAG, "No message to notify");
                return;
            }

            if (!getNotifications(context).isEnabled()) {
                Log.v(TAG, "Blocking notification due to global app setting");
                return;
            }

            if (!getNotifications(context).isEnabled(conversationId)) {
                Log.v(TAG, "Blocking notification due to conversation detail setting");
                return;
            }

            // Try to have content ready for viewing before posting a Notification
            Utils.waitForContent(App.getLayerClient().connect(), messageId,
                    new Utils.ContentAvailableCallback() {
                        @Override
                        public void onContentAvailable(LayerClient client, Queryable object) {
                            Log.v(TAG, "Pre-fetched notification content");
                            getNotifications(context).add(context, (Message) object, text);
                        }

                        @Override
                        public void onContentFailed(LayerClient client, Uri objectId, String reason) {
                            Log.e(TAG, "Failed to fetch notification content");
                        }
                    }
            );
        } else if (intent.getAction().equals(ACTION_CANCEL)) {
            // User swiped notification out
            Log.v(TAG, "Cancelling notifications for: " + conversationId);
            getNotifications(context).clear(conversationId);
        } else {
            Log.e(TAG, "Got unknown intent action: " + intent.getAction());
        }
    }

    public static synchronized Notifications getNotifications(Context context) {
        if (sNotifications == null) {
            sNotifications = new Notifications(context);
        }
        return sNotifications;
    }

    /**
     * Notifications manages notifications displayed on the user's device.  Notifications are
     * grouped by Conversation, where a Conversation's notifications are rolled-up into single
     * notification summaries.
     */
    public static class Notifications {
        private final int MAX_MESSAGES = 5;
        // Contains black-listed conversation IDs and the global "all" key for notifications
        private final SharedPreferences mDisableds;

        // Contains positions for message IDs
        private final SharedPreferences mPositions;
        private final SharedPreferences mMessages;
        private final NotificationManager mManager;

        public Notifications(Context context) {
            mDisableds = context.getSharedPreferences("notification_disableds", Context.MODE_PRIVATE);
            mPositions = context.getSharedPreferences("notification_positions", Context.MODE_PRIVATE);
            mMessages = context.getSharedPreferences("notification_messages", Context.MODE_PRIVATE);
            mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        public boolean isEnabled() {
            return !mDisableds.contains("all");
        }

        public boolean isEnabled(Uri conversationId) {
            return !mDisableds.contains(conversationId.toString());
        }

        public void setEnabled(boolean enabled) {
            if (enabled) {
                mDisableds.edit().remove("all").apply();
            } else {
                mDisableds.edit().putBoolean("all", true).apply();
                mManager.cancelAll();
            }
        }

        public void setEnabled(Uri conversationId, boolean enabled) {
            if (enabled) {
                mDisableds.edit().remove(conversationId.toString()).apply();
            } else {
                mDisableds.edit().putBoolean(conversationId.toString(), true).apply();
                mManager.cancel(conversationId.toString(), MESSAGE_ID);
            }
        }

        public void clear(final Conversation conversation) {
            if (conversation == null) return;
            clear(conversation.getId());
        }

        /**
         * Called when a Conversation is opened or message is marked as read
         * Clears messages map; sets position to greatest position
         *
         * @param conversationId Conversation whose notifications should be cleared
         */
        public void clear(final Uri conversationId) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (conversationId == null) return;
                    String key = conversationId.toString();
                    long maxPosition = getMaxPosition(conversationId);
                    mMessages.edit().remove(key).commit();
                    mPositions.edit().putLong(key, maxPosition).commit();
                    mManager.cancel(key, MESSAGE_ID);
                }
            }).start();
        }

        /**
         * Called when a new message arrives
         *
         * @param message Message to add
         * @param text    Notification text for added Message
         */
        protected void add(Context context, Message message, String text) {
            Conversation conversation = message.getConversation();
            String key = conversation.getId().toString();
            long currentPosition = mPositions.getLong(key, Long.MIN_VALUE);

            // Ignore older messages
            if (message.getPosition() <= currentPosition) return;

            String currentMessages = mMessages.getString(key, null);

            try {
                JSONObject messages = currentMessages == null ? new JSONObject() : new JSONObject(currentMessages);
                String messageKey = message.getId().toString();

                // Ignore if we already have this message
                if (messages.has(messageKey)) return;

                JSONObject messageEntry = new JSONObject();
                messageEntry.put("position", message.getPosition());
                messageEntry.put("text", text);
                messages.put(messageKey, messageEntry);

                mMessages.edit().putString(key, messages.toString()).commit();
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                return;
            }
            update(context, conversation, message);
        }

        private void update(Context context, Conversation conversation, Message message) {
            String messagesString = mMessages.getString(conversation.getId().toString(), null);
            if (messagesString == null) return;

            // Get current notification texts
            Map<Long, String> positionText = new HashMap<Long, String>();
            try {
                JSONObject messagesJson = new JSONObject(messagesString);
                Iterator<String> iterator = messagesJson.keys();
                while (iterator.hasNext()) {
                    String messageId = iterator.next();
                    JSONObject messageJson = messagesJson.getJSONObject(messageId);
                    long position = messageJson.getLong("position");
                    String text = messageJson.getString("text");
                    positionText.put(position, text);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                return;
            }

            // Sort by message position
            List<Long> positions = new ArrayList<Long>(positionText.keySet());
            Collections.sort(positions);

            // Construct notification
            String conversationTitle = Utils.getConversationTitle(App.getLayerClient(), App.getParticipantProvider(), conversation);
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle().setBigContentTitle(conversationTitle);
            int i;
            if (positions.size() <= MAX_MESSAGES) {
                i = 0;
                inboxStyle.setSummaryText(null);
            } else {
                i = positions.size() - MAX_MESSAGES;
                inboxStyle.setSummaryText("+" + i + " more");
            }
            while (i < positions.size()) {
                inboxStyle.addLine(positionText.get(positions.get(i++)));
            }

            String collapsedSummary = positions.size() == 1 ? positionText.get(positions.get(0)) :
                    positions.size() + " new messages";

            // Construct notification
            // TODO: use large icon based on avatars
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(conversationTitle)
                    .setContentText(collapsedSummary)
                    .setAutoCancel(true)
                    .setLights(context.getResources().getColor(R.color.atlas_action_bar_background), 100, 1900)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                    .setStyle(inboxStyle);

            // Intent to launch when clicked
            Intent clickIntent = new Intent(context, MessagesListActivity.class)
                    .setPackage(context.getApplicationContext().getPackageName())
                    .putExtra(LAYER_CONVERSATION_KEY, conversation.getId())
                    .putExtra(LAYER_MESSAGE_KEY, message.getId())
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent clickPendingIntent = PendingIntent.getActivity(
                    context, sPendingIntentCounter.getAndIncrement(),
                    clickIntent, PendingIntent.FLAG_ONE_SHOT);
            mBuilder.setContentIntent(clickPendingIntent);

            // Intent to launch when swiped out
            Intent cancelIntent = new Intent(ACTION_CANCEL)
                    .setPackage(context.getApplicationContext().getPackageName())
                    .putExtra(LAYER_CONVERSATION_KEY, conversation.getId())
                    .putExtra(LAYER_MESSAGE_KEY, message.getId());
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                    context, sPendingIntentCounter.getAndIncrement(),
                    cancelIntent, PendingIntent.FLAG_ONE_SHOT);
            mBuilder.setDeleteIntent(cancelPendingIntent);

            // Show the notification
            mManager.notify(conversation.getId().toString(), MESSAGE_ID, mBuilder.build());
        }

        /**
         * Returns the current maximum Message position within the given Conversation, or
         * Long.MIN_VALUE if no messages are found.
         *
         * @param conversationId Conversation whose maximum Message position to return.
         * @return the current maximum Message position or Long.MIN_VALUE.
         */
        private long getMaxPosition(Uri conversationId) {
            LayerClient layerClient = App.getLayerClient();

            Query q = Query.builder(Message.class)
                    .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversationId))
                    .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.DESCENDING))
                    .limit(1)
                    .build();

            List<Message> messages = layerClient.executeQueryForObjects(q);
            if (messages.isEmpty()) return Long.MIN_VALUE;
            return messages.get(0).getPosition();
        }
    }
}
