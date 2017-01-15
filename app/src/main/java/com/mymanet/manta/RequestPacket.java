package com.mymanet.manta;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent by device requesting a file from the network
 */
class RequestPacket extends Packet {

    private List<String> pathFromSrc;

    RequestPacket(String filename, int timeToLive, String src) {
        super(filename, timeToLive, src);
        pathFromSrc = new ArrayList<>();
    }

    void addToPath(String node) {
        this.pathFromSrc.add(node);
    }

}
