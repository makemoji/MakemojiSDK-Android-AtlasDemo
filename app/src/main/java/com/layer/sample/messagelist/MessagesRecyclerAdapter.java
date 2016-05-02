package com.layer.sample.messagelist;

import android.content.Context;
import android.support.annotation.Nullable;
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
import java.util.Map;

public class MessagesRecyclerAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private ParticipantProvider mParticipantProvider;
    private RecyclerViewController<Message> mQueryController;
    private String mAuthenticatedUserId;
    private Context mContext;
    private OnMessageAppendedListener mMessageAppenedListener;


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

    public void setMessageAppenedListener(OnMessageAppendedListener listener) {
        mMessageAppenedListener = listener;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);

        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = mQueryController.getItem(position);

        String userId = message.getSender().getUserId();
        Participant fromParticipant = mParticipantProvider.getParticipant(userId);
        boolean isSelf = userId.equals(mAuthenticatedUserId);
        holder.setIsUsersMessage(isSelf);
        if (isSelf) {
            holder.setParticipantName(null);
            holder.setStatusText(getDateWithStatusText(message));
        } else {
            holder.setParticipantName(fromParticipant.getName());
            holder.setStatusText(getDateText(message));
        }

        holder.setMessage(MessageUtils.getMessageText(message));
    }

    @Nullable
    private String getDateWithStatusText(Message message) {
        CharSequence formattedTime = getDateText(message);
        String status = getMessageStatus(message);

        if (formattedTime != null && status != null) {
            return formattedTime + " - " + status;
        } else if (formattedTime != null) {
            return formattedTime.toString();
        } else if (status != null) {
            return status;
        } else {
            return null;
        }
    }

    @Nullable
    private String getDateText(Message message) {
        Date sentDate = message.getSentAt();
        String formattedTime = null;
        if (sentDate != null) {
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (!DateUtils.isToday(sentDate.getTime())) {
                flags |= DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
            }
            formattedTime = DateUtils.formatDateTime(mContext, sentDate.getTime(), flags);
        }
        return formattedTime;
    }

    private String getMessageStatus(Message message) {
        String status = null;
        boolean sent = false;
        boolean delivered = false;
        Map<String, Message.RecipientStatus> recipientStatuses = message.getRecipientStatus();
        for (Map.Entry<String, Message.RecipientStatus> entry : recipientStatuses.entrySet()) {
            if (entry.getKey().equals(mAuthenticatedUserId)) {
                continue;
            }
            if (entry.getValue() == Message.RecipientStatus.READ) {
                status = mContext.getString(R.string.message_status_read);
                break;
            }
            switch (entry.getValue()) {
                case PENDING:
                    if (!sent && !delivered) {
                        status = mContext.getString(R.string.message_status_pending);
                    }
                    break;
                case SENT:
                    if (!delivered) {
                        status = mContext.getString(R.string.message_status_sent);
                    }
                    sent = true;
                    break;
                case DELIVERED:
                    status = mContext.getString(R.string.message_status_delivered);
                    delivered = true;
                    break;
            }

        }
        return status;
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
            if (mMessageAppenedListener != null && (position + 1) == getItemCount()) {
                mMessageAppenedListener.onMessageAppended();
            }
        }

        @Override
        public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
            int positionEnd = positionStart + itemCount;
            if (mMessageAppenedListener != null && (positionEnd + 1) == getItemCount()) {
                mMessageAppenedListener.onMessageAppended();
            }
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

    public interface OnMessageAppendedListener {
        void onMessageAppended();
    }
}
