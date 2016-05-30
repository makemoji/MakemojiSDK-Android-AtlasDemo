package com.layer.messenger.makemoji;

/**
 * A copy paster of the atlas composer to accomodate the MojiEditText component.
 * The send click listener has been changed.
 * Created by s_baar on 5/30/2016.
 */
/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.layer.atlas.messagetypes.AttachmentSender;
import com.layer.atlas.messagetypes.MessageSender;
import com.layer.atlas.messagetypes.text.TextSender;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.EditTextUtil;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiInputLayout;

import java.util.ArrayList;


public class MakeMojiAtlasComposer extends FrameLayout {
    private EditText mMessageEditText;
    private Button mSendButton;
    private ImageView mAttachButton;

    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;
    private Conversation mConversation;

    private TextSender mTextSender;
    private ArrayList<AttachmentSender> mAttachmentSenders = new ArrayList<AttachmentSender>();
    private MessageSender.Callback mMessageSenderCallback;

    private PopupWindow mAttachmentMenu;

    // styles
    private boolean mEnabled;
    private int mTextColor;
    private float mTextSize;
    private Typeface mTypeFace;
    private int mTextStyle;
    private int mUnderlineColor;
    private int mCursorColor;
    private Drawable mAttachmentSendersBackground;

    public MakeMojiAtlasComposer(Context context) {
        super(context);
        initAttachmentMenu(context, null, 0);
    }

    public MakeMojiAtlasComposer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MakeMojiAtlasComposer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
        initAttachmentMenu(context, attrs, defStyle);
    }

    /**
     * Prepares this AtlasMessageComposer for use.
     *
     * @return this AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer init(LayerClient layerClient, ParticipantProvider participantProvider) {
        LayoutInflater.from(getContext()).inflate(com.layer.atlas.R.layout.atlas_message_composer, this);

        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;

        mAttachButton = (ImageView) findViewById(com.layer.atlas.R.id.attachment);
        mAttachButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LinearLayout menu = (LinearLayout) mAttachmentMenu.getContentView();
                menu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                mAttachmentMenu.showAsDropDown(v, 0, -menu.getMeasuredHeight() - v.getHeight());
            }
        });

        mMessageEditText = (EditText) findViewById(com.layer.atlas.R.id.message_edit_text);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mConversation == null || mConversation.isDeleted()) return;
                if (s.length() > 0) {
                    mSendButton.setEnabled(isEnabled());
                    mConversation.send(LayerTypingIndicatorListener.TypingIndicator.STARTED);
                } else {
                    mSendButton.setEnabled(false);
                    mConversation.send(LayerTypingIndicatorListener.TypingIndicator.FINISHED);
                }
            }
        });

        mSendButton = (Button) findViewById(com.layer.atlas.R.id.send_button);
        final MojiInputLayout analytics = new MojiInputLayout(getContext());
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!mTextSender.requestSend(Moji.toHtml(mMessageEditText.getText()))) return;
                analytics.setInputText(mMessageEditText.getText());
                analytics.manualSaveInputToRecentsAndBackend();
                mMessageEditText.setText("");
                mSendButton.setEnabled(false);
            }
        });
        applyStyle();
        return this;
    }

    /**
     * Sets the Conversation used for sending Messages.
     *
     * @param conversation the Conversation used for sending Messages.
     * @return This AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer setConversation(Conversation conversation) {
        mConversation = conversation;
        if (mTextSender != null) mTextSender.setConversation(conversation);
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.setConversation(conversation);
        }
        return this;
    }

    /**
     * Sets a listener for receiving the message EditText focus change callbacks.
     *
     * @param listener Listener for receiving the message EditText focus change callbacks.
     * @return This AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer setOnMessageEditTextFocusChangeListener(OnFocusChangeListener listener) {
        mMessageEditText.setOnFocusChangeListener(listener);
        return this;
    }

    /**
     * Sets the TextSender used for sending composed text messages.
     *
     * @param textSender TextSender used for sending composed text messages.
     * @return This AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer setTextSender(TextSender textSender) {
        mTextSender = textSender;
        mTextSender.init(this.getContext().getApplicationContext(), mLayerClient, mParticipantProvider);
        mTextSender.setConversation(mConversation);
        if (mMessageSenderCallback != null) mTextSender.setCallback(mMessageSenderCallback);
        return this;
    }

    /**
     * Adds AttachmentSenders to this AtlasMessageComposer's attachment menu.
     *
     * @param senders AttachmentSenders to add to this AtlasMessageComposer's attachment menu.
     * @return This AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer addAttachmentSenders(AttachmentSender... senders) {
        for (AttachmentSender sender : senders) {
            if (sender.getTitle() == null && sender.getIcon() == null) {
                throw new NullPointerException("Attachment handlers must have at least a title or icon specified.");
            }
            sender.init(this.getContext().getApplicationContext(), mLayerClient, mParticipantProvider);
            sender.setConversation(mConversation);
            if (mMessageSenderCallback != null) sender.setCallback(mMessageSenderCallback);
            mAttachmentSenders.add(sender);
            addAttachmentMenuItem(sender);
        }
        if (!mAttachmentSenders.isEmpty()) mAttachButton.setVisibility(View.VISIBLE);
        return this;
    }

    /**
     * Sets an optional callback for receiving MessageSender events.  If non-null, overrides any
     * callbacks already set on MessageSenders.
     *
     * @param callback Callback to receive MessageSender events.
     * @return This AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer setMessageSenderCallback(MessageSender.Callback callback) {
        mMessageSenderCallback = callback;
        if (mMessageSenderCallback == null) return this;
        if (mTextSender != null) mTextSender.setCallback(callback);
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.setCallback(callback);
        }
        return this;
    }

    public MakeMojiAtlasComposer setTypeface(Typeface typeface) {
        this.mTypeFace = typeface;
        applyTypeface();
        return this;
    }

    /**
     * Must be called from Activity's onActivityResult to allow attachment senders to manage results
     * from e.g. selecting a gallery photo or taking a camera image.
     *
     * @param activity    Activity receiving the result.
     * @param requestCode Request code from the Activity's onActivityResult.
     * @param resultCode  Result code from the Activity's onActivityResult.
     * @param data        Intent data from the Activity's onActivityResult.
     * @return this AtlasMessageComposer.
     */
    public MakeMojiAtlasComposer onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.onActivityResult(activity, requestCode, resultCode, data);
        }
        return this;
    }

    /**
     * Must be called from Activity's onRequestPermissionsResult to allow attachment senders to
     * manage dynamic permisttions.
     *
     * @param requestCode  The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mAttachButton != null) mAttachButton.setEnabled(enabled);
        if (mMessageEditText != null) mMessageEditText.setEnabled(enabled);
        if (mSendButton != null) {
            mSendButton.setEnabled(enabled && (mMessageEditText != null) && (mMessageEditText.getText().length() > 0));
        }
        super.setEnabled(enabled);
    }

    private void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, com.layer.atlas.R.styleable.AtlasMessageComposer, com.layer.atlas.R.attr.AtlasMessageComposer, defStyle);
        mEnabled = ta.getBoolean(com.layer.atlas.R.styleable.AtlasMessageComposer_android_enabled, true);
        this.mTextColor = ta.getColor(com.layer.atlas.R.styleable.AtlasMessageComposer_inputTextColor, context.getResources().getColor(com.layer.atlas.R.color.atlas_text_black));
        this.mTextSize = ta.getDimensionPixelSize(com.layer.atlas.R.styleable.AtlasMessageComposer_inputTextSize, context.getResources().getDimensionPixelSize(com.layer.atlas.R.dimen.atlas_text_size_input));
        this.mTextStyle = ta.getInt(com.layer.atlas.R.styleable.AtlasMessageComposer_inputTextStyle, Typeface.NORMAL);
        String typeFaceName = ta.getString(com.layer.atlas.R.styleable.AtlasMessageComposer_inputTextTypeface);
        this.mTypeFace = typeFaceName != null ? Typeface.create(typeFaceName, mTextStyle) : null;
        this.mUnderlineColor = ta.getColor(com.layer.atlas.R.styleable.AtlasMessageComposer_inputUnderlineColor, context.getResources().getColor(com.layer.atlas.R.color.atlas_color_primary_blue));
        this.mCursorColor = ta.getColor(com.layer.atlas.R.styleable.AtlasMessageComposer_inputCursorColor, context.getResources().getColor(com.layer.atlas.R.color.atlas_color_primary_blue));
        this.mAttachmentSendersBackground = ta.getDrawable(com.layer.atlas.R.styleable.AtlasMessageComposer_attachmentSendersBackground);
        ta.recycle();
    }

    private void applyStyle() {
        setEnabled(mEnabled);

        mMessageEditText.setTextColor(mTextColor);
        mMessageEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        EditTextUtil.setCursorDrawableColor(mMessageEditText, mCursorColor);
        EditTextUtil.setUnderlineColor(mMessageEditText, mUnderlineColor);
        applyTypeface();

        ColorStateList list = getResources().getColorStateList(com.layer.atlas.R.color.atlas_message_composer_attach_button);
        Drawable d = DrawableCompat.wrap(mAttachButton.getDrawable().mutate());
        DrawableCompat.setTintList(d, list);
        mAttachButton.setImageDrawable(d);
    }

    private void applyTypeface() {
        mMessageEditText.setTypeface(mTypeFace, mTextStyle);
    }

    private void addAttachmentMenuItem(AttachmentSender sender) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout menuLayout = (LinearLayout) mAttachmentMenu.getContentView();

        View menuItem = inflater.inflate(com.layer.atlas.R.layout.atlas_message_composer_attachment_menu_item, menuLayout, false);
        ((TextView) menuItem.findViewById(com.layer.atlas.R.id.title)).setText(sender.getTitle());
        menuItem.setTag(sender);
        menuItem.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mAttachmentMenu.dismiss();
                ((AttachmentSender) v.getTag()).requestSend();
            }
        });
        if (sender.getIcon() != null) {
            ImageView iconView = ((ImageView) menuItem.findViewById(com.layer.atlas.R.id.icon));
            iconView.setImageResource(sender.getIcon());
            iconView.setVisibility(VISIBLE);
            Drawable d = DrawableCompat.wrap(iconView.getDrawable());
            DrawableCompat.setTint(d, getResources().getColor(com.layer.atlas.R.color.atlas_icon_enabled));
        }
        menuLayout.addView(menuItem);
    }

    private void initAttachmentMenu(Context context, AttributeSet attrs, int defStyle) {
        if (mAttachmentMenu != null) throw new IllegalStateException("Already initialized menu");

        if (attrs == null) {
            mAttachmentMenu = new PopupWindow(context);
        } else {
            mAttachmentMenu = new PopupWindow(context, attrs, defStyle);
        }
        mAttachmentMenu.setContentView(LayoutInflater.from(context).inflate(com.layer.atlas.R.layout.atlas_message_composer_attachment_menu, null));
        mAttachmentMenu.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mAttachmentMenu.setOutsideTouchable(true);
        mAttachmentMenu.setBackgroundDrawable(mAttachmentSendersBackground);
        mAttachmentMenu.setFocusable(true);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (mAttachmentSenders.isEmpty()) return superState;
        SavedState savedState = new SavedState(superState);
        for (AttachmentSender sender : mAttachmentSenders) {
            Parcelable parcelable = sender.onSaveInstanceState();
            if (parcelable == null) continue;
            savedState.put(sender.getClass(), parcelable);
        }
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        for (AttachmentSender sender : mAttachmentSenders) {
            Parcelable parcelable = savedState.get(sender.getClass());
            if (parcelable == null) continue;
            sender.onRestoreInstanceState(parcelable);
        }
    }

    /**
     * Saves a map from AttachmentSender class to AttachmentSender saved instance.
     */
    private static class SavedState extends BaseSavedState {
        Bundle mBundle = new Bundle();

        public SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState put(Class<? extends AttachmentSender> cls, Parcelable parcelable) {
            mBundle.putParcelable(cls.getName(), parcelable);
            return this;
        }

        Parcelable get(Class<? extends AttachmentSender> cls) {
            return mBundle.getParcelable(cls.getName());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(mBundle);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            mBundle = in.readBundle();
        }
    }
}
