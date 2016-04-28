package com.layer.sample.messagelist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.layer.sample.Participant;
import com.layer.sample.ParticipantProvider;
import com.layer.sample.R;
import com.layer.sample.util.MessageUtils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;

import java.util.Date;

public class MessagesRecyclerAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private ParticipantProvider mParticipantProvider;
    private RecyclerViewController<Message> mQueryController;
    private String mAuthenticatedUserId;
    private Context mContext;


    public MessagesRecyclerAdapter(Context context, LayerClient layerClient, ParticipantProvider participantProvider) {
        mContext = context;
        mParticipantProvider = participantProvider;
        mAuthenticatedUserId = layerClient.getAuthenticatedUserId();
        mQueryController = layerClient.newRecyclerViewController(null, null, new NotifyChangesCallback());
    }

    public void setConversation(Conversation conversation) {
        Query<Message> messageQuery = Query.builder(Message.class)
                .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.ASCENDING))
                .build();
        mQueryController.setQuery(messageQuery);
        mQueryController.execute();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);

        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = mQueryController.getItem(position);

        // Set participant name
        String userId = message.getSender().getUserId();
        Participant fromParticipant = mParticipantProvider.getParticipant(userId);
        boolean isSelf = userId.equals(mAuthenticatedUserId);
        holder.setIsUsersMessage(isSelf);
        if (isSelf) {
            holder.setParticipantName(null);
        } else {
            holder.setParticipantName(fromParticipant.getName());
        }

        // Set message
        holder.setMessage(MessageUtils.getMessageText(message));

        // Set date
        Date sentAt = message.getSentAt();
        String date = DateUtils.formatDateTime(mContext, sentAt.getTime(), DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_TIME);
        holder.setDateSent(date);
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount();
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
