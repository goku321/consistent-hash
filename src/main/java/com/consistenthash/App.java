package com.consistenthash;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        StickyKey ring = new StickyKey(10);
        Consumer consumer1 = new Consumer("consumer1");
       
        ring.addConsumer(consumer1);
    
 
    }
}
