package com.mymanet.manta;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by PiaKochar on 1/15/17.
 * http://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
 */

public enum PacketType {
    NONE,
    REQUEST,
    ACK,
    SEND,
    FILE;

    /*https://stackoverflow.com/questions/5292790/convert-integer-value-to-matching-java-enum#5292821*/
    private static final Map<Integer, PacketType> intToTypeMap = new HashMap<Integer, PacketType>();
    private static final Map<PacketType, Integer> typeToIntMap = new HashMap<PacketType, Integer>();

    static {
        intToTypeMap.put(0, NONE);
        intToTypeMap.put(1, REQUEST);
        intToTypeMap.put(2, ACK);
        intToTypeMap.put(3, SEND);
        intToTypeMap.put(4, FILE);
    }

    static {
        typeToIntMap.put(NONE, 0);
        typeToIntMap.put(REQUEST,1);
        typeToIntMap.put(ACK,2);
        typeToIntMap.put(SEND,3);
        typeToIntMap.put(FILE,4);
    }

    public static PacketType fromInt(int i) {
        PacketType type = intToTypeMap.get(Integer.valueOf(i));
        if (type == null)
            return PacketType.NONE;
        return type;
    }

    public static int toInt(PacketType type) {
        Integer i = typeToIntMap.get(type);
        if (i == null)
            return -1;
        return i;
    }
}

