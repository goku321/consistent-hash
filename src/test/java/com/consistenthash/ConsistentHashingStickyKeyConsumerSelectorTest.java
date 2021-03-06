package com.consistenthash;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import com.google.common.collect.ImmutableSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Test(groups = "broker")
public class ConsistentHashingStickyKeyConsumerSelectorTest {
    @Test
    public void testConsumerSelect() throws Exception {

        StickyKey selector = new StickyKey(5);
        String key1 = "anyKey";
        Assert.assertNull(selector.select(key1.getBytes()));

        Consumer consumer1 = mock(Consumer.class);
        when(consumer1.consumerName()).thenReturn("c1");
        selector.addConsumer(consumer1);
        Assert.assertEquals(selector.select(key1.getBytes()), consumer1);

        Consumer consumer2 = mock(Consumer.class);
        when(consumer2.consumerName()).thenReturn("c2");
        selector.addConsumer(consumer2);

        final int N = 200;
        final double PERCENT_ERROR = 0.20; // 20 %

        Map<String, Integer> selectionMap = new HashMap<>();
        for (int i = 0; i < N; i++) {
            String key = UUID.randomUUID().toString();
            Consumer selectedConsumer = selector.select(key.getBytes());
            int count = selectionMap.computeIfAbsent(selectedConsumer.consumerName(), c -> 0);
            selectionMap.put(selectedConsumer.consumerName(), count + 1);
        }

        // Check that keys got assigned uniformely to consumers
        Assert.assertEquals(selectionMap.get("c1"), N/2, N/2 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c2"), N/2, N/2 * PERCENT_ERROR);
        selectionMap.clear();

        Consumer consumer3 = mock(Consumer.class);
        when(consumer3.consumerName()).thenReturn("c3");
        selector.addConsumer(consumer3);

        for (int i = 0; i < N; i++) {
            String key = UUID.randomUUID().toString();
            Consumer selectedConsumer = selector.select(key.getBytes());
            int count = selectionMap.computeIfAbsent(selectedConsumer.consumerName(), c -> 0);
            selectionMap.put(selectedConsumer.consumerName(), count + 1);
        }

        Assert.assertEquals(selectionMap.get("c1"), N/3, N/3 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c2"), N/3, N/3 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c3"), N/3, N/3 * PERCENT_ERROR);
        selectionMap.clear();

        Consumer consumer4 = mock(Consumer.class);
        when(consumer4.consumerName()).thenReturn("c4");
        selector.addConsumer(consumer4);

        for (int i = 0; i < N; i++) {
            String key = UUID.randomUUID().toString();
            Consumer selectedConsumer = selector.select(key.getBytes());
            int count = selectionMap.computeIfAbsent(selectedConsumer.consumerName(), c -> 0);
            selectionMap.put(selectedConsumer.consumerName(), count + 1);
        }

        Assert.assertEquals(selectionMap.get("c1"), N/4, N/4 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c2"), N/4, N/4 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c3"), N/4, N/4 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c4"), N/4, N/4 * PERCENT_ERROR);
        selectionMap.clear();

        selector.removeConsumer(consumer1);

        for (int i = 0; i < N; i++) {
            String key = UUID.randomUUID().toString();
            Consumer selectedConsumer = selector.select(key.getBytes());
            int count = selectionMap.computeIfAbsent(selectedConsumer.consumerName(), c -> 0);
            selectionMap.put(selectedConsumer.consumerName(), count + 1);
        }

        Assert.assertEquals(selectionMap.get("c2"), N/3, N/3 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c3"), N/3, N/3 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c4"), N/3, N/3 * PERCENT_ERROR);
        selectionMap.clear();

        selector.removeConsumer(consumer2);
        for (int i = 0; i < N; i++) {
            String key = UUID.randomUUID().toString();
            Consumer selectedConsumer = selector.select(key.getBytes());
            int count = selectionMap.computeIfAbsent(selectedConsumer.consumerName(), c -> 0);
            selectionMap.put(selectedConsumer.consumerName(), count + 1);
        }

        System.err.println(selectionMap);
        Assert.assertEquals(selectionMap.get("c3"), N/2, N/2 * PERCENT_ERROR);
        Assert.assertEquals(selectionMap.get("c4"), N/2, N/2 * PERCENT_ERROR);
        selectionMap.clear();

        selector.removeConsumer(consumer3);
        for (int i = 0; i < N; i++) {
            String key = UUID.randomUUID().toString();
            Consumer selectedConsumer = selector.select(key.getBytes());
            int count = selectionMap.computeIfAbsent(selectedConsumer.consumerName(), c -> 0);
            selectionMap.put(selectedConsumer.consumerName(), count + 1);
        }

        Assert.assertEquals(selectionMap.get("c4").intValue(), N);
    }

    @Test
    public void testSelectConsumer() throws Exception {
        StickyKey ring = new StickyKey(100);
        String key1 = "oneoneone";

        Consumer consumer1 = mock(Consumer.class);
        when(consumer1.consumerName()).thenReturn("c1");
        ring.addConsumer(consumer1);
        Assert.assertEquals(ring.select(key1.getBytes()), consumer1);

        Consumer consumer2 = mock(Consumer.class);
        when(consumer2.consumerName()).thenReturn("tenant");
        ring.addConsumer(consumer2);
        Assert.assertEquals(ring.select(key1.getBytes()), consumer2);
    }


    @Test
    public void testGetConsumerKeyHashRanges() throws Exception {
        StickyKey selector = new StickyKey(3);
        List<String> consumerName = Arrays.asList("consumer1", "consumer2", "consumer3");
        for (String s : consumerName) {
            Consumer consumer = mock(Consumer.class);
            when(consumer.consumerName()).thenReturn(s);
            selector.addConsumer(consumer);
        }

        Map<String, Set<String>> expectedResult = new HashMap<>();
        expectedResult.put("consumer1", ImmutableSet.of("[0, 330121749]", "[330121750, 618146114]", "[1797637922, 1976098885]"));
        expectedResult.put("consumer2", ImmutableSet.of("[938427576, 1094135919]", "[1138613629, 1342907082]", "[1342907083, 1797637921]"));
        expectedResult.put("consumer3", ImmutableSet.of("[618146115, 772640562]", "[772640563, 938427575]", "[1094135920, 1138613628]"));
        for (Map.Entry<String, List<String>> entry : selector.getConsumerKeyHashRanges().entrySet()) {
            Assert.assertEquals(new HashSet<>(entry.getValue()), expectedResult.get(entry.getKey()));
            expectedResult.remove(entry.getKey());
        }
        Assert.assertEquals(expectedResult.size(), 0);
    }
}
