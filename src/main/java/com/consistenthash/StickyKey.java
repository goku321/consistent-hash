package com.consistenthash;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is a consumer selector based fixed hash range.
 *
 * The implementation uses consistent hashing to evenly split, the
 * number of keys assigned to each consumer.
 */
public class StickyKey implements StickyKeyConsumerSelector {

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    // Consistent-Hash ring
    private final NavigableMap<Integer, List<Consumer>> hashRing;

    private final int numberOfPoints;

    public StickyKey(int numberOfPoints) {
        this.hashRing = new TreeMap<>();
        this.numberOfPoints = numberOfPoints;
    }

    @Override
    public void addConsumer(Consumer consumer) throws Exception {
        rwLock.writeLock().lock();
        try {
            // Insert multiple points on the hash ring for every consumer
            // The points are deterministically added based on the hash of the consumer name
            for (int i = 0; i < numberOfPoints; i++) {
                String key = consumer.consumerName() + i;
                int hash = Murmur3_32Hash.getInstance().makeHash(key.getBytes());
                hashRing.compute(hash, (k, v) -> {
                    if (v == null) {
                        return Lists.newArrayList(consumer);
                    } else {
                        if (!v.contains(consumer)) {
                            v.add(consumer);
                            v.sort(Comparator.comparing(Consumer::consumerName, String::compareTo));
                        }
                        return v;
                    }
                });
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void removeConsumer(Consumer consumer) {
        rwLock.writeLock().lock();
        try {
            // Remove all the points that were added for this consumer
            for (int i = 0; i < numberOfPoints; i++) {
                String key = consumer.consumerName() + i;
                int hash = Murmur3_32Hash.getInstance().makeHash(key.getBytes());
                hashRing.compute(hash, (k, v) -> {
                    if (v == null) {
                        return null;
                    } else {
                        v.removeIf(c -> c.consumerName().equals(consumer.consumerName()));
                        if (v.isEmpty()) {
                            v = null;
                        }
                        return v;
                    }
                });
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Consumer select(int hash) {
        rwLock.readLock().lock();
        try {
            if (hashRing.isEmpty()) {
                return null;
            }

            List<Consumer> consumerList;
            Map.Entry<Integer, List<Consumer>> ceilingEntry = hashRing.ceilingEntry(hash);
            if (ceilingEntry != null) {
                consumerList =  ceilingEntry.getValue();
            } else {
                consumerList = hashRing.firstEntry().getValue();
            }

            return consumerList.get(hash % consumerList.size());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, List<String>> getConsumerKeyHashRanges() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        rwLock.readLock().lock();
        try {
            int start = 0;
            for (Map.Entry<Integer, List<Consumer>> entry: hashRing.entrySet()) {
                for (Consumer consumer: entry.getValue()) {
                    result.computeIfAbsent(consumer.consumerName(), key -> new ArrayList<>())
                            .add("[" + start + ", " + entry.getKey() + "]");
                }
                start = entry.getKey() + 1;
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return result;
    }

    Map<Integer, List<Consumer>> getRangeConsumer() {
        return Collections.unmodifiableMap(hashRing);
    }
}
