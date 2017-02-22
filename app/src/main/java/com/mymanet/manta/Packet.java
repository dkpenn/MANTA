package com.mymanet.manta;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

;

class Packet {
    String src;
    String filename;
    int timeToLive;
    int pathPosition;
    PacketType type;
    List<String> path;

    Packet(String filename, int timeToLive, String src, PacketType type) {
        this.filename = filename;
        this.timeToLive = timeToLive;
        this.pathPosition = 0;
        this.src = src;
        this.type = type;
        this.path = new ArrayList<>();
    }

    Packet(String filename, int timeToLive, String src, PacketType type, String path, int pathPosition) {
        this.filename = filename;
        this.timeToLive = timeToLive;
        this.pathPosition = pathPosition;
        this.src = src;
        this.type = type;
        this.path = toListOfNodes(path);
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

    void incrPathPosition() { this.pathPosition++; }

    void decrPathPosition() { this.pathPosition--; }

    int getPathPosition() { return this.pathPosition; }

    void addToPath(String node) {
        this.path.add(node);
    }

    String getNodeAtPathPosition() { return this.path.get(this.pathPosition); }

    boolean isLast(String device) { return device.equals(this.path.get(this.path.size() - 1)); }

    void changePacketType(PacketType type) { this.type = type; }

    /**
     * when sending ack, set pathPosition to node before current node
     */
    void changeToACK() {
        this.type = PacketType.ACK;
        this.pathPosition = this.path.size() - 2;
    }

    void changeToSEND() {
        this.type = PacketType.SEND;
        this.pathPosition = 1;
    }

    void changeToFILE() {
        this.type = PacketType.FILE;
        this.pathPosition = this.path.size() - 2;
    }

    private List<String> toListOfNodes(String path) {
        path = path.replace("\n","");
        return new ArrayList<String>(Arrays.asList(path.split("\t")));
    }

    public String pathToString() {
        StringBuffer sb = new StringBuffer();
        for(String node : path) {
            sb.append(node.toCharArray());
            sb.append('\t');
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

}
