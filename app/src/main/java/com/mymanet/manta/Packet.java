package com.mymanet.manta;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

;

class Packet {
    private String src;
    private String filename;
    private int timeToLive;
    private int pathPosition;
    private PacketType type;
    private List<String> path;

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

    List<String> getPath() {
        List<String> pathCopy = new ArrayList<>();
        for(String node : path) {
            pathCopy.add(node);
        }

        return pathCopy;
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

    /**
     * when sending ack, set pathPosition to node before current node
     */
    void changeToACK() {
        this.type = PacketType.ACK;
        this.pathPosition = this.path.size() - 2;
    }

    /**
     * when sending send, set pathPosition to second node
     */
    void changeToSEND() {
        this.type = PacketType.SEND;
        this.pathPosition = 1;
    }

    /**
     * when sending file, set pathPosition to node before current node
     */
    void changeToFILE() {
        this.type = PacketType.FILE;
        this.pathPosition = this.path.size() - 2;
    }

    void packetToStream(PrintWriter out, String type) {
        out.println(type);
        out.println(src);
        out.println(filename);
        out.println(timeToLive + "");
        out.println(pathToString());
        out.println(pathPosition + "");
    }

    void packetToStream(OutputStream out, String type) {
        try {
            String newline = "\n";
            out.write(type.getBytes("UTF-8"));
            out.write(newline.getBytes("UTF-8"));

            out.write(src.getBytes("UTF-8"));
            out.write(newline.getBytes("UTF-8"));

            out.write(filename.getBytes("UTF-8"));
            out.write(newline.getBytes("UTF-8"));

            out.write((timeToLive + "").getBytes("UTF-8"));
            out.write(newline.getBytes("UTF-8"));

            out.write(pathToString().getBytes("UTF-8"));
            out.write(newline.getBytes("UTF-8"));

            out.write((pathPosition + "").getBytes("UTF-8"));
            out.write(newline.getBytes("UTF-8"));

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> toListOfNodes(String path) {
        path = path.replace("\n","");
        ArrayList<String> nodes = new ArrayList<>(Arrays.asList(path.split("\t")));

        for(String node: nodes) {
            node.trim();
        }

        return nodes;
    }

    String pathToString() {
        StringBuffer sb = new StringBuffer();
        for(String node : path) {
            sb.append(node.toCharArray());
            sb.append('\t');
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

}
