package com.mymanet.manta;

import java.util.List;

/**
 * Created by PiaKochar on 1/15/17.
 */

/**
 * Sent back to requester when the file is found
 */
class AcknowledgementPacket extends Packet {

    List<String> path;

    AcknowledgementPacket(String filename, int timeToLive, String src, List<String> path) {
        super(filename, timeToLive, src);
        this.path = path;
    }

    void decrementPath() {
        this.path.remove(this.path.size() - 1);
    }

    String getNextNode() {
        return this.path.get(this.path.size() - 1);
    }
}
