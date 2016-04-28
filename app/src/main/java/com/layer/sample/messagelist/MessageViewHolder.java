package com.layer.sample.messagelist;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.sample.R;

public class MessageViewHolder extends RecyclerView.ViewHolder {

    private TextView participantNameTextView;
    private TextView messageTextView;
    private TextView dateSentTextView;
    private View itemView;

    public MessageViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        participantNameTextView = (TextView) itemView.findViewById(R.id.name);
        messageTextView = (TextView) itemView.findViewById(R.id.message);
        dateSentTextView = (TextView) itemView.findViewById(R.id.date_sent);
    }

    public void setParticipantName(String participantName) {
        if (TextUtils.isEmpty(participantName)) {
            participantNameTextView.setVisibility(View.GONE);
        } else {
            participantNameTextView.setVisibility(View.VISIBLE);
            participantNameTextView.setText(participantName);
        }
    }

    public void setMessage(String message) {
        messageTextView.setText(message);
    }

    public void setDateSent(String dateSent) {
        dateSentTextView.setText(dateSent);
    }

    public void setIsUsersMessage(boolean isUsersMessage) {
        int gravity;
        if (isUsersMessage) {
            gravity = Gravity.END;
            messageTextView.setBackgroundResource(R.drawable.message_item_cell_me);
            messageTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
        } else {
            gravity = Gravity.START;
            messageTextView.setBackgroundResource(R.drawable.message_item_cell_them);
            messageTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
        }
        ((LinearLayout.LayoutParams) messageTextView.getLayoutParams()).gravity = gravity;
        ((LinearLayout.LayoutParams) dateSentTextView.getLayoutParams()).gravity = gravity;
    }


}
