package com.layer.sample.conversationlist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.layer.sample.R;

public class ConversationViewHolder extends RecyclerView.ViewHolder {


    private TextView mTitleTextView;
    private TextView mMessageTextView;
    private TextView mLastMessageTimeTextView;
    private View mItemView;

    public ConversationViewHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
        mTitleTextView = (TextView) itemView.findViewById(R.id.conversation_title);
        mMessageTextView = (TextView) itemView.findViewById(R.id.conversation_message);
        mLastMessageTimeTextView = (TextView) itemView.findViewById(R.id.conversation_last_message_time);
    }

    public void setName(String name) {
        mTitleTextView.setText(name);
    }

    public void setMessage(String message) {
        mMessageTextView.setText(message);
    }

    public void setLastMessageTime(CharSequence time) {
        mLastMessageTimeTextView.setText(time);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mItemView.setOnClickListener(listener);
    }
}
