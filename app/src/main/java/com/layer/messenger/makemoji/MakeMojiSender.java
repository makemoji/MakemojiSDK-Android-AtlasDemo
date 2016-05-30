package com.layer.messenger.makemoji;

import com.layer.atlas.messagetypes.MessageSender;
import com.layer.atlas.messagetypes.text.TextSender;
import com.layer.atlas.util.Log;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.messaging.PushNotificationPayload;


public class MakeMojiSender extends TextSender {
    private final int mMaxNotificationLength;

    public MakeMojiSender() {
        this(20000);
    }

    public MakeMojiSender(int maxNotificationLength) {
        mMaxNotificationLength = maxNotificationLength;
    }

    public boolean requestSend(String text) {
        if (text == null || text.trim().length() == 0) {
            if (Log.isLoggable(Log.ERROR)) Log.e("No text to send");
            return false;
        }
        if (Log.isLoggable(Log.VERBOSE)) Log.v("Sending text message");

        // Create notification string
        String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
        String notificationString = getContext().getString(com.layer.atlas.R.string.atlas_notification_text, myName, (text.length() < mMaxNotificationLength) ? text : (text.substring(0, mMaxNotificationLength) + "…"));

        // Send message
        MessagePart part = getLayerClient().newMessagePart(text);
        PushNotificationPayload payload = new PushNotificationPayload.Builder()
                .text(notificationString)
                .build();
        Message message = getLayerClient().newMessage(new MessageOptions().defaultPushNotificationPayload(payload), part);
        return send(message);
    }
}