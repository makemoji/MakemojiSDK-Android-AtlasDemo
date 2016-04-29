package com.layer.sample.conversationlist;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.layer.sample.Participant;
import com.layer.sample.ParticipantProvider;
import com.layer.sample.PushNotificationReceiver;
import com.layer.sample.R;
import com.layer.sample.messagelist.MessagesListActivity;
import com.layer.sample.util.MessageUtils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ConversationRecyclerAdapter extends RecyclerView.Adapter<ConversationViewHolder> {

    private ParticipantProvider mParticipantProvider;
    private RecyclerViewController<Conversation> mQueryController;
    private String mAuthenticatedUserId;
    private Context mContext;

    public ConversationRecyclerAdapter(Context context, LayerClient layerClient, ParticipantProvider participantProvider) {
        mContext = context;
        mParticipantProvider = participantProvider;
        mAuthenticatedUserId = layerClient.getAuthenticatedUserId();
        setHasStableIds(false);

        Query<Conversation> query = Query.builder(Conversation.class)
                /* Only show conversations we're still a member of */
                .predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 1))

                /* Sort by the last Message's sentAt time */
                .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING))
                .build();

        mQueryController = layerClient.newRecyclerViewController(query, null, new NotifyChangesCallback());
        mQueryController.execute();
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);


        return new ConversationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        mQueryController.updateBoundPosition(position);
        final Conversation conversation = mQueryController.getItem(position);
        List<String> participantIds = conversation.getParticipants();

        setName(holder, participantIds);
        holder.setOnClickListener(new ItemClickListener(conversation));

        Message lastMessage = conversation.getLastMessage();
        setMessage(holder, lastMessage);
        setMessageDate(holder, lastMessage);
    }

    private void setMessageDate(ConversationViewHolder holder, Message lastMessage) {
        Date sentDate = lastMessage.getSentAt();
        if (sentDate != null) {
            CharSequence formattedTime = DateUtils.formatSameDayTime(sentDate.getTime(), System.currentTimeMillis(), DateFormat.DEFAULT, DateFormat.SHORT);
            holder.setLastMessageTime(formattedTime);
        } else {
            holder.setLastMessageTime(null);
        }
    }

    private void setMessage(ConversationViewHolder holder, Message lastMessage) {
        holder.setMessage(MessageUtils.getMessageText(lastMessage));
    }

    private void setName(ConversationViewHolder holder, List<String> participantIds) {
        StringBuilder sb = new StringBuilder();
        for (String participantId : participantIds) {
            if (mAuthenticatedUserId.equals(participantId)) {
                continue;
            }
            Participant participant = mParticipantProvider.getParticipant(participantId);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(participant.getName());
        }

        holder.setName(sb.toString());
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount();
    }

    private static class ItemClickListener implements View.OnClickListener {
        private final Conversation conversation;

        public ItemClickListener(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(), MessagesListActivity.class);
            intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, conversation.getId());
            v.getContext().startActivity(intent);
        }
    }

    private class NotifyChangesCallback implements RecyclerViewController.Callback {
        @Override
        public void onQueryDataSetChanged(RecyclerViewController controller) {
            notifyDataSetChanged();
        }

        @Override
        public void onQueryItemChanged(RecyclerViewController controller, int position) {
            notifyItemChanged(position);
        }

        @Override
        public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onQueryItemInserted(RecyclerViewController controller, int position) {
            notifyItemInserted(position);
        }

        @Override
        public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onQueryItemRemoved(RecyclerViewController controller, int position) {
            notifyItemRemoved(position);
        }

        @Override
        public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    }
}
