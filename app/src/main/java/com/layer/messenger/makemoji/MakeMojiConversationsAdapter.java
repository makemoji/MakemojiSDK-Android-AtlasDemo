package com.layer.messenger.makemoji;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.adapters.AtlasBaseAdapter;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.ConversationStyle;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Collection;
import java.util.HashSet;

/**
 * A copy paste of the AtlasConversationAdapter so that we can insert logic to render a preview of the makemoji conversation where appropriate.
 * This is necessary because the adapter does not currently support registering dynamic types and delegates the preview render to Util.java, which lists it as a todo.
 * Created by s_baa on 5/30/2016.
 */
    public class MakeMojiConversationsAdapter extends RecyclerView.Adapter<MakeMojiConversationsAdapter.ViewHolder> implements AtlasBaseAdapter<Conversation>, RecyclerViewController.Callback {
        protected final LayerClient mLayerClient;
        protected final ParticipantProvider mParticipantProvider;
        protected final Picasso mPicasso;
        private final RecyclerViewController<Conversation> mQueryController;
        private final LayoutInflater mInflater;
        private long mInitialHistory = 0;

        private OnConversationClickListener mConversationClickListener;
        private ViewHolder.OnClickListener mViewHolderClickListener;

        private final DateFormat mDateFormat;
        private final DateFormat mTimeFormat;
        private ConversationStyle conversationStyle;

        public MakeMojiConversationsAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso) {
            this(context, client, participantProvider, picasso, null);
        }

        public MakeMojiConversationsAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso, Collection<String> updateAttributes) {
            Query<Conversation> query = Query.builder(Conversation.class)
                /* Only show conversations we're still a member of */
                    .predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 1))

                /* Sort by the last Message's receivedAt time */
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_RECEIVED_AT, SortDescriptor.Order.DESCENDING))
                    .build();
            mQueryController = client.newRecyclerViewController(query, updateAttributes, this);
            mLayerClient = client;
            mParticipantProvider = participantProvider;
            mPicasso = picasso;
            mInflater = LayoutInflater.from(context);
            mDateFormat = android.text.format.DateFormat.getDateFormat(context);
            mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
            mViewHolderClickListener = new ViewHolder.OnClickListener() {
                @Override
                public void onClick(ViewHolder viewHolder) {
                    if (mConversationClickListener == null) return;
                    mConversationClickListener.onConversationClick(MakeMojiConversationsAdapter.this, viewHolder.getConversation());
                }

                @Override
                public boolean onLongClick(ViewHolder viewHolder) {
                    if (mConversationClickListener == null) return false;
                    return mConversationClickListener.onConversationLongClick(MakeMojiConversationsAdapter.this, viewHolder.getConversation());
                }
            };
            setHasStableIds(false);
        }

        /**
         * Refreshes this adapter by re-running the underlying Query.
         */
        public void refresh() {
            mQueryController.execute();
        }


        //==============================================================================================
        // Initial message history
        //==============================================================================================

        public MakeMojiConversationsAdapter setInitialHistoricMessagesToFetch(long initialHistory) {
            mInitialHistory = initialHistory;
            return this;
        }

        public void setStyle(ConversationStyle conversationStyle) {
            this.conversationStyle = conversationStyle;
        }

        private void syncInitialMessages(final int start, final int length) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long desiredHistory = mInitialHistory;
                    if (desiredHistory <= 0) return;
                    for (int i = start; i < start + length; i++) {
                        try {
                            final Conversation conversation = getItem(i);
                            if (conversation == null || conversation.getHistoricSyncStatus() != Conversation.HistoricSyncStatus.MORE_AVAILABLE) {
                                continue;
                            }
                            Query<Message> localCountQuery = Query.builder(Message.class)
                                    .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                                    .build();
                            long delta = desiredHistory - mLayerClient.executeQueryForCount(localCountQuery);
                            if (delta > 0) conversation.syncMoreHistoricMessages((int) delta);
                        } catch (IndexOutOfBoundsException e) {
                            // Concurrent modification
                        }
                    }
                }
            }).start();
        }


        //==============================================================================================
        // Listeners
        //==============================================================================================

        public MakeMojiConversationsAdapter setOnConversationClickListener(OnConversationClickListener conversationClickListener) {
            mConversationClickListener = conversationClickListener;
            return this;
        }


        //==============================================================================================
        // Adapter
        //==============================================================================================

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(mInflater.inflate(ViewHolder.RESOURCE_ID, parent, false), conversationStyle);
            viewHolder.setClickListener(mViewHolderClickListener);
            viewHolder.mAvatarCluster
                    .init(mParticipantProvider, mPicasso)
                    .setStyle(conversationStyle.getAvatarStyle());
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            mQueryController.updateBoundPosition(position);
            Conversation conversation = mQueryController.getItem(position);
            Message lastMessage = conversation.getLastMessage();
            Context context = viewHolder.itemView.getContext();

            viewHolder.setConversation(conversation);
            HashSet<String> participantIds = new HashSet<String>(conversation.getParticipants());
            participantIds.remove(mLayerClient.getAuthenticatedUserId());
            viewHolder.mAvatarCluster.setParticipants(participantIds);
            viewHolder.mTitleView.setText(Util.getConversationTitle(mLayerClient, mParticipantProvider, conversation));
            viewHolder.applyStyle(conversation.getTotalUnreadMessageCount() > 0);

            if (lastMessage == null) {
                viewHolder.mMessageView.setText(null);
                viewHolder.mTimeView.setText(null);
            } else {
                //custom logic here
                if (MakeMojiCellFactory.isType(lastMessage))
                    MakeMojiCellFactory.setMessagePreview(viewHolder.mMessageView,lastMessage);
                else {
                    viewHolder.mMessageView.setText(Util.getLastMessageString(context, lastMessage));
                }
                if (lastMessage.getReceivedAt() == null) {
                    viewHolder.mTimeView.setText(null);
                } else {
                    viewHolder.mTimeView.setText(Util.formatTime(context, lastMessage.getReceivedAt(), mTimeFormat, mDateFormat));
                }
            }
        }

        @Override
        public int getItemCount() {
            return mQueryController.getItemCount();
        }

        @Override
        public Integer getPosition(Conversation conversation) {
            return mQueryController.getPosition(conversation);
        }

        @Override
        public Integer getPosition(Conversation conversation, int lastPosition) {
            return mQueryController.getPosition(conversation, lastPosition);
        }

        @Override
        public Conversation getItem(int position) {
            return mQueryController.getItem(position);
        }

        @Override
        public Conversation getItem(RecyclerView.ViewHolder viewHolder) {
            return ((ViewHolder) viewHolder).getConversation();
        }


        //==============================================================================================
        // UI update callbacks
        //==============================================================================================

        @Override
        public void onQueryDataSetChanged(RecyclerViewController controller) {
            syncInitialMessages(0, getItemCount());
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
            syncInitialMessages(position, 1);
            notifyItemInserted(position);
        }

        @Override
        public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
            syncInitialMessages(positionStart, itemCount);
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


        //==============================================================================================
        // Inner classes
        //==============================================================================================

        static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
            // Layout to inflate
            public final static int RESOURCE_ID = com.layer.atlas.R.layout.atlas_conversation_item;

            // View cache
            protected TextView mTitleView;
            protected AtlasAvatar mAvatarCluster;
            protected TextView mMessageView;
            protected TextView mTimeView;

            protected ConversationStyle conversationStyle;
            protected Conversation mConversation;
            protected OnClickListener mClickListener;

            public ViewHolder(View itemView, ConversationStyle conversationStyle) {
                super(itemView);
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
                this.conversationStyle = conversationStyle;

                mAvatarCluster = (AtlasAvatar) itemView.findViewById(com.layer.atlas.R.id.avatar);
                mTitleView = (TextView) itemView.findViewById(com.layer.atlas.R.id.title);
                mMessageView = (TextView) itemView.findViewById(com.layer.atlas.R.id.last_message);
                mTimeView = (TextView) itemView.findViewById(com.layer.atlas.R.id.time);
                itemView.setBackgroundColor(conversationStyle.getCellBackgroundColor());
            }

            public void applyStyle(boolean unread) {
                mTitleView.setTextColor(unread ? conversationStyle.getTitleUnreadTextColor() : conversationStyle.getTitleTextColor());
                mTitleView.setTypeface(unread ? conversationStyle.getTitleUnreadTextTypeface() : conversationStyle.getTitleTextTypeface(), unread ? conversationStyle.getTitleUnreadTextStyle() : conversationStyle.getTitleTextStyle());
                mMessageView.setTextColor(unread ? conversationStyle.getSubtitleTextColor() : conversationStyle.getSubtitleTextColor());
                mMessageView.setTypeface(unread ? conversationStyle.getSubtitleUnreadTextTypeface() : conversationStyle.getSubtitleTextTypeface(), unread ? conversationStyle.getSubtitleUnreadTextStyle() : conversationStyle.getSubtitleTextStyle());
                mTimeView.setTextColor(conversationStyle.getDateTextColor());
                mTimeView.setTypeface(conversationStyle.getDateTextTypeface());
            }

            protected ViewHolder setClickListener(OnClickListener clickListener) {
                mClickListener = clickListener;
                return this;
            }

            public Conversation getConversation() {
                return mConversation;
            }

            public void setConversation(Conversation conversation) {
                mConversation = conversation;
            }

            @Override
            public void onClick(View v) {
                if (mClickListener == null) return;
                mClickListener.onClick(this);
            }

            @Override
            public boolean onLongClick(View v) {
                if (mClickListener == null) return false;
                return mClickListener.onLongClick(this);
            }

            interface OnClickListener {
                void onClick(ViewHolder viewHolder);

                boolean onLongClick(ViewHolder viewHolder);
            }
        }

        /**
         * Listens for item clicks on an IntegrationConversationsAdapter.
         */
        public interface OnConversationClickListener {
            /**
             * Alerts the listener to item clicks.
             *
             * @param adapter      The IntegrationConversationsAdapter which had an item clicked.
             * @param conversation The item clicked.
             */
            void onConversationClick(MakeMojiConversationsAdapter adapter, Conversation conversation);

            /**
             * Alerts the listener to long item clicks.
             *
             * @param adapter      The IntegrationConversationsAdapter which had an item long-clicked.
             * @param conversation The item long-clicked.
             * @return true if the long-click was handled, false otherwise.
             */
            boolean onConversationLongClick(MakeMojiConversationsAdapter adapter, Conversation conversation);
        }
    }

