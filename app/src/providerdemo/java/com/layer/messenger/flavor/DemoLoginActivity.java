package com.layer.messenger.flavor;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.messenger.App;
import com.layer.messenger.ConversationsListActivity;
import com.layer.messenger.R;
import com.layer.messenger.util.AuthenticationProvider;
import com.layer.messenger.util.Log;

public class DemoLoginActivity extends AppCompatActivity {
    EditText mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_demo);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        mName = (EditText) findViewById(R.id.name);
        mName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    final String name = mName.getText().toString().trim();
                    if (name.isEmpty()) return true;
                    login(name);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mName.setEnabled(true);
    }

    private void login(final String name) {
        mName.setEnabled(false);
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.login_dialog_message));
        progressDialog.show();
        App.authenticate(new DemoAuthenticationProvider.Credentials(App.getLayerAppId(), name),
                new AuthenticationProvider.Callback() {
                    @Override
                    public void onSuccess(AuthenticationProvider provider, String userId) {
                        progressDialog.dismiss();
                        if (Log.isLoggable(Log.VERBOSE)) {
                            Log.v("Successfully authenticated as `" + name + "` with userId `" + userId + "`");
                        }
                        Intent intent = new Intent(DemoLoginActivity.this, ConversationsListActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        DemoLoginActivity.this.startActivity(intent);
                    }

                    @Override
                    public void onError(AuthenticationProvider provider, final String error) {
                        progressDialog.dismiss();
                        if (Log.isLoggable(Log.ERROR)) {
                            Log.e("Failed to authenticate as `" + name + "`: " + error);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DemoLoginActivity.this, error, Toast.LENGTH_LONG).show();
                                mName.setEnabled(true);
                            }
                        });
                    }
                });
    }
}
