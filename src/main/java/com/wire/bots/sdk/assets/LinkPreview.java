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

import java.util.UUID;

import com.waz.model.Messages;

public class LinkPreview implements IGeneric {

    private final String url;
    private final String title;

    public LinkPreview(String url, String title) {
        this.url = url;
        this.title = title;
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {
        Messages.Article article = Messages.Article.newBuilder()
                .setTitle(title)
                .setPermanentUrl(url)
                .build();

        Messages.LinkPreview linkPreview = Messages.LinkPreview.newBuilder()
                .setUrl(url)
                .setUrlOffset(0)
                .setArticle(article)
                .build();

        Messages.Text.Builder text = Messages.Text.newBuilder()
                .setContent(url)
                .addLinkPreview(linkPreview);

        return Messages.GenericMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setText(text.build())
                .build();
    }
}
