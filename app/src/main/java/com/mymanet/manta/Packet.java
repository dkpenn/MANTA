package com.mymanet.manta;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

;

abstract class Packet {
    String src;
    String filename;
    int timeToLive;
    PacketType type;

    Packet(String filename, int timeToLive, String src, PacketType type) {
        this.filename = filename;
        this.timeToLive = timeToLive;
        this.src = src;
        this.type = type;
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

    String getFilename() { return this.filename; }

    PacketType getPacketType() {return this.type; }

    int getTimeToLive() { return this.timeToLive; }

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
