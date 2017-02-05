package com.mymanet.manta;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sent by device requesting a file from the network
 */
class RequestPacket extends Packet {

    private List<String> pathFromSrc;

    RequestPacket(String filename, int timeToLive, String src) {
        super(filename, timeToLive, src, PacketType.REQUEST);

        pathFromSrc = new ArrayList<>();
    }

    RequestPacket(InputStream is) {
        super(is);

    }

    RequestPacket(String filename, int timeToLive, String src, String path) {
        super(filename, timeToLive, src, PacketType.REQUEST);

        pathFromSrc = toListOfNodes(path);
    }

    private List<String> toListOfNodes(String path) {
        return new ArrayList<String>(Arrays.asList(path.split("\t")));
    }


    void addToPath(String node) {
        this.pathFromSrc.add(node);
    }

    public String pathToString() {
        StringBuffer sb = new StringBuffer();
        for(String node : pathFromSrc) {
            sb.append(node.toCharArray());
            sb.append('\t');
        }
        sb.append('\n');
        return sb.toString();
     }
}
