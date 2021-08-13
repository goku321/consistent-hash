package com.consistenthash;
import java.nio.charset.StandardCharsets;

public class Murmur3_32Hash implements Hash {
    private static final Murmur3_32Hash instance = new Murmur3_32Hash();

    private Murmur3_32Hash(){ }

    public static Hash getInstance() {
        return instance;
    }

    @Override
    public int makeHash(String s) {
        return com.consistenthash.common.Murmur3_32Hash.getInstance()
            .makeHash(s.getBytes(StandardCharsets.UTF_8)) & Integer.MAX_VALUE;
    }

    @Override
    public int makeHash(byte[] b) {
        return com.consistenthash.common.Murmur3_32Hash.getInstance().makeHash(b) & Integer.MAX_VALUE;
    }
}
