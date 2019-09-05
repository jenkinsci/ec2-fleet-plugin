package com.amazon.jenkins.ec2fleet;

import com.google.common.util.concurrent.SettableFuture;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import hudson.util.DaemonThreadFactory;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lock {@link Node} from Jenkins usage. For two purposes:
 * <ul>
 *     <li>for correct capacity planning from Jenkins (see details below)</li>
 *     <li>and avoid submit tasks to not fully ready EC2 instances</li>
 * </ul>
 *
 * <h3>Correct capacity</h3>
 * Keep {@link hudson.slaves.NodeProvisioner.PlannedNode#future} not resolved until node will not be online
 * or timeout reached.
 * <p>
 * Default Jenkins node capacity planner {@link hudson.slaves.NodeProvisioner.Strategy} count planned nodes
 * as available capacity, but exclude offline computers {@link Computer#isOnline()} from available capacity.
 * Because EC2 instance requires some time when it was added into fleet to start up, next situation happens:
 * plugin add described capacity as node into Jenkins pool, but Jenkins keeps it as offline as no way to connect,
 * during time when node is offline, Jenkins will try to request more nodes from plugin as offline nodes
 * excluded from capacity.
 * <p>
 * This class fix this situation and keep planned node until instance is really online, so Jenkins planner
 * count planned node as available capacity and doesn't request more.
 * <p>
 * Before each wait it will try to {@link Computer#connect(boolean)}, because by default Jenkins is trying to
 * make a few short interval reconnection initially (when EC2 instance still is not ready) after that
 * with big interval, experiment shows a few minutes and more.
 * <p>
 * Based on https://github.com/jenkinsci/ec2-plugin/blob/master/src/main/java/hudson/plugins/ec2/EC2Cloud.java#L640
 *
 * <h3>Not fully ready EC2 instances</h3>
 * Allow to specify special script which will be executed on a node to confirm that node is ready to accept
 * tasks. This class will block node from incoming task by {@link SlaveComputer#setAcceptingTasks(boolean)}
 * and mark node as temporary offline by {@link Computer#setTemporarilyOffline(boolean, OfflineCause)}
 * until script will be executed with zero code
 *
 * @see EC2FleetCloud
 * @see EC2FleetNode
 * @see EC2OnlineCheckScriptCause
 */
@ThreadSafe
class EC2FleetOnlineChecker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetOnlineChecker.class.getName());
    // use daemon thread, so no problem when stop jenkins
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    public static void start(final EC2FleetNode node, final SettableFuture<Node> future, final long timeout, final long interval, final String connectionScript) {
        EXECUTOR.execute(new EC2FleetOnlineChecker(node, future, timeout, interval, connectionScript));
    }

    private final long start;
    private final EC2FleetNode node;
    private final SettableFuture<Node> future;
    private final long timeout;
    private final long interval;
    private final String checkScript;

    private Phase phase = Phase.PENDING;

    private enum Phase {
        PENDING, CONNECTING, CHECKING
    }

    private EC2FleetOnlineChecker(
            final EC2FleetNode node, final SettableFuture<Node> future, final long timeout, final long interval,
            final String checkScript) {
        this.start = System.currentTimeMillis();
        this.node = node;
        this.future = future;
        this.timeout = timeout;
        this.interval = interval;
        this.checkScript = checkScript;
    }

    /**
     * @param computer possible <code>null</code>
     */
    private void enableAcceptingTasks(@Nullable final Computer computer) {
        if (computer != null && hasCheckScript()) {
            ((SlaveComputer) computer).setAcceptingTasks(true);
            computer.setTemporarilyOffline(false, null);
        }
    }

    @Override
    public void run() {
        final Computer computer = node.toComputer();

        if (future.isCancelled()) {
            enableAcceptingTasks(computer);
            return;
        }

        if (timeout < 1 || interval < 1) {
            enableAcceptingTasks(computer);
            future.set(node);
            info("connection check disabled, resolve planned node");
            return;
        }

        if (System.currentTimeMillis() - start > timeout) {
            enableAcceptingTasks(computer);
            future.setException(new IllegalStateException(
                    "Fail to provision node, cannot connect to " + node.getNodeName() + " in " + timeout + " msec"));
            return;
        }

        if (computer != null && hasCheckScript()) {
            // we disable task accepting when online check script specified to avoid case
            // when node is online and jenkins start to send tasks but script result still not ok
            // at the end of check we set to true
            ((SlaveComputer) computer).setAcceptingTasks(false);
        }

        // to speed up process for cases when computer is present and online we
        // update phase and check it again, instead of just waiting next iteration
        if (phase == Phase.PENDING) {
            if (computer != null) phase = Phase.CONNECTING;
            else info("no connection, wait before retry");
        }

        if (phase == Phase.CONNECTING) {
            if (computer.isOnline()) {
                if (hasCheckScript()) {
                    // make comp temp offline to execute script
                    computer.setTemporarilyOffline(true, new EC2OnlineCheckScriptCause(node.getNodeName()));
                    phase = Phase.CHECKING;
                    info("online, starting checking script");
                } else {
                    enableAcceptingTasks(computer);
                    future.set(node);
                    info("connected, resolve planned node");
                    return;
                }
            } else {
                computer.connect(false);
                info("no connection, connect and wait before retry");
            }
        }

        // we don't check online here as settings temporary offline will set online to false
        // instead we are using phase
        if (phase == Phase.CHECKING) {
            if (executeCheckScript()) {
                enableAcceptingTasks(computer);
                future.set(node);
                info("online and check script is ok, resolve planned node");
                return;
            } else {
                info("check script not ok, wait before retry");
            }
        }

        EXECUTOR.schedule(this, interval, TimeUnit.MILLISECONDS);
    }

    private boolean hasCheckScript() {
        return StringUtils.isNotEmpty(checkScript);
    }

    private boolean executeCheckScript() {
        info("running online check script");
        try {
            final int exitCode = node.executeScript(checkScript);
            info("online check script exit code %s", exitCode);
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, String.format("%s exception when run online check script", node.getNodeName()), e);
            return false;
        }
    }

    private void info(String message, Object... params) {
        LOGGER.log(Level.INFO, node.getNodeName() + " " + String.format(message, params));
    }

}
