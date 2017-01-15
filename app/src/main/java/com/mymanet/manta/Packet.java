package com.mymanet.manta;

import java.util.LinkedList;

/**
 * Created by PiaKochar on 1/15/17.
 */

class Packet {
    String src;
    String filename;
    int timeToLive;

    Packet(String filename, int timeToLive, String src) {
        this.filename = filename;
        this.timeToLive = timeToLive;
        this.src = src;
    }

    String getSrc() {
        return this.src;
    }

    boolean isTtlZero() {
        return this.timeToLive == 0;
    }

    void decrementTtl() {
        this.timeToLive--;
    }

}
