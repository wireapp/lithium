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

import com.wire.cryptobox.CryptoException;

public interface WireClientFactory {
    /**
     * Create one thread safe client object that can be used to post messages into conversation
     *
     * @param botId          Bot ID. Unique UUID per conversation
     * @param conversationId Conversation ID
     * @param clientId       Unique client ID for this bot
     * @param token          Life time token
     * @return New instance of Wire Client class
     * @throws CryptoException
     */
    WireClient createClient(String botId, String conversationId, String clientId, String token) throws Exception;
}
