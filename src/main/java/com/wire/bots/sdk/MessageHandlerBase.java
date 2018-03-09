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
import com.wire.bots.sdk.server.model.NewBot;

import java.util.ArrayList;

public abstract class MessageHandlerBase {

    /**
     * @param newBot Initialization object for new Bot instance
     *               -  id          : The unique user ID for the bot.
     *               -  client      : The client ID for the bot.
     *               -  origin      : The profile of the user who requested the bot, as it is returned from GET /bot/users.
     *               -  conversation: The conversation as seen by the bot and as returned from GET /bot/conversation.
     *               -  token       : The bearer token that the bot must use on inbound requests.
     *               -  locale      : The preferred locale for the bot to use, in form of an IETF language tag.
     * @return If TRUE is returned new bot instance is created for this conversation
     * If FALSE is returned this service declines to create new bot instance for this conversation
     */
    public boolean onNewBot(NewBot newBot) {
        return true;
    }

    /**
     * This callback is invoked by the framework every time new participant joins this conversation
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     */
    public void onNewConversation(WireClient client) {
    }

    /**
     * This callback is invoked by the framework every time new participant joins this conversation
     *
     * @param client  Thread safe wire client that can be used to post back to this conversation
     * @param userIds List of UserIds that just joined this conversation
     */
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
    }

    /**
     * @param client  Thread safe wire client that can be used to post back to this conversation
     * @param userIds List of UserIds that just left this conversation
     */
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
    }

    /**
     * This callback is called when this bot gets removed from the conversation
     *
     * @param botId Id of the Bot that got removed
     */
    public void onBotRemoved(String botId) {
    }

    /**
     * @return Bot name that will be used for this conversation. If NULL is returned the Default Bot Name will be used
     */
    public String getName() {
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
    public void onEvent(WireClient client, String userId, Messages.GenericMessage genericMessage) {
    }

    /**
     * Called when user edits previously sent message
     *
     * @param client Thread safe wire client that can be used to post back to this conversation
     * @param msg    New Message containing replacing messageId
     */
    public void onEditText(WireClient client, TextMessage msg) {

    }

    public void onCalling(WireClient client, String userId, String clientId, String content) {

    }
}
