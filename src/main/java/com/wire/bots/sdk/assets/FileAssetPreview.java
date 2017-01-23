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

package com.wire.bots.sdk.assets;

import com.waz.model.Messages;

import java.io.File;
import java.util.UUID;

public class FileAssetPreview implements IGeneric {

    private final String id;
    private final String name;
    private final String mimeType;
    private final File file;
    private final long size;

    public FileAssetPreview(File file, String mimeType) throws Exception {
        this.id = UUID.randomUUID().toString();
        this.name = file.getName();
        this.mimeType = mimeType;
        this.file = file;
        this.size = file.length();
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(size)
                .setName(name)
                .setMimeType(mimeType);

        Messages.Asset.Builder asset = Messages.Asset.newBuilder()
                .setOriginal(original);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(id)
                .setAsset(asset)
                .build();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public File getFile() {
        return file;
    }
}
