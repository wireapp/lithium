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
import com.wire.bots.sdk.tools.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class GenericMessageProcessor {
    private final static ConcurrentHashMap<UUID, Messages.Asset.Original> originals = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<UUID, Messages.Asset.RemoteData> remotes = new ConcurrentHashMap<>();

    private final WireClient client;
    private final MessageHandlerBase handler;

    public GenericMessageProcessor(WireClient client, MessageHandlerBase handler) {
        this.client = client;
        this.handler = handler;
    }

    private static void uploaded(MessageAssetBase msg, Messages.Asset.RemoteData uploaded) {
        msg.setAssetKey(uploaded.getAssetId());
        msg.setAssetToken(uploaded.hasAssetToken() ? uploaded.getAssetToken() : null);
        msg.setOtrKey(uploaded.getOtrKey().toByteArray());
        msg.setSha256(uploaded.getSha256().toByteArray());
    }

    private static void origin(MessageAssetBase msg, Messages.Asset.Original original) {
        msg.setMimeType(original.getMimeType());
        msg.setSize(original.getSize());
        msg.setName(original.hasName() ? original.getName() : null);
    }

    public void cleanUp(UUID messageId) {
        remotes.remove(messageId);
        originals.remove(messageId);
    }

    public boolean process(UUID from, String sender, UUID convId, String time, Messages.GenericMessage generic) {
        UUID messageId = UUID.fromString(generic.getMessageId());

        Messages.Text text = null;
        Messages.Asset asset = null;

        Logger.debug("msgId: %s hasText: %s, hasAsset: %s",
                messageId,
                generic.hasText(),
                generic.hasAsset());
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

            if (ephemeral.hasText() && ephemeral.getText().hasContent()) {
                EphemeralTextMessage msg = new EphemeralTextMessage(messageId, convId, sender, from);
                msg.setExpireAfterMillis(ephemeral.getExpireAfterMillis());
                msg.setText(ephemeral.getText().getContent());
                msg.setTime(time);

                handler.onText(client, msg);
                return true;
            }

            if (ephemeral.hasAsset()) {
                asset = ephemeral.getAsset();
            }
        }

        // Edit message
        if (generic.hasEdited() && generic.getEdited().hasText()) {
            Messages.MessageEdit edited = generic.getEdited();
            UUID replacingMessageId = UUID.fromString(edited.getReplacingMessageId());

            EditedTextMessage msg = new EditedTextMessage(messageId, convId, sender, from);
            msg.setReplacingMessageId(replacingMessageId);
            msg.setText(edited.getText().getContent());
            msg.setTime(time);

            handler.onEditText(client, msg);
            return true;
        }

        // Text
        if (text != null) {
            if (!text.getLinkPreviewList().isEmpty()) {
                for (Messages.LinkPreview link : text.getLinkPreviewList()) {
                    Messages.Asset image = link.getImage();

                    MessageAssetBase base = new MessageAssetBase(messageId, convId, sender, from);
                    origin(base, image.getOriginal());
                    uploaded(base, image.getUploaded());

                    LinkPreviewMessage msg = new LinkPreviewMessage(base, image.getOriginal().getImage());
                    msg.setTime(time);

                    msg.setSummary(link.getSummary());
                    msg.setTitle(link.getTitle());
                    msg.setUrl(link.getUrl());
                    msg.setUrlOffset(link.getUrlOffset());

                    msg.setText(text.getContent());
                    handler.onLinkPreview(client, msg);
                }
                return true;
            }

            if (text.hasContent()) {
                TextMessage msg = new TextMessage(messageId, convId, sender, from);
                msg.setText(text.getContent());
                msg.setTime(time);

                if (text.hasQuote())
                    msg.setQuotedMessageId(UUID.fromString(text.getQuote().getQuotedMessageId()));

                handler.onText(client, msg);
                return true;
            }
        }

        if (generic.hasCalling()) {
            Messages.Calling calling = generic.getCalling();
            if (calling.hasContent()) {
                CallingMessage message = new CallingMessage(messageId, convId, sender, from);
                message.setContent(calling.getContent());
                message.setTime(time);
                handler.onCalling(client, message);
            }
            return true;
        }

        if (generic.hasDeleted()) {
            DeletedTextMessage msg = new DeletedTextMessage(messageId, convId, sender, from);
            UUID delMsgId = UUID.fromString(generic.getDeleted().getMessageId());
            msg.setDeletedMessageId(delMsgId);
            msg.setTime(time);

            handler.onDelete(client, msg);
            return true;
        }

        if (generic.hasReaction()) {
            Messages.Reaction reaction = generic.getReaction();
            UUID reactionMessageId = UUID.fromString(reaction.getMessageId());
            if (reaction.hasEmoji()) {
                ReactionMessage msg = new ReactionMessage(messageId, convId, sender, from);
                msg.setEmoji(reaction.getEmoji());
                msg.setReactionMessageId(reactionMessageId);
                msg.setTime(time);

                handler.onReaction(client, msg);
                return true;
            }
        }
        // Assets
        if (asset != null) {
            Logger.debug("Asset: msgId: %s hasOriginal: %s, hasUploaded: %s",
                    messageId,
                    asset.hasOriginal(),
                    asset.hasUploaded());

            if (asset.hasUploaded()) {
                remotes.put(messageId, asset.getUploaded());
            }

            if (asset.hasOriginal()) {
                originals.put(messageId, asset.getOriginal());
            }

            Messages.Asset.Original original = originals.get(messageId);
            Messages.Asset.RemoteData remoteData = remotes.get(messageId);

            if (original != null && remoteData != null) {
                Logger.debug("Original: msgId: %s hasAudio: %s, hasVideo: %s, hasImage: %s, hasPreview %s",
                        messageId,
                        original.hasAudio(),
                        original.hasVideo(),
                        original.hasImage(),
                        asset.hasPreview());

                MessageAssetBase base = new MessageAssetBase(messageId, convId, sender, from);
                origin(base, original);
                uploaded(base, remoteData);

                if (asset.hasPreview()) {
                    Messages.Asset.Preview preview = asset.getPreview();
                    if (preview.hasRemote()) {
                        uploaded(base, preview.getRemote());

                        ImageMessage msg = new ImageMessage(base, preview.getImage());
                        msg.setTime(time);
                        msg.setSize(preview.getSize());
                        msg.setMimeType(preview.getMimeType());

                        handler.onVideoPreview(client, msg);
                    }
                }

                if (original.hasImage()) {
                    ImageMessage msg = new ImageMessage(base, original.getImage());
                    msg.setTime(time);

                    handler.onImage(client, msg);
                    return true;
                }
                if (original.hasAudio()) {
                    AudioMessage msg = new AudioMessage(base, original.getAudio());
                    msg.setTime(time);

                    handler.onAudio(client, msg);
                    return true;
                }
                if (original.hasVideo()) {
                    VideoMessage msg = new VideoMessage(base, original.getVideo());
                    msg.setTime(time);

                    handler.onVideo(client, msg);
                    return true;
                }
                {
                    AttachmentMessage msg = new AttachmentMessage(base);
                    msg.setTime(time);

                    handler.onAttachment(client, msg);
                    return true;
                }
            }
        }

        return false;
    }
}
