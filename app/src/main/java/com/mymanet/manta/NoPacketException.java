package com.mymanet.manta;

/**
 * Created by dk on 2/4/17.
 * Exception to be thrown if packet is not initialized when sending
 */

public class NoPacketException extends Exception {

    public NoPacketException() {
        super("Packet not initialized");
    }
}
