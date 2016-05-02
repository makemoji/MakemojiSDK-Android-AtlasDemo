package com.layer.sample.messagelist;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;

import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;

public class MessageRefreshListener implements OnRefreshListener, LayerChangeEventListener {
    private static final int MESSAGE_SYNC_AMOUNT = 25;

    private Conversation mConversation;
    private SwipeRefreshLayout mRefreshLayout;


    public MessageRefreshListener(Conversation conversation, SwipeRefreshLayout refreshLayout) {
        mConversation = conversation;
        mRefreshLayout = refreshLayout;
    }

    public void registerLayerListener(LayerClient layerClient) {
        layerClient.registerEventListener(this);
    }

    public void unregisterLayerListener(LayerClient layerClient) {
        layerClient.unregisterEventListener(this);
    }

    private void disableAndStopRefreshing() {
        mRefreshLayout.setEnabled(false);
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onRefresh() {
        if (mConversation != null &&
                mConversation.getHistoricSyncStatus() == Conversation.HistoricSyncStatus.MORE_AVAILABLE) {
            mConversation.syncMoreHistoricMessages(MESSAGE_SYNC_AMOUNT);
        } else {
            disableAndStopRefreshing();
        }
    }

    @Override
    public void onChangeEvent(LayerChangeEvent layerChangeEvent) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mConversation == null) {
                    disableAndStopRefreshing();
                    return;
                }
                Conversation.HistoricSyncStatus status = mConversation.getHistoricSyncStatus();
                mRefreshLayout.setEnabled(status == Conversation.HistoricSyncStatus.MORE_AVAILABLE);
                mRefreshLayout.setRefreshing(status == Conversation.HistoricSyncStatus.SYNC_PENDING);
            }
        });
    }
}
