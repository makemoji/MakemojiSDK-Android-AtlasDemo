package com.layer.sample.util;

import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.util.List;

public class MessageUtils {

    public static String getMessageText(Message message) {
        String messageText = null;
        List<MessagePart> messageParts = message.getMessageParts();
        if (messageParts != null && !messageParts.isEmpty()) {
            MessagePart messagePart = messageParts.get(0);
            String mimeType = messagePart.getMimeType();
            if (mimeType.equals("text/plain")) {
                messageText = new String(messagePart.getData());
            } else {
                messageText = getMimeTypesAndSizes(messageParts);
            }
        }

        return messageText;
    }

    private static String getMimeTypesAndSizes(List<MessagePart> messageParts) {

        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (MessagePart part : messageParts) {
            if (i != 0) builder.append("\n");
            builder.append("[").append(i).append("]: ")
                    .append(part.getSize()).append("-byte `")
                    .append(part.getMimeType()).append("`");
            i++;
        }
        return builder.toString();
    }
}
