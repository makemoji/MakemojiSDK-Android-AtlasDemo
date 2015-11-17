package com.layer.messenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.utilities.Utils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerConnectionListener;
import com.layer.sdk.messaging.Conversation;

import java.util.List;
import java.util.Locale;

public class SettingsActivity extends BaseActivity implements LayerConnectionListener, LayerAuthenticationListener, LayerChangeEventListener, View.OnLongClickListener {
    private static final String TAG = SettingsActivity.class.getSimpleName();

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

    public SettingsActivity() {
        super(R.layout.activity_settings, R.menu.menu_settings, R.string.title_settings, true);
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
        mAndroidVersion = (TextView) findViewById(R.id.android_version);
        mAtlasVersion = (TextView) findViewById(R.id.atlas_version);
        mLayerVersion = (TextView) findViewById(R.id.layer_version);
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
                new AlertDialog.Builder(SettingsActivity.this)
                        .setCancelable(false)
                        .setMessage(R.string.alert_message_logout)
                        .setPositiveButton(R.string.alert_button_logout, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                final ProgressDialog progressDialog = new ProgressDialog(SettingsActivity.this);
                                progressDialog.setMessage(getResources().getString(R.string.logout_dialog_message));
                                progressDialog.setCancelable(false);
                                progressDialog.show();
                                App.deauthenticate(new Utils.DeauthenticationCallback() {
                                    @Override
                                    public void onDeauthenticationSuccess(LayerClient client) {
                                        progressDialog.dismiss();
                                        setEnabled(true);
                                        App.routeLogin(SettingsActivity.this);
                                    }

                                    @Override
                                    public void onDeauthenticationFailed(LayerClient client, String reason) {
                                        progressDialog.dismiss();
                                        setEnabled(true);
                                        Toast.makeText(SettingsActivity.this, "Failed to deauthenticate: " + reason, Toast.LENGTH_SHORT).show();
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
                PushNotificationReceiver.getNotifications(SettingsActivity.this).setEnabled(isChecked);
            }
        });

        mVerboseLogging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LayerClient.setLoggingEnabled(SettingsActivity.this, isChecked);
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
        mUserState.setText(getLayerClient().isConnected() ? "Connected" : "Disconnected");

        /* Notifications */
        mShowNotifications.setChecked(PushNotificationReceiver.getNotifications(this).isEnabled());

        /* Debug */
        // enable logging through adb: `adb shell setprop log.tag.LayerSDK VERBOSE`
        boolean enabledByEnvironment = Log.isLoggable("LayerSDK", Log.VERBOSE);
        mVerboseLogging.setEnabled(!enabledByEnvironment);
        mVerboseLogging.setChecked(enabledByEnvironment ? true : LayerClient.isLoggingEnabled());
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mAppVersion.setText("Name " + pInfo.versionName + " / Code " + pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        mAndroidVersion.setText("Version " + Build.VERSION.RELEASE + " / API level" + Build.VERSION.SDK_INT);
        mAtlasVersion.setText(Utils.getVersion());
        mLayerVersion.setText(LayerClient.getVersion());
        mUserId.setText(getLayerClient().getAuthenticatedUserId());
        
        /* Statistics */
        long totalMessages = 0;
        long totalUnread = 0;
        List<Conversation> conversations = getLayerClient().getConversations();
        for (Conversation conversation : conversations) {
            totalMessages += conversation.getTotalMessageCount();
            totalUnread += conversation.getTotalUnreadMessageCount();
        }
        mConversationCount.setText(Integer.toString(conversations.size()));
        mMessageCount.setText(Long.toString(totalMessages));
        mUnreadMessageCount.setText(Long.toString(totalUnread));

        /* Rich Content */
        mDiskUtilization.setText(readableByteFormat(getLayerClient().getDiskUtilization()));
        long allowance = getLayerClient().getDiskCapacity();
        mDiskAllowance.setText(allowance == 0 ? "Unlimited" : readableByteFormat(allowance));
        mAutoDownloadMimeTypes.setText(TextUtils.join("\n", getLayerClient().getAutoDownloadMimeTypes()));
    }

    private String readableByteFormat(long bytes) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        double value;
        String suffix;
        if (bytes >= gb) {
            value = (double) bytes / (double) gb;
            suffix = "GB";
        } else if (bytes >= mb) {
            value = (double) bytes / (double) mb;
            suffix = "MB";
        } else if (bytes >= kb) {
            value = (double) bytes / (double) kb;
            suffix = "KB";
        } else {
            value = (double) bytes;
            suffix = "B";
        }
        return String.format(Locale.US, "%.2f %s", value, suffix);
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
            Utils.copyToClipboard(v.getContext(), "Settings", ((TextView) v).getText().toString());
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
