package com.amazon.jenkins.ec2fleet;

import com.google.common.util.concurrent.SettableFuture;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.util.DaemonThreadFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes EC2 instance ID and trying to connect to instance, in case of success create Jenkins node
 * {@link EC2FleetNode} otherwise return error.
 * <p>
 * https://github.com/jenkinsci/ec2-plugin/blob/master/src/main/java/hudson/plugins/ec2/EC2Cloud.java#L640
 *
 * @see EC2FleetCloud
 * @see EC2FleetNode
 */
@SuppressWarnings("WeakerAccess")
@ThreadSafe
class EC2FleetOnlineChecker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetOnlineChecker.class.getName());
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    public static void start(final Node node, final SettableFuture<Node> future, final long timeout, final long interval) {
        EXECUTOR.execute(new EC2FleetOnlineChecker(node, future, timeout, interval));
    }

    private final long start;
    private final Node node;
    private final SettableFuture<Node> future;
    private final long timeout;
    private final long interval;

    private EC2FleetOnlineChecker(
            final Node node, final SettableFuture<Node> future, final long timeout, final long interval) {
        this.start = System.currentTimeMillis();
        this.node = node;
        this.future = future;
        this.timeout = timeout;
        this.interval = interval;
    }

    @Override
    public void run() {
        if (future.isCancelled()) {
            return;
        }

        if (interval < 1) {
            future.set(node);
            LOGGER.log(Level.INFO, String.format("%s connection check disabled, resolve planned node", node.getNodeName()));
            return;
        }

        if (System.currentTimeMillis() - start > timeout) {
            future.setException(new IllegalStateException(
                    "Fail to provision node, cannot connect to " + node.getNodeName() + " in " + timeout + " msec"));
            return;
        }

        final Computer computer = node.toComputer();
        if (computer != null) {
            if (computer.isOnline()) {
                future.set(node);
                LOGGER.log(Level.INFO, String.format("%s connected, resolve planned node", node.getNodeName()));
                return;
            }
        }

        LOGGER.log(Level.INFO, String.format("%s no connection, wait before retry", node.getNodeName()));
        EXECUTOR.schedule(this, interval, TimeUnit.MILLISECONDS);
    }

}
