package com.layer.sample.messagelist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.layer.sample.App;
import com.layer.sample.BaseActivity;
import com.layer.sample.ConversationSettingsActivity;
import com.layer.sample.PushNotificationReceiver;
import com.layer.sample.R;
import com.layer.sample.util.ConversationUtils;
import com.layer.sample.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerConversationException;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;

public class MessagesListActivity extends BaseActivity {
    public static final String EXTRA_KEY_PARTICIPANT_IDS = "participantIds";

    private static final int MAX_NOTIFICATION_LENGTH = 200;

    private RecyclerView mMessagesList;
    private LinearLayoutManager mMessagesListLayoutManager;
    private MessagesRecyclerAdapter mMessagesAdapter;
    private TypingIndicatorListener mTypingIndicatorListener;
    private EditText mMessageEntry;
    private Button mSendButton;
    private SwipeRefreshLayout mMessagesRefreshLayout;

    private MessageRefreshListener mMessagesRefreshListener;
    private Conversation mConversation;

    public MessagesListActivity() {
        super(R.layout.activity_messages_list, R.menu.menu_messages_list, R.string.title_select_conversation, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (App.routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        initializeUi();
        setConversationFromIntent();
        createAndSetRefreshListener();
        fetchMessages();
    }

    @Override
    protected void onResume() {
        // Clear any notifications for this conversation
        PushNotificationReceiver.getNotifications(this).clear(mConversation);
        super.onResume();
        setTitle(mConversation != null);
        getLayerClient().registerTypingIndicator(mTypingIndicatorListener);
        mMessagesRefreshListener.registerLayerListener(getLayerClient());
    }

    @Override
    protected void onPause() {
        // Update the notification position to the latest seen
        PushNotificationReceiver.getNotifications(this).clear(mConversation);
        super.onPause();
        getLayerClient().unregisterTypingIndicator(mTypingIndicatorListener);
        mMessagesRefreshListener.unregisterLayerListener(getLayerClient());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_details:
                if (mConversation == null) return true;
                Intent intent = new Intent(this, ConversationSettingsActivity.class);
                intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, mConversation.getId());
                startActivity(intent);
                return true;

            case R.id.action_sendlogs:
                LayerClient.sendLogs(getLayerClient(), this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeUi() {
        initializeMessagesAdapter();
        initializeMessagesList();
        initializeTypingIndicator();
        initializeMessageEntry();
        initializeSendButton();
    }

    private void initializeMessagesAdapter() {
        mMessagesAdapter = new MessagesRecyclerAdapter(this, getLayerClient(), getParticipantProvider());
        mMessagesAdapter.setMessageAppenedListener(new ScrollOnMessageAppendedListener());

        mMessagesRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
    }

    private void initializeMessagesList() {
        mMessagesList = (RecyclerView) findViewById(R.id.messages_list);
        mMessagesListLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mMessagesListLayoutManager.setStackFromEnd(true);
        mMessagesList.setLayoutManager(mMessagesListLayoutManager);
        mMessagesList.setAdapter(mMessagesAdapter);
    }

    private void initializeTypingIndicator() {
        TextView typingIndicatorView = (TextView) findViewById(R.id.typing_indicator);
        mTypingIndicatorListener = new TypingIndicatorListener(typingIndicatorView, getParticipantProvider());
    }

    private void initializeMessageEntry() {
        mMessageEntry = (EditText) findViewById(R.id.message_entry);
        mMessageEntry.addTextChangedListener(new MessageTextWatcher());
    }

    private void initializeSendButton() {
        mSendButton = (Button) findViewById(R.id.send_button);
    }

    private void setConversationFromIntent() {
        Conversation conversation = null;
        Intent intent = getIntent();
        if (intent.hasExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY)) {
            Uri conversationId = intent.getParcelableExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY);
            conversation = getLayerClient().getConversation(conversationId);
        } else if (intent.hasExtra(EXTRA_KEY_PARTICIPANT_IDS)) {
            String[] participantIds = intent.getStringArrayExtra(EXTRA_KEY_PARTICIPANT_IDS);
            try {
                conversation = getLayerClient().newConversation(new ConversationOptions().distinct(true), participantIds);
            } catch (LayerConversationException e) {
                conversation = e.getConversation();
            }
        }
        mConversation = conversation;
    }

    private void createAndSetRefreshListener() {
        mMessagesRefreshListener = new MessageRefreshListener(mConversation, mMessagesRefreshLayout);
        mMessagesRefreshLayout.setOnRefreshListener(mMessagesRefreshListener);
    }

    public void setTitle(boolean useConversation) {
        if (!useConversation) {
            setTitle(R.string.title_select_conversation);
        } else {
            setTitle(ConversationUtils.getConversationTitle(getLayerClient(), getParticipantProvider(), mConversation));
        }
    }

    private void fetchMessages() {
        mMessagesAdapter.setConversation(mConversation);
    }

    public void onSendClicked(View v) {
        if (Log.isLoggable(Log.VERBOSE)) {
            Log.v("Sending text message");
        }
        String text = mMessageEntry.getText().toString();

        // Send message
        String notificationString = createMessageNotificationString(text);
        MessageOptions messageOptions = new MessageOptions().pushNotificationMessage(notificationString);
        sendMessage(text, messageOptions);

        // Clear text
        mMessageEntry.setText(null);
    }

    private String createMessageNotificationString(String text) {
        String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
        String pushMessage = (text.length() < MAX_NOTIFICATION_LENGTH) ? text : (text.substring(0, MAX_NOTIFICATION_LENGTH) + "…");
        return String.format("%s: %s", myName, pushMessage);
    }

    private void sendMessage(String text, MessageOptions messageOptions) {
        MessagePart part = getLayerClient().newMessagePart(text);
        Message message = getLayerClient().newMessage(messageOptions, part);
        mConversation.send(message);
    }

    private class MessageTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mConversation == null || mConversation.isDeleted()) return;

            if (s.length() > 0 && !s.toString().trim().isEmpty()) {
                mSendButton.setEnabled(true);
                mConversation.send(LayerTypingIndicatorListener.TypingIndicator.STARTED);
            } else {
                mSendButton.setEnabled(false);
                mConversation.send(LayerTypingIndicatorListener.TypingIndicator.FINISHED);
            }
        }
    }

    private class ScrollOnMessageAppendedListener implements MessagesRecyclerAdapter.OnMessageAppendedListener {
        @Override
        public void onMessageAppended() {
            scrollOnNewMessage();
        }

        private void scrollOnNewMessage() {
            int end = mMessagesAdapter.getItemCount() - 1;
            if (end <= 0) return;
            int visible = mMessagesListLayoutManager.findLastVisibleItemPosition();
            // -3 because -1 seems too finicky
            if (visible >= (end - 3)) mMessagesList.scrollToPosition(end);
        }
    }
}
