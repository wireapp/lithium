package com.wire.lithium.server;

import com.google.protobuf.ByteString;
import com.waz.model.Messages;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.GenericMessageProcessor;
import com.wire.xenon.models.AudioMessage;
import com.wire.xenon.models.LinkPreviewMessage;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class GenericMessageProcessorTest {

    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    private static final String URL = "https://wire.com";
    private static final String CONTENT = "This is https://wire.com";
    private static final int URL_OFFSET = 8;
    private static final String ASSET_KEY = "key";
    private static final String ASSET_TOKEN = "token";
    private static final int HEIGHT = 43;
    private static final int WIDTH = 84;
    private static final int SIZE = 123;
    private static final String MIME_TYPE = "image/png";
    public static final String AUDIO_MIME_TYPE = "audio/x-m4a";
    public static final String NAME = "audio.m4a";
    public static final int DURATION = 27000;

    @Test
    public void testLinkPreview() {
        MessageHandler handler = new MessageHandler();
        GenericMessageProcessor processor = new GenericMessageProcessor(null, handler);

        UUID from = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        String sender = "";
        String time = "";
        UUID messageId = UUID.randomUUID();

        Messages.Asset.ImageMetaData.Builder image = Messages.Asset.ImageMetaData.newBuilder()
                .setHeight(HEIGHT)
                .setWidth(WIDTH);

        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(SIZE)
                .setMimeType(MIME_TYPE)
                .setImage(image);

        Messages.Asset.RemoteData.Builder uploaded = Messages.Asset.RemoteData.newBuilder()
                .setAssetId(ASSET_KEY)
                .setAssetToken(ASSET_TOKEN)
                .setOtrKey(ByteString.EMPTY)
                .setSha256(ByteString.EMPTY);

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setOriginal(original)
                .setUploaded(uploaded);

        Messages.LinkPreview.Builder linkPreview = Messages.LinkPreview.newBuilder()
                .setTitle(TITLE)
                .setSummary(SUMMARY)
                .setUrl(URL)
                .setUrlOffset(URL_OFFSET)
                .setImage(asset);

        Messages.Text.Builder text = Messages.Text.newBuilder()
                .setContent(CONTENT)
                .addLinkPreview(linkPreview);

        Messages.GenericMessage.Builder builder = Messages.GenericMessage.newBuilder()
                .setMessageId(messageId.toString())
                .setText(text);

        processor.process(from, sender, convId, time, builder.build());
    }

    @Test
    public void testAudio() throws UnsupportedEncodingException {
        MessageHandler handler = new MessageHandler();
        GenericMessageProcessor processor = new GenericMessageProcessor(null, handler);

        UUID from = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        String sender = "sender";
        String time = "some time";
        UUID messageId = UUID.randomUUID();

        Messages.Asset.AudioMetaData.Builder audioMeta = Messages.Asset.AudioMetaData.newBuilder()
                .setDurationInMillis(DURATION)
                .setNormalizedLoudness(ByteString.copyFrom("231241534534sdSCCKENHWUXNasdaeqdAnkjBckjwenfjwef", "UTF-8"));

        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(SIZE)
                .setName(NAME)
                .setMimeType(AUDIO_MIME_TYPE)
                .setAudio(audioMeta);

        Messages.Asset.RemoteData.Builder uploaded = Messages.Asset.RemoteData.newBuilder()
                .setAssetId(ASSET_KEY)
                .setAssetToken(ASSET_TOKEN)
                .setOtrKey(ByteString.EMPTY)
                .setSha256(ByteString.EMPTY);

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setOriginal(original)
                .setUploaded(uploaded);

        Messages.GenericMessage.Builder builder = Messages.GenericMessage.newBuilder()
                .setMessageId(messageId.toString())
                .setAsset(asset);

        processor.process(from, sender, convId, time, builder.build());
    }

    private static class MessageHandler extends MessageHandlerBase {
        @Override
        public void onLinkPreview(WireClient client, LinkPreviewMessage msg) {
            assert msg.getTitle().equals(TITLE);
            assert msg.getSummary().equals(SUMMARY);
            assert msg.getUrl().equals(URL);
            assert msg.getText().equals(CONTENT);
            assert msg.getUrlOffset() == URL_OFFSET;

            assert msg.getAssetKey().equals(ASSET_KEY);
            assert msg.getWidth() == WIDTH;
            assert msg.getHeight() == HEIGHT;
            assert msg.getSize() == SIZE;
            assert msg.getMimeType().equals(MIME_TYPE);
            assert msg.getAssetToken().equals(ASSET_TOKEN);
        }

        @Override
        public void onAudio(WireClient client, AudioMessage msg) {
            assert msg.getMimeType().equals(AUDIO_MIME_TYPE);
        }
    }
}
