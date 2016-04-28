package com.layer.sample.conversationlist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.layer.sample.Participant;
import com.layer.sample.ParticipantProvider;
import com.layer.sample.R;
import com.layer.sdk.messaging.Conversation;

import java.util.List;

public class ConversationViewHolder extends RecyclerView.ViewHolder {


    private TextView nameTextView;
    private TextView messageTextView;
    private TextView lastMessageTimeTextView;
    private View itemView;

    public ConversationViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        nameTextView = (TextView) itemView.findViewById(R.id.name);
        messageTextView = (TextView) itemView.findViewById(R.id.message);
        lastMessageTimeTextView = (TextView) itemView.findViewById(R.id.last_message_time);
    }

    public void setName(String name) {
        nameTextView.setText(name);
    }

    public void setMessage(String message) {
        messageTextView.setText(message);
    }

    public void setLastMessageTime(String time) {
        lastMessageTimeTextView.setText(time);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        itemView.setOnClickListener(listener);
    }
}
