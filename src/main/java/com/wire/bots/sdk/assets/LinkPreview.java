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

import java.util.UUID;

public class LinkPreview implements IGeneric {
    private final String url;
    private final String title;
    private final Messages.Asset img;
    private UUID messageId = UUID.randomUUID();

    public LinkPreview(String url, String title, Messages.Asset img) {
        this.url = url;
        this.title = title;
        this.img = img;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() {
        // Legacy todo: remove it!
        Messages.Article article = Messages.Article.newBuilder()
                .setTitle(title)
                .setPermanentUrl(url)
                .setImage(img)
                .build();
        // Legacy

        Messages.LinkPreview.Builder linkPreview = Messages.LinkPreview.newBuilder()
                .setUrl(url)
                .setUrlOffset(0)
                .setImage(img)
                .setPermanentUrl(url)
                .setTitle(title)
                .setArticle(article);

        Messages.Text.Builder text = Messages.Text.newBuilder()
                .setContent(url)
                .addLinkPreview(linkPreview);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(getMessageId().toString())
                .setText(text.build())
                .build();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }
}
