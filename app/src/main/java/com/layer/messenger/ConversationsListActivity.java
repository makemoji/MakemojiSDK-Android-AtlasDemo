package com.layer.messenger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.layer.atlas.AtlasConversationsRecyclerView;
import com.layer.atlas.adapters.AtlasConversationsAdapter;
import com.layer.atlas.utilities.views.SwipeableItem;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

public class ConversationsListActivity extends BaseActivity {
    private static final String TAG = ConversationsListActivity.class.getSimpleName();

    public ConversationsListActivity() {
        super(R.layout.activity_conversations_list, R.menu.menu_conversations_list, R.string.title_conversations_list, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (App.routeLogin(this)) {
            finish();
            return;
        }

        final AtlasConversationsRecyclerView conversationsList = (AtlasConversationsRecyclerView) findViewById(R.id.conversations_list);

        // Atlas methods
        conversationsList.init(getLayerClient(), getParticipantProvider(), getPicasso())
                .setInitialHistoricMessagesToFetch(20)
                .setOnConversationClickListener(new AtlasConversationsAdapter.OnConversationClickListener() {
                    @Override
                    public void onConversationClick(AtlasConversationsAdapter adapter, Conversation conversation) {
                        Intent intent = new Intent(ConversationsListActivity.this, MessagesListActivity.class);
                        Log.v(TAG, "Launching MessagesListActivity with existing conversation ID: " + conversation.getId());
                        intent.putExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY, conversation.getId());
                        startActivity(intent);
                    }

                    @Override
                    public boolean onConversationLongClick(AtlasConversationsAdapter adapter, Conversation conversation) {
                        Toast.makeText(ConversationsListActivity.this, "Long: " + conversation.getId().toString(), Toast.LENGTH_LONG).show();
                        return true;
                    }
                })
                .setOnConversationSwipeListener(new SwipeableItem.OnSwipeListener<Conversation>() {
                    @Override
                    public void onSwipe(final Conversation conversation, int direction) {
                        new AlertDialog.Builder(ConversationsListActivity.this)
                                .setMessage("Delete Conversation?")
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO: simply update this one conversation
                                        conversationsList.getAdapter().notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        conversation.delete(LayerClient.DeletionMode.ALL_PARTICIPANTS);
                                    }
                                }).show();
                    }
                });

        findViewById(R.id.floating_action_button)
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(ConversationsListActivity.this, MessagesListActivity.class));
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.routeLogin(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_sendlogs:
                LayerClient.sendLogs(getLayerClient(), this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
