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

import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.User;
import com.wire.cryptobox.CryptoException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Thread safe class for postings into this conversation
 */
public interface WireClient extends Closeable {
    /**
     * Post text in the conversation
     *
     * @param txt Plain text to be posted into this conversation
     * @throws Exception
     */
    void sendText(String txt) throws Exception;

    /**
     * Post text into the conversation
     *
     * @param txt     Plain text to be posted into this conversation
     * @param expires Time in milliseconds for this message to expire
     * @throws Exception
     */
    void sendText(String txt, long expires) throws Exception;

    /**
     * Post url with preview into the conversation
     *
     * @param url   Url
     * @param title Title of this page
     * @throws Exception
     */
    void sendLinkPreview(String url, String title) throws Exception;

    /**
     * Post picture
     *
     * @param bytes    Row image to be sent
     * @param mimeType Mime type of the image.
     * @throws Exception
     */
    void sendPicture(byte[] bytes, String mimeType) throws Exception;

    /**
     * Post audio file
     *
     * @param bytes    Raw audio file
     * @param name     Name of this content - this will be showed as title
     * @param mimeType Mime Type of this content
     * @param duration Duration in milliseconds
     * @throws Exception
     */
    void sendAudio(byte[] bytes, String name, String mimeType, long duration) throws Exception;

    /**
     * Post generic file up to 25MB as an attachment into this conversation.
     *
     * @param file File to be sent as attachment
     * @param mime Mime type of this attachment
     * @throws Exception
     */
    void sendFile(File file, String mime) throws Exception;

    /**
     * This method is invoked by the sdk. It sends delivery receipt when the message is received
     *
     * @param msgId Message ID as received from the Backend
     * @throws Exception
     */
    void sendDelivery(String msgId) throws Exception;

    /**
     * This method downloads asset from the Backend.
     *
     * @param assetKey        Unique asset identifier (UUID)
     * @param assetToken      Asset token (null in case of public assets)
     * @param sha256Challenge SHA256 hash code for this asset
     * @param otrKey          Encryption key to be used to decrypt the data
     * @return Decrypted asset data
     * @throws Exception
     */
    byte[] downloadAsset(String assetKey, String assetToken, byte[] sha256Challenge, byte[] otrKey) throws Exception;

    /**
     * @return Bot ID as UUID
     */
    String getId();

    /**
     * @return Conversation ID as UUID
     */
    String getConversationId();

    /**
     * @return Device ID as returned by the Wire Backend
     */
    String getDeviceId();

    /**
     * Fetch users' profiles from the Backend
     *
     * @param userIds User IDs (UUID) that are being requested
     * @return Collection of user profiles (name, accent colour,...)
     * @throws IOException
     */
    Collection<User> getUsers(ArrayList<String> userIds) throws IOException;

    /**
     * Fetch conversation details from the Backend
     *
     * @return Conversation details including Conversation ID, Conversation name, List of participants
     * @throws IOException
     */
    Conversation getConversation() throws IOException;

    /**
     * Bots cannot send/receive/accept connect requests. This method can be used when
     * running the sdk as a regular user and you need to
     * accept/reject a connect request.
     *
     * @param user User ID as UUID
     * @throws IOException
     */
    void acceptConnection(String user) throws IOException;

    /**
     * Decrypt cipher either using existing session or it creates new session from this cipher and decrypts
     *
     * @param userId   Sender's User id
     * @param clientId Sender's Client id
     * @param cypher   Encrypted, Base64 encoded string
     * @return Decrypted blob
     * @throws com.wire.cryptobox.CryptoException
     */
    byte[] decrypt(String userId, String clientId, String cypher) throws CryptoException;

    /**
     * Invoked by the sdk. Called once when the conversation is created
     *
     * @return Last prekey
     * @throws CryptoException
     */
    PreKey newLastPreKey() throws CryptoException;

    /**
     * Invoked by the sdk. Called once when the conversation is created and then occasionally when number of available
     * keys drops too low
     *
     * @param from  Starting offset
     * @param count Number of keys to generate
     * @return List of prekeys
     * @throws CryptoException
     */
    ArrayList<PreKey> newPreKeys(int from, int count) throws CryptoException;

    /**
     * Uploads previously generated prekeys to BE
     *
     * @param preKeys Pre keys to be uploaded
     * @throws IOException
     */
    void uploadPreKeys(ArrayList<PreKey> preKeys) throws IOException;

    /**
     * Returns the list of available prekeys.
     * If the number is too low (less than 8) you should generate new prekeys and upload them to BE
     *
     * @return List of available prekeys' ids
     */
    ArrayList<Integer> getAvailablePrekeys();

    /**
     * Checks if CryptoBox is closed
     *
     * @return True if crypto box is closed
     */
    boolean isClosed();

    byte[] downloadProfilePicture(String assetKey) throws IOException;
}