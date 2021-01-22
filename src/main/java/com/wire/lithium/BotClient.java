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

package com.wire.lithium;

import com.wire.xenon.WireAPI;
import com.wire.xenon.WireClientBase;
import com.wire.xenon.assets.*;
import com.wire.xenon.backend.models.NewBot;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.models.AssetKey;
import com.wire.xenon.tools.Util;

import java.io.File;
import java.util.UUID;

/**
 *
 */
public class BotClient extends WireClientBase {

    public BotClient(WireAPI api, Crypto crypto, NewBot state) {
        super(api, crypto, state);
    }

    public UUID sendTextWithMention(String txt, UUID mentionUserId) throws Exception {
        int offset = Util.mentionStart(txt);
        int len = Util.mentionLen(txt);
        MessageText generic = new MessageText(txt)
                .addMention(mentionUserId, offset, len);

        postGenericMessage(generic);
        return generic.getMessageId();
    }

    public UUID sendPicture(byte[] bytes, String mimeType) throws Exception {
        Picture image = new Picture(bytes, mimeType);

        AssetKey assetKey = uploadAsset(image);
        image.setAssetKey(assetKey.key);
        image.setAssetToken(assetKey.token);

        postGenericMessage(image);
        return image.getMessageId();
    }

    public UUID sendAudio(byte[] bytes, String name, String mimeType, long duration) throws Exception {
        AudioPreview preview = new AudioPreview(bytes, name, mimeType, duration);
        AudioAsset audioAsset = new AudioAsset(bytes, preview);

        postGenericMessage(preview);

        AssetKey assetKey = uploadAsset(audioAsset);
        audioAsset.setAssetKey(assetKey.key);
        audioAsset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(audioAsset);
        return audioAsset.getMessageId();
    }

    public UUID sendVideo(byte[] bytes, String name, String mimeType, long duration, int h, int w) throws Exception {
        UUID messageId = UUID.randomUUID();
        VideoPreview preview = new VideoPreview(name, mimeType, duration, h, w, bytes.length, messageId);
        VideoAsset asset = new VideoAsset(bytes, mimeType, messageId);

        postGenericMessage(preview);

        AssetKey assetKey = uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post original + remote asset message
        postGenericMessage(asset);
        return asset.getMessageId();
    }

    public UUID sendFile(File f, String mime) throws Exception {
        UUID messageId = UUID.randomUUID();
        FileAssetPreview preview = new FileAssetPreview(f.getName(), mime, f.length(), messageId);
        FileAsset asset = new FileAsset(f, mime, messageId);

        // post original
        postGenericMessage(preview);

        // upload asset to backend
        AssetKey assetKey = uploadAsset(asset);
        asset.setAssetKey(assetKey.key);
        asset.setAssetToken(assetKey.token);

        // post remote asset message
        postGenericMessage(asset);
        return asset.getMessageId();
    }

}
