package com.mymanet.manta;

import java.util.LinkedList;
import java.util.List;

public class RequestPacket {

    private List<Integer> pathFromSrc;
    private String filename;
    private int timeToLive;
    private int src;

    public RequestPacket(String filename, int timeToLive, int src) {
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
}
