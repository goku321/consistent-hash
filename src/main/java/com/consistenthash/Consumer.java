package com.consistenthash;

public class Consumer {
    private final String consumerName;

    public Consumer(String name) {
        this.consumerName = name;
    }
    
    public String consumerName() {
        return consumerName;
    }
}
