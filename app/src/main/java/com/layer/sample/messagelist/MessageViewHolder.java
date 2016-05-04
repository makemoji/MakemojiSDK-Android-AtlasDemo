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

    private TextView mParticipantNameTextView;
    private TextView mMessageTextView;
    private TextView mStatusTextView;
    private View mItemView;

    public MessageViewHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
        mParticipantNameTextView = (TextView) itemView.findViewById(R.id.participant_name);
        mMessageTextView = (TextView) itemView.findViewById(R.id.message);
        mStatusTextView = (TextView) itemView.findViewById(R.id.message_status);
    }

    public void setParticipantName(String participantName) {
        if (TextUtils.isEmpty(participantName)) {
            mParticipantNameTextView.setVisibility(View.GONE);
        } else {
            mParticipantNameTextView.setVisibility(View.VISIBLE);
            mParticipantNameTextView.setText(participantName);
        }
    }

    public void setMessage(String message) {
        mMessageTextView.setText(message);
    }

    public void setStatusText(String status) {
        mStatusTextView.setText(status);
    }

    public void setIsUsersMessage(boolean isUsersMessage) {
        int gravity;
        if (isUsersMessage) {
            gravity = Gravity.END;
            mMessageTextView.setBackgroundResource(R.drawable.message_item_cell_me);
            mMessageTextView.setTextColor(ContextCompat.getColor(mItemView.getContext(), android.R.color.white));
        } else {
            gravity = Gravity.START;
            mMessageTextView.setBackgroundResource(R.drawable.message_item_cell_them);
            mMessageTextView.setTextColor(ContextCompat.getColor(mItemView.getContext(), android.R.color.black));
        }
        ((LinearLayout.LayoutParams) mMessageTextView.getLayoutParams()).gravity = gravity;
        ((LinearLayout.LayoutParams) mStatusTextView.getLayoutParams()).gravity = gravity;
    }


}
