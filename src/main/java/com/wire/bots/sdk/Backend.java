package com.wire.bots.sdk;

import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.models.otr.Devices;
import com.wire.bots.sdk.models.otr.Missing;
import com.wire.bots.sdk.models.otr.OtrMessage;
import com.wire.bots.sdk.models.otr.PreKeys;

public interface Backend {
    Devices sendMessage(OtrMessage msg, Object... ignoreMissing) throws HttpException;

    Devices sendPartialMessage(OtrMessage msg, String userId) throws HttpException;

    PreKeys getPreKeys(Missing missing);
}
