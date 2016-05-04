package com.layer.sample.messagelist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.layer.sample.Participant;
import com.layer.sample.ParticipantProvider;
import com.layer.sample.R;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;

import java.util.ArrayList;
import java.util.List;

public class TypingIndicatorListener implements LayerTypingIndicatorListener {
    private final List<String> mActiveTypists = new ArrayList<>();
    private final TextView mIndicatorView;
    private final ParticipantProvider mParticipantProvider;


    public TypingIndicatorListener(TextView indicatorView, ParticipantProvider participantProvider) {
        mIndicatorView = indicatorView;
        mParticipantProvider = participantProvider;
    }

    @Override
    public void onTypingIndicator(LayerClient layerClient, Conversation conversation, String userId, TypingIndicator typingIndicator) {
        if (typingIndicator == TypingIndicator.FINISHED) {
            mActiveTypists.remove(userId);
        } else if (!mActiveTypists.contains(userId)){
            mActiveTypists.add(userId);
        }
        refreshView();
    }

    private void refreshView() {
        String indicatorText = createTypistsString();
        mIndicatorView.setText(indicatorText);
        if (TextUtils.isEmpty(indicatorText)) {
            mIndicatorView.setVisibility(View.GONE);
        } else {
            mIndicatorView.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    private String createTypistsString() {
        StringBuilder sb = new StringBuilder();
        Context context = mIndicatorView.getContext();
        for (String typistId : mActiveTypists) {
            Participant participant = mParticipantProvider.getParticipant(typistId);
            if (participant != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(context.getString(R.string.typing_indicator_format, participant.getName()));
            }
        }
        return sb.toString();
    }
}
