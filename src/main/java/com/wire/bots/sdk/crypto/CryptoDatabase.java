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

import com.wire.bots.cryptobox.CryptoDb;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.cryptobox.ICryptobox;
import com.wire.bots.cryptobox.IStorage;

import java.io.IOException;

/**
 * Wrapper for the Crypto Box. This class is thread safe.
 */
public class CryptoDatabase extends CryptoBase {
    private final CryptoDb box;

    /**
     * Opens the CryptoBox using given directory path
     * The given directory must exist and be writable.
     * <p/>
     * Note: Do not create multiple OtrManagers that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.
     *
     * @param botId Bot id
     * @param storage Instance of a IStorage class
     * @throws Exception
     */
    public CryptoDatabase(String botId, IStorage storage) throws CryptoException {
        try {
            box = new CryptoDb(botId, storage);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    public CryptoDatabase(String botId, IStorage storage, String dir) throws CryptoException {
        try {
            box = new CryptoDb(botId, storage, dir);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public ICryptobox box() {
        return box;
    }

    @Override
    public void purge() throws IOException {
        box.purge();
    }
}
