package com.layer.messenger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.layer.atlas.AtlasAddressBar;
import com.layer.atlas.AtlasHistoricMessagesFetchLayout;
import com.layer.atlas.AtlasMessageComposer;
import com.layer.atlas.AtlasMessagesRecyclerView;
import com.layer.atlas.AtlasTypingIndicator;
import com.layer.atlas.messagetypes.generic.GenericCellFactory;
import com.layer.atlas.messagetypes.location.LocationCellFactory;
import com.layer.atlas.messagetypes.location.LocationSender;
import com.layer.atlas.messagetypes.singlepartimage.SinglePartImageCellFactory;
import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.messagetypes.text.TextSender;
import com.layer.atlas.messagetypes.threepartimage.CameraSender;
import com.layer.atlas.messagetypes.threepartimage.GallerySender;
import com.layer.atlas.messagetypes.threepartimage.ThreePartImageCellFactory;
import com.layer.atlas.typingindicators.BubbleTypingIndicatorFactory;
import com.layer.atlas.util.Util;
import com.layer.atlas.util.views.SwipeableItem;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerConversationException;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;

import java.util.List;

public class MessagesListActivity extends BaseActivity {
    private UiState mState;
    private Conversation mConversation;

    private AtlasAddressBar mAddressBar;
    private AtlasHistoricMessagesFetchLayout mHistoricFetchLayout;
    private AtlasMessagesRecyclerView mMessagesList;
    private AtlasTypingIndicator mTypingIndicator;
    private AtlasMessageComposer mMessageComposer;

    public MessagesListActivity() {
        super(R.layout.activity_messages_list, R.menu.menu_messages_list, R.string.title_select_conversation, true);
    }

    private void setUiState(UiState state) {
        if (mState == state) return;
        mState = state;
        switch (state) {
            case ADDRESS:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.VISIBLE);
                mHistoricFetchLayout.setVisibility(View.GONE);
                mMessageComposer.setVisibility(View.GONE);
                break;

            case ADDRESS_COMPOSER:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.VISIBLE);
                mHistoricFetchLayout.setVisibility(View.GONE);
                mMessageComposer.setVisibility(View.VISIBLE);
                break;

            case ADDRESS_CONVERSATION_COMPOSER:
                mAddressBar.setVisibility(View.VISIBLE);
                mAddressBar.setSuggestionsVisibility(View.GONE);
                mHistoricFetchLayout.setVisibility(View.VISIBLE);
                mMessageComposer.setVisibility(View.VISIBLE);
                break;

            case CONVERSATION_COMPOSER:
                mAddressBar.setVisibility(View.GONE);
                mAddressBar.setSuggestionsVisibility(View.GONE);
                mHistoricFetchLayout.setVisibility(View.VISIBLE);
                mMessageComposer.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (App.routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        mAddressBar = ((AtlasAddressBar) findViewById(R.id.conversation_launcher))
                .init(getLayerClient(), getParticipantProvider(), getPicasso())
                .setOnConversationClickListener(new AtlasAddressBar.OnConversationClickListener() {
                    @Override
                    public void onConversationClick(AtlasAddressBar addressBar, Conversation conversation) {
                        setConversation(conversation, true);
                        setTitle(true);
                    }
                })
                .setOnParticipantSelectionChangeListener(new AtlasAddressBar.OnParticipantSelectionChangeListener() {
                    @Override
                    public void onParticipantSelectionChanged(AtlasAddressBar addressBar, final List<String> participantIds) {
                        if (participantIds.isEmpty()) {
                            setConversation(null, false);
                            return;
                        }
                        try {
                            setConversation(getLayerClient().newConversation(new ConversationOptions().distinct(true), participantIds), false);
                        } catch (LayerConversationException e) {
                            setConversation(e.getConversation(), false);
                        }
                    }
                })
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (mState == UiState.ADDRESS_CONVERSATION_COMPOSER) {
                            mAddressBar.setSuggestionsVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }
                })
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            setUiState(UiState.CONVERSATION_COMPOSER);
                            setTitle(true);
                            return true;
                        }
                        return false;
                    }
                });

        mHistoricFetchLayout = ((AtlasHistoricMessagesFetchLayout) findViewById(R.id.historic_sync_layout))
                .init(getLayerClient())
                .setHistoricMessagesPerFetch(20);

        mMessagesList = ((AtlasMessagesRecyclerView) findViewById(R.id.messages_list))
                .init(getLayerClient(), getParticipantProvider(), getPicasso())
                .addCellFactories(
                        new TextCellFactory(),
                        new ThreePartImageCellFactory(this, getLayerClient(), getPicasso()),
                        new LocationCellFactory(this, getPicasso()),
                        new SinglePartImageCellFactory(this, getLayerClient(), getPicasso()),
                        new GenericCellFactory())
                .setOnMessageSwipeListener(new SwipeableItem.OnSwipeListener<Message>() {
                    @Override
                    public void onSwipe(final Message message, int direction) {
                        new AlertDialog.Builder(MessagesListActivity.this)
                                .setMessage(R.string.alert_message_delete_message)
                                .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO: simply update this one message
                                        mMessagesList.getAdapter().notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                })
                                .setNeutralButton(R.string.alert_button_delete_my_devices, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        message.delete(LayerClient.DeletionMode.ALL_MY_DEVICES);
                                    }
                                })
                                .setPositiveButton(R.string.alert_button_delete_all_participants, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        message.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
                                    }
                                })
                                .show();
                    }
                });

        mTypingIndicator = new AtlasTypingIndicator(this)
                .init(getLayerClient())
                .setTypingIndicatorFactory(new BubbleTypingIndicatorFactory())
                .setTypingActivityListener(new AtlasTypingIndicator.TypingActivityListener() {
                    @Override
                    public void onTypingActivityChange(AtlasTypingIndicator typingIndicator, boolean active) {
                        mMessagesList.setFooterView(active ? typingIndicator : null);
                    }
                });

        mMessageComposer = ((AtlasMessageComposer) findViewById(R.id.message_composer))
                .init(getLayerClient(), getParticipantProvider())
                .setTextSender(new TextSender())
                .addAttachmentSenders(
                        new CameraSender(R.string.attachment_menu_camera, R.drawable.ic_photo_camera_white_24dp, this),
                        new GallerySender(R.string.attachment_menu_gallery, R.drawable.ic_photo_white_24dp, this),
                        new LocationSender(R.string.attachment_menu_location, R.drawable.ic_place_white_24dp, this))
                .setOnMessageEditTextFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            setUiState(UiState.CONVERSATION_COMPOSER);
                            setTitle(true);
                        }
                    }
                });

        // Get or create Conversation from Intent extras
        Conversation conversation = null;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY)) {
                Uri conversationId = intent.getParcelableExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY);
                conversation = getLayerClient().getConversation(conversationId);
            } else if (intent.hasExtra("participantIds")) {
                String[] participantIds = intent.getStringArrayExtra("participantIds");
                try {
                    conversation = getLayerClient().newConversation(new ConversationOptions().distinct(true), participantIds);
                } catch (LayerConversationException e) {
                    conversation = e.getConversation();
                }
            }
        }
        setConversation(conversation, conversation != null);
    }

    @Override
    protected void onResume() {
        // Clear any notifications for this conversation
        PushNotificationReceiver.getNotifications(this).clear(mConversation);
        super.onResume();
        setTitle(mConversation != null);
    }

    @Override
    protected void onPause() {
        // Update the notification position to the latest seen
        PushNotificationReceiver.getNotifications(this).clear(mConversation);
        super.onPause();
    }

    public void setTitle(boolean useConversation) {
        if (!useConversation) {
            setTitle(R.string.title_select_conversation);
        } else {
            setTitle(Util.getConversationTitle(getLayerClient(), getParticipantProvider(), mConversation));
        }
    }

    private void setConversation(Conversation conversation, boolean hideLauncher) {
        mConversation = conversation;
        mHistoricFetchLayout.setConversation(conversation);
        mMessagesList.setConversation(conversation);
        mTypingIndicator.setConversation(conversation);
        mMessageComposer.setConversation(conversation);

        // UI state
        if (conversation == null) {
            setUiState(UiState.ADDRESS);
            return;
        }

        if (hideLauncher) {
            setUiState(UiState.CONVERSATION_COMPOSER);
            return;
        }

        if (conversation.getHistoricSyncStatus() == Conversation.HistoricSyncStatus.INVALID) {
            // New "temporary" conversation
            setUiState(UiState.ADDRESS_COMPOSER);
        } else {
            setUiState(UiState.ADDRESS_CONVERSATION_COMPOSER);
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMessageComposer.onActivityResult(this, requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mMessageComposer.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private enum UiState {
        ADDRESS,
        ADDRESS_COMPOSER,
        ADDRESS_CONVERSATION_COMPOSER,
        CONVERSATION_COMPOSER
    }
}
