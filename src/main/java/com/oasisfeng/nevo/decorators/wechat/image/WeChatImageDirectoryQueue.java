package com.oasisfeng.nevo.decorators.wechat.image;

public class WeChatImageDirectoryQueue {
    private Record[] array;
    private int head;
    private int current;

    public WeChatImageDirectoryQueue(int capacity) {
        array = new Record[capacity];
        head = 0;
        current = 0;
    }

    public synchronized void pushDirectory(long time ,String name) {
        Record record = new Record();
        record.createTime = time;
        record.name = name;

        array[current] = record;

        current = current % array.length;

        if ( head == current )
            head++;
        current++;
    }

    public synchronized String findDirectory(long time ,int maxDiff) {
        Record result = null;

        for ( int i = head ; i != current ; i = (i + 1) % array.length ) {
            if ( array[i].createTime > time )
                break;
            else
                result = array[i];
        }

        if ( result == null )
            return null;

        if ( Math.abs(result.createTime - time) > maxDiff )
            return null;

        return result.name;
    }

    private static class Record {
        long createTime;
        String name;
    }
}
