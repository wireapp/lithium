package com.wire.blender;

import java.util.ArrayList;
import java.util.List;

public class Blender {
    static {
        System.loadLibrary("blender"); // Load native library at runtime
    }
    
    private final List<BlenderListener> listeners = new ArrayList<>();
    private long blenderPointer;

    public void log(String msg) {
    }

    public void registerListener(BlenderListener listener) {
        listeners.add(listener);
    }

    private void onCallingMessage(String id,
                                  String userId,
                                  String clientId,
                                  String peerId,
                                  String peerClientId,
                                  String content,
                                  boolean trans) {

        for (BlenderListener listener : listeners) {
            listener.onCallingMessage(id,
                    userId,
                    clientId,
                    peerId,
                    peerClientId,
                    content,
                    trans);
        }
    }

    private void onConfigRequest() {
    }

    public native void recvMessage(String convId, String userId,
                                   String clientId, String content);

    public native void init(String name, String userId, String clientId,
                            String localAddress, int minPort, int maxPort);
}
