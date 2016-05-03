package com.layer.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.layer.sample.messagelist.MessagesListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SelectParticipantsActivity extends BaseActivity {
    private static final String EXTRA_KEY_HAS_PARTICIPANTS = "hasParticipants";

    private boolean mHasCheckedParticipants;
    private ListView mParticipantList;
    private ParticipantAdapter mParticipantAdapter;

    public SelectParticipantsActivity() {
        super(R.layout.activity_new_conversation, R.menu.menu_select_participants, R.string.title_select_participants, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mHasCheckedParticipants = savedInstanceState.getBoolean(EXTRA_KEY_HAS_PARTICIPANTS);
        }

        mParticipantList = (ListView) findViewById(R.id.participant_list);

        List<Participant> sortedParticipants = getParticipantsExcludingCurrentUser();
        mParticipantAdapter = new ParticipantAdapter(this);
        mParticipantAdapter.addAll(sortedParticipants);

        mParticipantList.setAdapter(mParticipantAdapter);
        mParticipantList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mHasCheckedParticipants = mParticipantList.getCheckedItemCount() != 0;
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem doneButton = menu.findItem(R.id.action_done);
        doneButton.setVisible(mHasCheckedParticipants);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_done) {
            startConversationActivity();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_HAS_PARTICIPANTS, mHasCheckedParticipants);
    }

    @NonNull
    private List<Participant> getParticipantsExcludingCurrentUser() {
        HashMap<String, Participant> participantMap = new HashMap<>();
        getParticipantProvider().getMatchingParticipants(null, participantMap);
        participantMap.remove(getLayerClient().getAuthenticatedUserId());
        List<Participant> sortedParticipants = new ArrayList<>(participantMap.values());
        Collections.sort(sortedParticipants);
        return sortedParticipants;
    }

    private void startConversationActivity() {
        Intent intent = new Intent(this, MessagesListActivity.class);
        intent.putExtra(MessagesListActivity.EXTRA_KEY_PARTICIPANT_IDS, getSelectedParticipantIds());
        startActivity(intent);
    }

    private String[] getSelectedParticipantIds() {
        SparseBooleanArray positions = mParticipantList.getCheckedItemPositions();
        List<String> participantIds = new ArrayList<>(positions.size());

        for (int i = 0; i < positions.size(); i++) {
            int checkedPosition = positions.keyAt(i);
            Participant participant = mParticipantAdapter.getItem(checkedPosition);
            participantIds.add(participant.getId());
        }
        String[] participantIdArray = new String[participantIds.size()];
        return participantIds.toArray(participantIdArray);
    }

    private static class ParticipantAdapter extends ArrayAdapter<Participant> {

        public ParticipantAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_multiple_choice);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            CheckedTextView textView = (CheckedTextView) v;
            textView.setText(getItem(position).getName());
            return v;
        }
    }
}
