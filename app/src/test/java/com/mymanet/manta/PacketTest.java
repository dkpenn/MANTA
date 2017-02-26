package com.mymanet.manta;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        List<String> path1 = p.getPath();
        assertNotNull(path1);
        assertEquals(path1.size(), 1);
        assertEquals(path1.get(0), "Hello");
    }

    @Test
    public void packetPathTwo() throws Exception {
        Packet p =  new Packet("hello.txt", 10, "Lord", PacketType.ACK);
        p.addToPath("Hello");
        String pathString = p.pathToString();
        Packet p2 = new Packet("hello.txt", 10, "Lord", PacketType.ACK, pathString, 0);
        List<String> path2 = p2.getPath();
        assertNotNull(path2);
        assertEquals(path2.size(), 1);
        assertEquals("Hello", path2.get(0));
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
        List<String> path = p2.getPath();
        assertNotNull(path);
        assertEquals(path.size(), 4);
        assertEquals("Hello", path.get(0));
        assertEquals("Goodbye", path.get(1));
        assertEquals("Where", path.get(2));
        assertEquals("Hero" , path.get(3));
    }

    @Test
    public void pathStringOne() throws Exception {
        String path = "Hello\n";
        String p2 =path.replace("\n","");
        assertEquals("Hello",p2);
        //ArrayList<String> (Arrays.asList((path.substring(0, pathLength-1)).split("\t")));
    }

}