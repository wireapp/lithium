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

import com.google.protobuf.ByteString;
import com.waz.model.Messages;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

public class AudioPreview implements IGeneric {
    private final String name;
    private final String mimeType;
    private final String messageId;
    private final long duration;
    private final int size;
    private final byte[] levels;

    public AudioPreview(byte[] bytes, String name, String mimeType, long duration) throws Exception {
        this.name = name;
        this.mimeType = mimeType;
        this.messageId = UUID.randomUUID().toString();
        this.duration = duration;
        this.size = bytes.length;
        this.levels = getNormalizedLoudness(new ByteArrayInputStream(bytes));
    }

    private static byte[] getNormalizedLoudness(InputStream stream) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(stream)) {
            ArrayList<Double> vals = new ArrayList<>(100);

            byte[] buffer = Util.toByteArray(in);
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            double max = 0;
            long sum = 0;
            int step = buffer.length / 100;
            for (int i = 0; i < buffer.length - 1; i += 2) {
                short sample = bb.getShort(i);
                sum += sample * sample;
                if (i % step == 0) {
                    double avg = Math.sqrt(sum / step);
                    sum = 0;
                    if (avg > max)
                        max = avg;
                    vals.add(avg);
                }
            }

            byte[] ret = new byte[vals.size()];
            final double d = 255 / max;
            for (int i = 0; i < vals.size(); i++) {
                long round = Math.round(vals.get(i) * d);
                byte b = (byte) round;
                ret[i] = b;
            }

            return ret;
        } catch (Exception e) {
            Logger.warning(e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public Messages.GenericMessage createGenericMsg() throws Exception {

        Messages.Asset.AudioMetaData.Builder audio = Messages.Asset.AudioMetaData.newBuilder()
                .setDurationInMillis(duration)
                .setNormalizedLoudness(ByteString.copyFrom(levels));

        Messages.Asset.Original.Builder original = Messages.Asset.Original.newBuilder()
                .setSize(size)
                .setName(name)
                .setMimeType(mimeType)
                .setAudio(audio.build());

        Messages.Asset asset = Messages.Asset.newBuilder()
                .setOriginal(original.build())
                .build();

        return Messages.GenericMessage.newBuilder()
                .setMessageId(messageId)
                .setAsset(asset)
                .build();
    }

    public String getName() {
        return name;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

}
