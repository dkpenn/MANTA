package com.mymanet.manta;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by dk on 2/17/17.
 */
public class PacketTest {

    @Test
    public void incrPathPosition() throws Exception {

    }

    @Test
    public void decrPathPosition() throws Exception {

    }

    @Test
    public void addToPath() throws Exception {

    }

    @Test
    public void changePacketType() throws Exception {

    }

    @Test
    public void pathToString() throws Exception {

    }

    @Test
    public void packetPath() throws Exception {
        String path = "Hello";
        Packet p =  new Packet("hello.txt", 10, "Lord", PacketType.ACK, path, 0);
        assertNotNull(p.path);
        assertEquals(p.path.size(), 1);
        assertEquals(p.path.get(0), "Hello");
    }

    @Test
    public void packetPathTwo() throws Exception {
        Packet p =  new Packet("hello.txt", 10, "Lord", PacketType.ACK);
        p.addToPath("Hello");
        String pathString = p.pathToString();
        Packet p2 = new Packet("hello.txt", 10, "Lord", PacketType.ACK, pathString, 0);
        assertNotNull(p2.path);
        assertEquals(p2.path.size(), 1);
        assertEquals("Hello", p2.path.get(0));
    }

    @Test
    public void packetPathThree() throws Exception {
        Packet p =  new Packet("hello.txt", 10, "Lord", PacketType.ACK);
        p.addToPath("Hello");
        p.addToPath("Goodbye");
        p.addToPath("Where");
        p.addToPath("Hero");
        String pathString = p.pathToString();
        Packet p2 = new Packet("hello.txt", 10, "Lord", PacketType.ACK, pathString, 0);
        assertNotNull(p2.path);
        assertEquals(p2.path.size(), 4);
        assertEquals("Hello", p2.path.get(0));
        assertEquals("Goodbye", p2.path.get(1));
        assertEquals("Where", p2.path.get(2));
        assertEquals("Hero" , p2.path.get(3));
    }

    @Test
    public void pathStringOne() throws Exception {
        String path = "Hello\n";
        String p2 =path.replace("\n","");
        assertEquals("Hello",p2);
        //ArrayList<String> (Arrays.asList((path.substring(0, pathLength-1)).split("\t")));
    }

}