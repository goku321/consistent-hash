package com.consistenthash;

public interface Hash extends com.consistenthash.common.Hash {

    /**
     * Generate the hash of a given String
     *
     * @return The hash of {@param s}, which is non-negative integer.
     */
    int makeHash(String s);
}
