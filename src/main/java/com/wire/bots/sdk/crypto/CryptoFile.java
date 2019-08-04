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
import com.wire.bots.sdk.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

/**
 * Wrapper for the Crypto Box. This class is thread safe.
 */
public class CryptoFile extends CryptoBase {
    private final CryptoBox box;
    private final String root;

    /**
     * Opens the CryptoBox using given directory path
     * The given directory must be writable.
     * <p/>
     * Note: Do not create multiple OtrManagers that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.
     *
     * @param rootDir The root storage directory of the box
     * @param botId   Bot id
     * @throws Exception
     */
    public CryptoFile(String rootDir, UUID botId) throws CryptoException {
        root = String.format("%s/%s", rootDir, botId);
        box = CryptoBox.open(root);
    }

    /**
     * Opens the CryptoBox using given directory path
     * The given directory must be writable.
     * <p/>
     * Note: Do not create multiple OtrManagers that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.
     *
     * @param db    config
     * @param botId Bot id
     * @throws Exception
     */
    public CryptoFile(UUID botId, Configuration.DB db) throws CryptoException {
        String path;
        try {
            URL root = new URL(db.url);
            path = root.getPath();
        } catch (Exception e) {
            path = "data";
        }
        root = String.format("%s/%s", path, botId);
        box = CryptoBox.open(root);
    }

    @Override
    public ICryptobox box() {
        return box;
    }

    @Override
    public void purge() throws IOException {
        box.close();
        Path rootPath = Paths.get(root);
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
