//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk.server;

import com.waz.model.Messages;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.AudioMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.MessageAssetBase;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.models.VideoMessage;

/**
 */
public class GenericMessageProcessor {
    private final WireClient client;
    private final MessageHandlerBase handler;

    public GenericMessageProcessor(WireClient client, MessageHandlerBase handler) {
        this.client = client;
        this.handler = handler;
    }

    public boolean process(String userId, Messages.GenericMessage generic) {
        String messageId = generic.getMessageId();
        String convId = client.getConversationId();
        String clientId = client.getDeviceId();

        Messages.Text text = null;
        Messages.Asset asset = null;

        // Text
        if (generic.hasText()) {
            text = generic.getText();
        }

        // Assets
        if (generic.hasAsset()) {
            asset = generic.getAsset();
        }

        if (generic.hasEphemeral()) {
            if (generic.getEphemeral().hasText()) {
                text = generic.getEphemeral().getText();
            }

            if (generic.getEphemeral().hasAsset()) {
                asset = generic.getEphemeral().getAsset();
            }
        }

        // Text
        if (text != null && text.hasContent() && text.getLinkPreviewList().isEmpty()) {
            TextMessage msg = new TextMessage(messageId, convId, clientId, userId);
            msg.setText(text.getContent());

            handler.onText(client, msg);
            return true;
        }

        // Assets
        if (asset != null) {
            if (asset.hasOriginal()) {
                Messages.Asset.Original original = asset.getOriginal();
                if (original.hasImage()) {
                    ImageMessage msg = new ImageMessage(messageId, convId, clientId, userId);

                    initAsset(asset, original, msg);

                    Messages.Asset.ImageMetaData image = original.getImage();
                    msg.setHeight(image.getHeight());
                    msg.setWidth(image.getWidth());
                    msg.setTag(image.hasTag() ? image.getTag() : null);

                    handler.onImage(client, msg);
                    return true;
                }
                if (original.hasAudio()) {
                    AudioMessage msg = new AudioMessage(messageId, convId, clientId, userId);

                    initAsset(asset, original, msg);

                    Messages.Asset.AudioMetaData audio = original.getAudio();
                    msg.setDuration(audio.getDurationInMillis());

                    if (msg.getAssetKey() != null && !msg.getAssetKey().isEmpty())
                        handler.onAudio(client, msg);

                    return true;
                }
                if (original.hasVideo()) {
                    VideoMessage msg = new VideoMessage(messageId, convId, clientId, userId);

                    initAsset(asset, original, msg);

                    Messages.Asset.VideoMetaData video = original.getVideo();
                    msg.setDuration(video.getDurationInMillis());
                    msg.setHeight(video.getHeight());
                    msg.setWidth(video.getWidth());

                    handler.onVideo(client, msg);
                    return true;
                }

                {
                    // this must be a generic file attachment then
                    AttachmentMessage msg = new AttachmentMessage(messageId, convId, clientId, userId);

                    initAsset(asset, original, msg);

                    if (msg.getAssetKey() != null && !msg.getAssetKey().isEmpty())
                        handler.onAttachment(client, msg);
                    return true;
                }
            }
        }

        return false;
    }

    @Deprecated
    public boolean process(String userId, String assetId, Messages.GenericMessage generic) {
        String messageId = generic.getMessageId();
        String convId = client.getConversationId();
        String clientId = client.getDeviceId();

        Messages.ImageAsset image = generic.hasImage() ? generic.getImage() : null;
        if (image != null && image.getTag().startsWith("medium")) {
            ImageMessage msg = new ImageMessage(messageId, convId, clientId, userId);

            msg.setAssetKey(assetId);
            msg.setOtrKey(image.getOtrKey().toByteArray());
            msg.setMimeType(image.getMimeType());
            msg.setSize(image.getSize());
            msg.setSha256(image.getSha256().toByteArray());
            msg.setHeight(image.getHeight());
            msg.setWidth(image.getWidth());
            msg.setTag(image.hasTag() ? image.getTag() : null);

            handler.onImage(client, msg);
            return true;
        }

        Messages.Asset asset = generic.hasAsset() ? generic.getAsset() : null;
        if (asset != null) {
            if (asset.hasOriginal()) {
                Messages.Asset.Original original = asset.getOriginal();
                if (original.hasAudio()) {
                    AudioMessage msg = new AudioMessage(messageId, convId, clientId, userId);

                    initAsset(asset, original, msg);

                    msg.setAssetKey(assetId);

                    Messages.Asset.AudioMetaData audio = original.getAudio();
                    msg.setDuration(audio.getDurationInMillis());

                    handler.onAudio(client, msg);
                    return true;
                }
                if (original.hasVideo()) {
                    VideoMessage msg = new VideoMessage(messageId, convId, clientId, userId);

                    initAsset(asset, original, msg);

                    msg.setAssetKey(assetId);

                    Messages.Asset.VideoMetaData video = original.getVideo();
                    msg.setDuration(video.getDurationInMillis());
                    msg.setHeight(video.getHeight());
                    msg.setWidth(video.getWidth());

                    handler.onVideo(client, msg);
                    return true;
                }
            }

            if (asset.hasUploaded()) {
                Messages.Asset.RemoteData uploaded = asset.getUploaded();
                // this must be a generic file attachment then
                AttachmentMessage msg = new AttachmentMessage(messageId, convId, clientId, userId);

                msg.setAssetKey(uploaded.getAssetId());
                msg.setAssetToken(uploaded.hasAssetToken() ? uploaded.getAssetToken() : null);
                msg.setOtrKey(uploaded.getOtrKey().toByteArray());
                msg.setSha256(uploaded.getSha256().toByteArray());
                msg.setAssetKey(assetId);

                handler.onAttachment(client, msg);
                return true;
            }
        }

        return false;
    }

    private static void initAsset(Messages.Asset asset, Messages.Asset.Original original, MessageAssetBase msg) {
        msg.setMimeType(original.getMimeType());
        msg.setSize(original.getSize());
        msg.setName(original.hasName() ? original.getName() : null);

        if (asset.hasUploaded()) {
            Messages.Asset.RemoteData uploaded = asset.getUploaded();
            msg.setAssetKey(uploaded.getAssetId());
            msg.setAssetToken(uploaded.hasAssetToken() ? uploaded.getAssetToken() : null);
            msg.setOtrKey(uploaded.getOtrKey().toByteArray());
            msg.setSha256(uploaded.getSha256().toByteArray());
        }
    }
}
