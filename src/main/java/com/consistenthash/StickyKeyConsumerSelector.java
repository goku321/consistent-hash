package com.consistenthash;

import java.util.List;
import java.util.Map;

public interface StickyKeyConsumerSelector {

    int DEFAULT_RANGE_SIZE =  2 << 15;

    /**
     * Add a new consumer.
     *
     * @param consumer new consumer
     */
    void addConsumer(Consumer consumer) throws Exception;

    /**
     * Remove the consumer.
     * @param consumer consumer to be removed
     */
    void removeConsumer(Consumer consumer);

    /**
     * Select a consumer by sticky key.
     *
     * @param stickyKey sticky key
     * @return consumer
     */
    default Consumer select(byte[] stickyKey) {
        return select(makeStickyKeyHash(stickyKey));
    }

    static int makeStickyKeyHash(byte[] stickyKey) {
        return Murmur3_32Hash.getInstance().makeHash(stickyKey);
    }

    /**
     * Select a consumer by hash.
     *
     * @param hash hash corresponding to sticky key
     * @return consumer
     */
    Consumer select(int hash);

    /**
     * Get key hash ranges handled by each consumer.
     * @return A map where key is a consumer name and value is list of hash range it receiving message for.
     */
    Map<String, List<String>> getConsumerKeyHashRanges();
}
