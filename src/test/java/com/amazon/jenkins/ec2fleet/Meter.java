package com.amazon.jenkins.ec2fleet;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class Meter {

    private final String name;
    private final AtomicInteger count = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();

    public Meter(final String name) {
        this.name = name;
    }

    public Shot start() {
        return new Shot(this);
    }

    private void time(long time) {
        count.incrementAndGet();
        total.addAndGet(time);
    }

    public String toString() {
        final long tempTotal = total.get();
        final int tempCount = count.get();
        return name + " meter, " +
                "total " + TimeUnit.MILLISECONDS.toSeconds(tempTotal) + ", sec" +
                " count " + tempCount +
                " avg " + (tempCount == 0 ? "~" : tempTotal / tempCount) + " msec";
    }

    @ThreadSafe
    public static class Shot implements Closeable {

        private final long start;
        private final Meter meter;

        private Shot(Meter meter) {
            this.start = System.currentTimeMillis();
            this.meter = meter;
        }

        public void close() {
            meter.time(System.currentTimeMillis() - start);
        }

    }

}
