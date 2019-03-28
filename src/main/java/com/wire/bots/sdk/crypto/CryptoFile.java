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

package com.wire.bots.sdk.crypto;

import com.wire.bots.cryptobox.CryptoBox;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.cryptobox.ICryptobox;

/**
 * Wrapper for the Crypto Box. This class is thread safe.
 */
public class CryptoFile extends CryptoBase {
    private final CryptoBox box;

    /**
     * Opens the CryptoBox using given directory path
     * The given directory must exist and be writable.
     * <p/>
     * Note: Do not create multiple OtrManagers that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.
     *
     * @param uri   The root storage directory of the box
     * @param botId Bot id
     * @throws Exception
     */
    public CryptoFile(String uri, String botId) throws CryptoException {
        String path = String.format("%s/%s", uri, botId);
        box = CryptoBox.open(path);
    }

    @Override
    public ICryptobox box() {
        return box;
    }
}
