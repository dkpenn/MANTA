package com.mymanet.manta;

import java.util.LinkedList;
import java.util.List;

class RequestPacket {

    private List<Integer> pathFromSrc;
    private String filename;
    private int timeToLive;
    private String src;

    RequestPacket(String filename, int timeToLive, String src) {
        pathFromSrc = new LinkedList<>();
        this.filename = filename;
        this.timeToLive = timeToLive;
        this.src = src;
    }

    void addToPath(int node) {
        this.pathFromSrc.add(node);
    }

    void decrementTtl() {
        this.timeToLive--;
    }

    boolean isTtlZero() {
        return this.timeToLive == 0;
    }

    String getSrc() {
        return this.src;
    }
}
