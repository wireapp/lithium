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

package com.wire.bots.sdk.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextMessage extends MessageBase {
    @JsonProperty
    private String text;

    @JsonProperty
    private UUID quotedMessageId;

    @JsonProperty
    private byte[] quotedMessageSha256;

    @JsonProperty
    private ArrayList<Mention> mentions = new ArrayList<>();

    @JsonCreator
    public TextMessage(@JsonProperty("messageId") UUID messageId,
                       @JsonProperty("conversationId") UUID convId,
                       @JsonProperty("clientId") String clientId,
                       @JsonProperty("userId") UUID userId) {
        super(messageId, convId, clientId, userId);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public UUID getQuotedMessageId() {
        return quotedMessageId;
    }

    public void setQuotedMessageId(UUID quotedMessageId) {
        this.quotedMessageId = quotedMessageId;
    }

    public byte[] getQuotedMessageSha256() {
        return quotedMessageSha256;
    }

    public void setQuotedMessageSha256(byte[] quotedMessageSha256) {
        this.quotedMessageSha256 = quotedMessageSha256;
    }

    public void addMention(String userId, int offset, int len) {
        Mention mention = new Mention();
        mention.userId = UUID.fromString(userId);
        mention.offset = offset;
        mention.length = len;

        mentions.add(mention);
    }

    public ArrayList<Mention> getMentions() {
        return mentions;
    }

    public static class Mention {
        public UUID userId;
        public int offset;
        public int length;
    }
}
