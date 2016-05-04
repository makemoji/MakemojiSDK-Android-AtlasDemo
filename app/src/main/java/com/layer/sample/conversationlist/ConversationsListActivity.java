package com.layer.sample.conversationlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.layer.sample.App;
import com.layer.sample.AppSettingsActivity;
import com.layer.sample.BaseActivity;
import com.layer.sample.R;
import com.layer.sample.SelectParticipantsActivity;
import com.layer.sdk.LayerClient;

public class ConversationsListActivity extends BaseActivity {

    public ConversationsListActivity() {
        super(R.layout.activity_conversations_list, R.menu.menu_conversations_list, R.string.title_conversations_list, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (App.routeLogin(this)) {
            if (!isFinishing()) finish();
            return;
        }

        setUpFab();
        setUpRecyclerViewAndAdapter();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, AppSettingsActivity.class));
                return true;

            case R.id.action_sendlogs:
                LayerClient.sendLogs(getLayerClient(), this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    private void setUpFab() {
        View floatingActionButton = findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ConversationsListActivity.this, SelectParticipantsActivity.class));
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void setUpRecyclerViewAndAdapter() {
        RecyclerView conversationsList = (RecyclerView) findViewById(R.id.conversations_list);
        conversationsList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        conversationsList.setHasFixedSize(true);
        conversationsList.addItemDecoration(new DividerItemDecoration(this));

        LayerClient layerClient = getLayerClient();
        ConversationRecyclerAdapter conversationsAdapter = new ConversationRecyclerAdapter(layerClient, getParticipantProvider());
        conversationsList.setAdapter(conversationsAdapter);
    }
}
