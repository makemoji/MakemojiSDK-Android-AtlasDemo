package com.layer.messenger.makemoji;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.messagetypes.AtlasCellFactory;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.util.Util;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Actor;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import com.makemoji.mojilib.HyperMojiListener;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.ParsedAttributes;

public class MakeMojiCellFactory extends AtlasCellFactory<MakeMojiCellFactory.CellHolder, MakeMojiCellFactory.TextInfo> implements View.OnLongClickListener {
    public final static String MIME_TYPE = "text/plain";
    HyperMojiListener hyperMojiListener;

    public MakeMojiCellFactory(HyperMojiListener hyperMojiListener) {
        super(256 * 1024);
        this.hyperMojiListener = hyperMojiListener;
    }

    public static boolean isType(Message message) {
        return message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
    }

    public static String getMessagePreview(Context context, Message message) {
        MessagePart part = message.getMessageParts().get(0);
        // For large text content, the MessagePart may not be downloaded yet.
        return part.isContentReady() ? new String(part.getData()) : "";
    }

    public static void setMessagePreview(TextView textView, Message message){
        MessagePart part = message.getMessageParts().get(0);
        String html = part.isContentReady() ? new String(part.getData()) : "";
        Moji.setText(html,textView,true);
    }
    @Override
    public boolean isBindable(Message message) {
        return MakeMojiCellFactory.isType(message);
    }

    @SuppressWarnings("ResourceAsColor")
    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        View v = layoutInflater.inflate(com.layer.atlas.R.layout.atlas_message_item_cell_text, cellView, true);
        v.setBackgroundResource(isMe ? com.layer.atlas.R.drawable.atlas_message_item_cell_me : com.layer.atlas.R.drawable.atlas_message_item_cell_them);
        ((GradientDrawable) v.getBackground()).setColor(isMe ? mMessageStyle.getMyBubbleColor() : mMessageStyle.getOtherBubbleColor());

        TextView t = (TextView) v.findViewById(com.layer.atlas.R.id.cell_text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, isMe ? mMessageStyle.getMyTextSize() : mMessageStyle.getOtherTextSize());
        t.setTextColor(isMe ? mMessageStyle.getMyTextColor() : mMessageStyle.getOtherTextColor());
        t.setLinkTextColor(isMe ? mMessageStyle.getMyTextColor() : mMessageStyle.getOtherTextColor());
        t.setTypeface(isMe ? mMessageStyle.getMyTextTypeface() : mMessageStyle.getOtherTextTypeface(), isMe ? mMessageStyle.getMyTextStyle() : mMessageStyle.getOtherTextStyle());
        return new CellHolder(v,hyperMojiListener);
    }

    @Override
    public TextInfo parseContent(LayerClient layerClient, ParticipantProvider participantProvider, Message message) {
        MessagePart part = message.getMessageParts().get(0);
        String html = part.isContentReady() ? new String(part.getData()) : "";
        ParsedAttributes parsedAttributes = Moji.parseHtml(html,null,true); //also contains size, color.
        String name;
        Actor sender = message.getSender();
        if (sender.getName() != null) {
            name = sender.getName() + ": ";
        } else {
            Participant participant = participantProvider.getParticipant(sender.getUserId());
            name = participant == null ? "" : (participant.getName() + ": ");
        }
        return new TextInfo(html,parsedAttributes.spanned, name);
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, final TextInfo parsed, Message message, CellHolderSpecs specs) {
     //   Moji.setText(parsed.getSpanned(),cellHolder.mTextView);
        MessagePart part = message.getMessageParts().get(0);
        String text = part.isContentReady() ? new String(part.getData()) : "";
        Moji.setText(text,cellHolder.mTextView,true);
        cellHolder.mTextView.setTag(parsed);
        cellHolder.mTextView.setOnLongClickListener(this);
    }

    /**
     * Long click copies message text and sender name to clipboard
     */
    @Override
    public boolean onLongClick(View v) {
        TextInfo parsed = (TextInfo) v.getTag();
        String text = parsed.getmHtml();
        Util.copyToClipboard(v.getContext(), com.layer.atlas.R.string.atlas_text_cell_factory_clipboard_description, text);
        Toast.makeText(v.getContext(), com.layer.atlas.R.string.atlas_text_cell_factory_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        return true;
    }

    public static class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view,HyperMojiListener hyperMojiListener) {
            mTextView = (TextView) view.findViewById(com.layer.atlas.R.id.cell_text);
            mTextView.setTag(com.makemoji.mojilib.R.id._makemoji_hypermoji_listener_tag_id,hyperMojiListener);
        }
    }

    public static class TextInfo implements AtlasCellFactory.ParsedContent {
        private final String mHtml;
        private final Spanned mSpanned;
        private final String mClipboardPrefix;
        private final int mSize;

        public TextInfo(String html,Spanned spanned, String clipboardPrefix) {
            mHtml = html;
            mSpanned = spanned;
            mClipboardPrefix = clipboardPrefix;
            mSize = mSpanned.toString().getBytes().length + mClipboardPrefix.getBytes().length;
        }

        public Spanned getSpanned() {
            return mSpanned;
        }

        public String getmHtml(){return mHtml;}

        public String getClipboardPrefix() {
            return mClipboardPrefix;
        }

        @Override
        public int sizeOf() {
            return mSize;
        }
    }
}
