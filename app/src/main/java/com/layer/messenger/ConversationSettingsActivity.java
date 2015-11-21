package com.layer.messenger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerPolicyListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.policy.Policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConversationSettingsActivity extends BaseActivity implements LayerPolicyListener, LayerChangeEventListener {
    private EditText mConversationName;
    private Switch mShowNotifications;
    private RecyclerView mParticipantRecyclerView;
    private Button mLeaveButton;
    private Button mAddParticipantsButton;

    private Conversation mConversation;
    private ParticipantAdapter mParticipantAdapter;

    public ConversationSettingsActivity() {
        super(R.layout.activity_conversation_settings, R.menu.menu_conversation_details, R.string.title_conversation_details, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConversationName = (EditText) findViewById(R.id.conversation_name);
        mShowNotifications = (Switch) findViewById(R.id.show_notifications_switch);
        mParticipantRecyclerView = (RecyclerView) findViewById(R.id.participants);
        mLeaveButton = (Button) findViewById(R.id.leave_button);
        mAddParticipantsButton = (Button) findViewById(R.id.add_participant_button);

        // Get Conversation from Intent extras
        Uri conversationId = getIntent().getParcelableExtra(PushNotificationReceiver.LAYER_CONVERSATION_KEY);
        mConversation = getLayerClient().getConversation(conversationId);
        if (mConversation == null && !isFinishing()) finish();

        mParticipantAdapter = new ParticipantAdapter();
        mParticipantRecyclerView.setAdapter(mParticipantAdapter);

        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mParticipantRecyclerView.setLayoutManager(manager);

        mConversationName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String title = ((EditText) v).getText().toString().trim();
                    Util.setConversationMetadataTitle(mConversation, title);
                    Toast.makeText(v.getContext(), R.string.toast_group_name_updated, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        mShowNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PushNotificationReceiver.getNotifications(ConversationSettingsActivity.this)
                        .setEnabled(mConversation.getId(), isChecked);
            }
        });

        mLeaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnabled(false);
                mConversation.removeParticipants(getLayerClient().getAuthenticatedUserId());
                refresh();
                Intent intent = new Intent(ConversationSettingsActivity.this, ConversationsListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                setEnabled(true);
                ConversationSettingsActivity.this.startActivity(intent);
            }
        });

        mAddParticipantsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
                Toast.makeText(v.getContext(), "Coming soon", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void setEnabled(boolean enabled) {
        mShowNotifications.setEnabled(enabled);
        mLeaveButton.setEnabled(enabled);
    }

    private void refresh() {
        if (!getLayerClient().isAuthenticated()) return;

        mConversationName.setText(Util.getConversationMetadataTitle(mConversation));
        mShowNotifications.setChecked(PushNotificationReceiver.getNotifications(this).isEnabled(mConversation.getId()));

        Set<String> participantsMinusMe = new HashSet<String>(mConversation.getParticipants());
        participantsMinusMe.remove(getLayerClient().getAuthenticatedUserId());

        if (participantsMinusMe.size() == 0) {
            // I've been removed
            mConversationName.setEnabled(false);
            mLeaveButton.setVisibility(View.GONE);
        } else if (participantsMinusMe.size() == 1) {
            // 1-on-1
            mConversationName.setEnabled(false);
            mLeaveButton.setVisibility(View.GONE);
        } else {
            // Group
            mConversationName.setEnabled(true);
            mLeaveButton.setVisibility(View.VISIBLE);
        }
        mParticipantAdapter.refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLayerClient().registerPolicyListener(this).registerEventListener(this);
        setEnabled(true);
        refresh();
    }

    @Override
    protected void onPause() {
        getLayerClient().unregisterPolicyListener(this).unregisterEventListener(this);
        super.onPause();
    }

    @Override
    public void onPolicyListUpdate(LayerClient layerClient, List<Policy> list, List<Policy> list1) {
        refresh();
    }

    @Override
    public void onChangeEvent(LayerChangeEvent layerChangeEvent) {
        refresh();
    }

    private class ParticipantAdapter extends RecyclerView.Adapter<ViewHolder> {
        List<Participant> mParticipants = new ArrayList<Participant>();

        public void refresh() {
            // Get new sorted list of Participants
            ParticipantProvider provider = App.getParticipantProvider();
            mParticipants.clear();
            for (String participantId : mConversation.getParticipants()) {
                if (participantId.equals(getLayerClient().getAuthenticatedUserId())) continue;
                Participant participant = provider.getParticipant(participantId);
                if (participant == null) continue;
                mParticipants.add(participant);
            }
            Collections.sort(mParticipants);

            // Adjust participant container height
            int height = Math.round(mParticipants.size() * getResources().getDimensionPixelSize(com.layer.atlas.R.dimen.atlas_secondary_item_height));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            mParticipantRecyclerView.setLayoutParams(params);

            // Notify changes
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(parent);
            viewHolder.mAvatar.init(App.getParticipantProvider(), App.getPicasso());
            viewHolder.itemView.setTag(viewHolder);

            // Click to display remove / block dialog
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ViewHolder holder = (ViewHolder) v.getTag();

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext())
                            .setMessage(holder.mTitle.getText().toString());

                    if (mConversation.getParticipants().size() > 2) {
                        builder.setNeutralButton(R.string.alert_button_remove, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mConversation.removeParticipants(holder.mParticipant.getId());
                            }
                        });
                    }

                    builder.setPositiveButton(holder.mBlockPolicy != null ? R.string.alert_button_unblock : R.string.alert_button_block,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Participant participant = holder.mParticipant;
                                    if (holder.mBlockPolicy == null) {
                                        // Block
                                        holder.mBlockPolicy = new Policy.Builder(Policy.PolicyType.BLOCK).sentByUserId(participant.getId()).build();
                                        getLayerClient().addPolicy(holder.mBlockPolicy);
                                        holder.mBlocked.setVisibility(View.VISIBLE);
                                    } else {
                                        getLayerClient().removePolicy(holder.mBlockPolicy);
                                        holder.mBlockPolicy = null;
                                        holder.mBlocked.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }).setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                }
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            Participant participant = mParticipants.get(position);
            viewHolder.mTitle.setText(participant.getName());
            viewHolder.mAvatar.setParticipants(participant.getId());
            viewHolder.mParticipant = participant;

            Policy block = null;
            for (Policy policy : getLayerClient().getPolicies()) {
                if (policy.getPolicyType() != Policy.PolicyType.BLOCK) continue;
                if (!policy.getSentByUserID().equals(participant.getId())) continue;
                block = policy;
                break;
            }

            viewHolder.mBlockPolicy = block;
            viewHolder.mBlocked.setVisibility(block == null ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return mParticipants.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        AtlasAvatar mAvatar;
        TextView mTitle;
        ImageView mBlocked;
        Participant mParticipant;
        Policy mBlockPolicy;

        public ViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.participant_item, parent, false));
            mAvatar = (AtlasAvatar) itemView.findViewById(R.id.avatar);
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mBlocked = (ImageView) itemView.findViewById(R.id.blocked);
        }
    }
}
