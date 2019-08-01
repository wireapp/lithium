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

package com.wire.bots.sdk;

import com.waz.model.Messages;
import com.wire.bots.sdk.models.*;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.SystemMessage;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public abstract class MessageHandlerBase {

    /**
     * @param newBot Initialization object for new Bot instance
     *               -  id          : The unique user ID for the bot.
     *               -  client      : The client ID for the bot.
     *               -  origin      : The profile of the user who requested the bot, as it is returned from GET /bot/users.
     *               -  conversation: The convId as seen by the bot and as returned from GET /bot/convId.
     *               -  token       : The bearer token that the bot must use on inbound requests.
     *               -  locale      : The preferred locale for the bot to use, in form of an IETF language tag.
     * @return If TRUE is returned new bot instance is created for this conversation
     * If FALSE is returned this service declines to create new bot instance for this conversation
     */
    public boolean onNewBot(NewBot newBot) {
        return true;
    }

    /**
     * This callback is invoked by the framework when the bot is added into a conversation
     *
     * @param client  Thread safe wire client that can be used to post back to this conversation
     * @param message SystemMessage object. message.conversation is never null
     */
    public void onNewConversation(WireClient client, SystemMessage message) {

    }

    /**
     * This callback is invoked by the framework every time connection request is received
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param from   UserId of the connection request source user
     * @param to     UserId of the connection request destination user
     * @param status Relation status of the connection request
     * @return TRUE if connection was accepted
     */
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        // Bot received connect request and we want to accept it immediately
        if (status.equals("pending")) {
            try {
                client.acceptConnection(to);
                return true;
            } catch (Exception e) {
                Logger.error("MessageHandlerBase:onConnectRequest: %s", e);
                return false;
            }
        }
        // Connect request sent by the bot got accepted
        if (status.equals("accepted")) {
            return true;
        }
        return false;
    }

    /**
     * This callback is invoked by the framework every time new participant joins this conversation
     *
     * @param client  Thread safe wire client that can be used to post back to this conversation
     * @param message System message object with message.users as List of UserIds that just joined this conversation
     */
    public void onMemberJoin(WireClient client, SystemMessage message) {
    }

    /**
     * @param client  Thread safe wire client that can be used to post back to this conversation
     * @param message System message object with message.users as List of UserIds that just joined this conversation
     */
    public void onMemberLeave(WireClient client, SystemMessage message) {
    }

    /**
     * This callback is called when this bot gets removed from the conversation
     *
     * @param botId  Id of the Bot that got removed
     * @param msg   System message
     */
    public void onBotRemoved(UUID botId, SystemMessage msg) {
    }

    /**
     * @param newBot
     * @return Bot name that will be used for this conversation. If NULL is returned the Default Bot Name will be used
     */
    public String getName(NewBot newBot) {
        return null;
    }

    /**
     * @return Bot's Accent Colour index (from [1 - 7]) that will be used for this conversation. If 0 is returned the
     * default one will be used
     */
    public int getAccentColour() {
        return 0;
    }

    /**
     * @return Asset key for the small profile picture. If NULL is returned the default key will be used
     */
    public String getSmallProfilePicture() {
        return null;
    }

    /**
     * @return Asset key for the big profile picture. If NULL is returned the default key will be used
     */
    public String getBigProfilePicture() {
        return null;
    }

    /**
     * This method is called when a text is posted into the conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    Message containing text
     */
    public void onText(WireClient client, TextMessage msg) {
    }

    /**
     * This method is called when an image is posted into the conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    Message containing image metadata
     */
    public void onImage(WireClient client, ImageMessage msg) {
    }

    /**
     * This method is called when an audio recording is posted into the conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    Message containing audio metadata
     */
    public void onAudio(WireClient client, AudioMessage msg) {
    }

    /**
     * This method is called when a video recording is posted into the conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    Message containing video metadata
     */
    public void onVideo(WireClient client, VideoMessage msg) {
    }

    /**
     * This method is called when a file attachment is posted into the conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    Message containing file metadata
     */
    public void onAttachment(WireClient client, AttachmentMessage msg) {
    }

    /**
     * This is generic method that is called every time something is posted to this conversation.
     *
     * @param client         Thread safe wire client that can be used to post back to this conversation
     * @param userId         User Id for the sender
     * @param genericMessage Generic message as it comes from the BE
     */
    public void onEvent(WireClient client, UUID userId, Messages.GenericMessage genericMessage) {
    }

    /**
     * Called when user edits previously sent message
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    New Message containing replacing messageId
     */
    public void onEditText(WireClient client, EditedTextMessage msg) {

    }

    public void onCalling(WireClient client, CallingMessage msg) {

    }

    public void onConversationRename(WireClient client, SystemMessage systemMessage) {

    }

    public void onDelete(WireClient client, DeletedTextMessage msg) {

    }

    public void onReaction(WireClient client, ReactionMessage msg) {

    }

    public void onNewTeamMember(UUID id, UUID teamId, UUID userId) {

    }

    public void onUserUpdate(UUID id, UUID userId) {

    }

    public void onVideoPreview(WireClient client, ImageMessage msg) {

    }

    public void onLinkPreview(WireClient client, LinkPreviewMessage msg) {

    }

    public void onPing(WireClient client, PingMessage msg) {

    }
    
    /**
     * This method is called when ephemeral text is posted into the conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    Message containing text and expiration time
     */
    public void onText(WireClient client, EphemeralTextMessage msg) {
    }

    public void onConfirmation(WireClient client, ConfirmationMessage msg) {

    }

    public void validatePreKeys(WireClient client, int size) {
        try {
            int minAvailable = 8 * size;
            if (minAvailable > 0) {
                ArrayList<Integer> availablePrekeys = client.getAvailablePrekeys();
                availablePrekeys.remove(new Integer(65535));  //remove the last prekey
                if (!availablePrekeys.isEmpty() && availablePrekeys.size() < minAvailable) {
                    Integer lastKeyOffset = Collections.max(availablePrekeys);
                    ArrayList<PreKey> keys = client.newPreKeys(lastKeyOffset + 1, minAvailable);
                    client.uploadPreKeys(keys);
                    Logger.info("Uploaded " + keys.size() + " prekeys");
                }
            }
        } catch (Exception e) {
            Logger.error("validatePreKeys: bot: %s %s", client.getId(), e);
        }
    }
}
