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
import com.wire.bots.sdk.models.*;

/**
 */
public class GenericMessageProcessor {
    private final WireClient client;
    private final MessageHandlerBase handler;

    public GenericMessageProcessor(WireClient client, MessageHandlerBase handler) {
        this.client = client;
        this.handler = handler;
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

        // Ephemeral messages
        if (generic.hasEphemeral()) {
            Messages.Ephemeral ephemeral = generic.getEphemeral();
            if (ephemeral.hasText()) {
                text = ephemeral.getText();
            }

            if (ephemeral.hasAsset()) {
                asset = ephemeral.getAsset();
            }
        }

        // Edit message
        if (generic.hasEdited() && generic.getEdited().hasText()) {
            Messages.MessageEdit edited = generic.getEdited();
            TextMessage msg = new TextMessage(edited.getReplacingMessageId(), convId, clientId, userId);
            msg.setText(edited.getText().getContent());

            handler.onEditText(client, msg);
            return true;
        }

        // Text
        if (text != null && text.hasContent() && text.getLinkPreviewList().isEmpty()) {
            TextMessage msg = new TextMessage(messageId, convId, clientId, userId);
            msg.setText(text.getContent());

            handler.onText(client, msg);
            return true;
        }

        if (generic.hasCalling()) {
            Messages.Calling calling = generic.getCalling();
            if (calling.hasContent()) {
                String content = calling.getContent();
                handler.onCalling(client, userId, clientId, content);
            }
            return true;
        }

        //Logger.info("Generic: hasAsset: %s, hasImage: %s", generic.hasAsset(), generic.hasImage());

        // Assets
        if (asset != null) {
            //Logger.info("Generic: hasOriginal: %s, hasUploaded: %s", asset.hasOriginal(), asset.hasUploaded());

            if (asset.hasOriginal()) {
                Messages.Asset.Original original = asset.getOriginal();
                //Logger.info("Generic: hasAudio: %s, hasVideo: %s", original.hasAudio(), original.hasVideo());

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

                    if (msg.getAssetKey() != null && !msg.getAssetKey().isEmpty())
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
}
