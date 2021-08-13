package com.consistenthash.common;

public interface Hash {

    /**
     * Generate the hash of a given byte array.
     *
     * @return The hash of {@code b}, which is non-negative integer.
     */
    int makeHash(byte[] b);
}
