package com.layer.messenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.util.Util;
import com.layer.messenger.util.Log;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerConnectionListener;
import com.layer.sdk.messaging.Conversation;

import java.util.List;

public class AppSettingsActivity extends BaseActivity implements LayerConnectionListener, LayerAuthenticationListener, LayerChangeEventListener, View.OnLongClickListener {
    /* Account */
    private AtlasAvatar mAvatar;
    private TextView mUserName;
    private TextView mUserState;
    private Button mLogoutButton;

    /* Notifications */
    private Switch mShowNotifications;

    /* Debug */
    private Switch mVerboseLogging;
    private TextView mAppVersion;
    private TextView mAndroidVersion;
    private TextView mAtlasVersion;
    private TextView mLayerVersion;
    private TextView mUserId;

    /* Statistics */
    private TextView mConversationCount;
    private TextView mMessageCount;
    private TextView mUnreadMessageCount;

    /* Rich Content */
    private TextView mDiskUtilization;
    private TextView mDiskAllowance;
    private TextView mAutoDownloadMimeTypes;

    public AppSettingsActivity() {
        super(R.layout.activity_app_settings, R.menu.menu_settings, R.string.title_settings, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View cache
        mAvatar = (AtlasAvatar) findViewById(R.id.avatar);
        mUserName = (TextView) findViewById(R.id.user_name);
        mUserState = (TextView) findViewById(R.id.user_state);
        mLogoutButton = (Button) findViewById(R.id.logout_button);
        mShowNotifications = (Switch) findViewById(R.id.show_notifications_switch);
        mVerboseLogging = (Switch) findViewById(R.id.logging_switch);
        mAppVersion = (TextView) findViewById(R.id.app_version);
        mAtlasVersion = (TextView) findViewById(R.id.atlas_version);
        mLayerVersion = (TextView) findViewById(R.id.layer_version);
        mAndroidVersion = (TextView) findViewById(R.id.android_version);
        mUserId = (TextView) findViewById(R.id.user_id);
        mConversationCount = (TextView) findViewById(R.id.conversation_count);
        mMessageCount = (TextView) findViewById(R.id.message_count);
        mUnreadMessageCount = (TextView) findViewById(R.id.unread_message_count);
        mDiskUtilization = (TextView) findViewById(R.id.disk_utilization);
        mDiskAllowance = (TextView) findViewById(R.id.disk_allowance);
        mAutoDownloadMimeTypes = (TextView) findViewById(R.id.auto_download_mime_types);
        mAvatar.init(getParticipantProvider(), getPicasso());

        // Long-click copy-to-clipboard
        mUserName.setOnLongClickListener(this);
        mUserState.setOnLongClickListener(this);
        mAppVersion.setOnLongClickListener(this);
        mAndroidVersion.setOnLongClickListener(this);
        mAtlasVersion.setOnLongClickListener(this);
        mLayerVersion.setOnLongClickListener(this);
        mUserId.setOnLongClickListener(this);
        mConversationCount.setOnLongClickListener(this);
        mMessageCount.setOnLongClickListener(this);
        mUnreadMessageCount.setOnLongClickListener(this);
        mDiskUtilization.setOnLongClickListener(this);
        mDiskAllowance.setOnLongClickListener(this);
        mAutoDownloadMimeTypes.setOnLongClickListener(this);

        // Buttons and switches
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                setEnabled(false);
                new AlertDialog.Builder(AppSettingsActivity.this)
                        .setCancelable(false)
                        .setMessage(R.string.alert_message_logout)
                        .setPositiveButton(R.string.alert_button_logout, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Log.isLoggable(Log.VERBOSE)) {
                                    Log.v("Deauthenticating");
                                }
                                dialog.dismiss();
                                final ProgressDialog progressDialog = new ProgressDialog(AppSettingsActivity.this);
                                progressDialog.setMessage(getResources().getString(R.string.alert_dialog_logout));
                                progressDialog.setCancelable(false);
                                progressDialog.show();
                                App.deauthenticate(new Util.DeauthenticationCallback() {
                                    @Override
                                    public void onDeauthenticationSuccess(LayerClient client) {
                                        if (Log.isLoggable(Log.VERBOSE)) {
                                            Log.v("Successfully deauthenticated");
                                        }
                                        progressDialog.dismiss();
                                        setEnabled(true);
                                        App.routeLogin(AppSettingsActivity.this);
                                    }

                                    @Override
                                    public void onDeauthenticationFailed(LayerClient client, String reason) {
                                        if (Log.isLoggable(Log.ERROR)) {
                                            Log.e("Failed to deauthenticate: " + reason);
                                        }
                                        progressDialog.dismiss();
                                        setEnabled(true);
                                        Toast.makeText(AppSettingsActivity.this, getString(R.string.toast_failed_to_deauthenticate, reason), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                setEnabled(true);
                            }
                        })
                        .show();
            }
        });

        mShowNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PushNotificationReceiver.getNotifications(AppSettingsActivity.this).setEnabled(isChecked);
            }
        });

        mVerboseLogging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LayerClient.setLoggingEnabled(AppSettingsActivity.this, isChecked);
                com.layer.atlas.util.Log.setAlwaysLoggable(isChecked);
                Log.setAlwaysLoggable(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLayerClient()
                .registerAuthenticationListener(this)
                .registerConnectionListener(this)
                .registerEventListener(this);
        refresh();
    }

    @Override
    protected void onPause() {
        getLayerClient()
                .unregisterAuthenticationListener(this)
                .unregisterConnectionListener(this)
                .unregisterEventListener(this);
        super.onPause();
    }

    public void setEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogoutButton.setEnabled(enabled);
                mShowNotifications.setEnabled(enabled);
                mVerboseLogging.setEnabled(enabled);
            }
        });
    }

    private void refresh() {
        if (!getLayerClient().isAuthenticated()) return;

        /* Account */
        Participant participant = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId());
        mAvatar.setParticipants(getLayerClient().getAuthenticatedUserId());
        mUserName.setText(participant.getName());
        mUserState.setText(getLayerClient().isConnected() ? R.string.settings_content_connected : R.string.settings_content_disconnected);

        /* Notifications */
        mShowNotifications.setChecked(PushNotificationReceiver.getNotifications(this).isEnabled());

        /* Debug */
        // enable logging through adb: `adb shell setprop log.tag.LayerSDK VERBOSE`
        boolean enabledByEnvironment = android.util.Log.isLoggable("LayerSDK", Log.VERBOSE);
        mVerboseLogging.setEnabled(!enabledByEnvironment);
        mVerboseLogging.setChecked(enabledByEnvironment || LayerClient.isLoggingEnabled());
        mAppVersion.setText(getString(R.string.settings_content_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        mAtlasVersion.setText(Util.getVersion());
        mLayerVersion.setText(LayerClient.getVersion());
        mAndroidVersion.setText(getString(R.string.settings_content_android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        mUserId.setText(getLayerClient().getAuthenticatedUserId());
        
        /* Statistics */
        long totalMessages = 0;
        long totalUnread = 0;
        List<Conversation> conversations = getLayerClient().getConversations();
        for (Conversation conversation : conversations) {
            totalMessages += conversation.getTotalMessageCount();
            totalUnread += conversation.getTotalUnreadMessageCount();
        }
        mConversationCount.setText(String.format("%d", conversations.size()));
        mMessageCount.setText(String.format("%d", totalMessages));
        mUnreadMessageCount.setText(String.format("%d", totalUnread));

        /* Rich Content */
        mDiskUtilization.setText(readableByteFormat(getLayerClient().getDiskUtilization()));
        long allowance = getLayerClient().getDiskCapacity();
        if (allowance == 0) {
            mDiskAllowance.setText(R.string.settings_content_disk_unlimited);
        } else {
            mDiskAllowance.setText(readableByteFormat(allowance));
        }
        mAutoDownloadMimeTypes.setText(TextUtils.join("\n", getLayerClient().getAutoDownloadMimeTypes()));
    }

    private String readableByteFormat(long bytes) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        double value;
        int suffix;
        if (bytes >= gb) {
            value = (double) bytes / (double) gb;
            suffix = R.string.settings_content_disk_gb;
        } else if (bytes >= mb) {
            value = (double) bytes / (double) mb;
            suffix = R.string.settings_content_disk_mb;
        } else if (bytes >= kb) {
            value = (double) bytes / (double) kb;
            suffix = R.string.settings_content_disk_kb;
        } else {
            value = (double) bytes;
            suffix = R.string.settings_content_disk_b;
        }
        return getString(R.string.settings_content_disk_usage, value, getString(suffix));
    }


    @Override
    public void onAuthenticated(LayerClient layerClient, String s) {
        refresh();
    }

    @Override
    public void onDeauthenticated(LayerClient layerClient) {
        refresh();
    }

    @Override
    public void onAuthenticationChallenge(LayerClient layerClient, String s) {

    }

    @Override
    public void onAuthenticationError(LayerClient layerClient, LayerException e) {

    }

    @Override
    public void onConnectionConnected(LayerClient layerClient) {
        refresh();
    }

    @Override
    public void onConnectionDisconnected(LayerClient layerClient) {
        refresh();
    }

    @Override
    public void onConnectionError(LayerClient layerClient, LayerException e) {

    }

    @Override
    public void onChangeEvent(LayerChangeEvent layerChangeEvent) {
        refresh();
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof TextView) {
            Util.copyToClipboard(v.getContext(), R.string.settings_clipboard_description, ((TextView) v).getText().toString());
            Toast.makeText(this, R.string.toast_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
