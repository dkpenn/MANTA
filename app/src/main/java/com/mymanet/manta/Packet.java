package com.mymanet.manta;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * Created by PiaKochar on 1/15/17.
 */

abstract class Packet {
    String src;
    String filename;
    int timeToLive;

    Packet(String filename, int timeToLive, String src) {
        this.filename = filename;
        this.timeToLive = timeToLive;
        this.src = src;
    }

    /**
     * Instantaite a packet from an input stream
     * @param is
     */
    Packet(InputStream is) {
        // TODO implement
        // get src, filename, ttl
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

    /**
     * Translate packet to a file
     * @return file version of packet
     */
    File toFile() {
        // TODO implement
        return new File("hello");
    }
}
