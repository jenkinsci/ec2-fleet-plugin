package com.amazon.jenkins.ec2fleet;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LazyUuidTest {

    @Test
    public void getValue_provides_uuid() {
        Assert.assertEquals(36, new LazyUuid().getValue().length());
    }

    @Test
    public void getValue_provides_same_value_if_multiplecall() {
        final LazyUuid lazyUuid = new LazyUuid();
        Assert.assertEquals(lazyUuid.getValue(), lazyUuid.getValue());
        Assert.assertEquals(lazyUuid.getValue(), lazyUuid.getValue());
    }

    @Test
    public void getValue_is_thread_safe() throws InterruptedException {
        final LazyUuid lazyUuid = new LazyUuid();

        final LazyUuidGetter[] threads = new LazyUuidGetter[3];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new LazyUuidGetter(lazyUuid);
            threads[i].start();
        }

        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        final Set<String> history = new HashSet<>();

        for (final LazyUuidGetter thread : threads) {
            thread.interrupt();

            history.addAll(thread.history);
        }

        Assert.assertEquals(1, history.size());
        Assert.assertNotNull(history.iterator().next());
    }

    private static class LazyUuidGetter extends Thread {

        private final Set<String> history;
        private final LazyUuid lazyUuid;

        LazyUuidGetter(LazyUuid lazyUuid) {
            this.lazyUuid = lazyUuid;
            history = new HashSet<>();
        }

        @Override
        public void run() {
            while (true) {
                history.add(lazyUuid.getValue());

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return; // stop
                }
            }
        }
    }
}
