package com.amazon.jenkins.ec2fleet;

import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({EC2FleetOnlineChecker.class, EC2FleetNode.class, Jenkins.class, Computer.class})
public class EC2FleetOnlineCheckerTest {

    private CompletableFuture<Node> future = new CompletableFuture<>();

    @Mock
    private EC2FleetNode node;

    @Mock
    private Computer computer;

    @Mock
    private Jenkins jenkins;

    @Before
    public void before() throws Exception {
        when(node.getDisplayName()).thenReturn("MockEC2FleetCloud i-1");

        PowerMockito.mockStatic(Jenkins.class);

        when(Jenkins.get()).thenReturn(jenkins);

        // final method
        PowerMockito.when(node.toComputer()).thenReturn(computer);

        PowerMockito.whenNew(EC2FleetNode.class).withAnyArguments().thenReturn(node);
    }

    @Test
    public void shouldStopImmediatelyIfFutureIsCancelled() throws InterruptedException, ExecutionException {
        future.cancel(true);

        EC2FleetOnlineChecker.start(node, future, 0, 0);
        try {
            future.get();
            Assert.fail();
        } catch (CancellationException e) {
            // ok
        }
    }

    @Test
    public void shouldStopAndFailFutureIfTimeout() {
        EC2FleetOnlineChecker.start(node, future, 100, 50);
        try {
            future.get();
            Assert.fail();
        } catch (InterruptedException | ExecutionException e) {
            Assert.assertEquals("Failed to provision node. Could not connect to node '" + node.getDisplayName() + "' before timeout (100ms)", e.getCause().getMessage());
            Assert.assertEquals(IllegalStateException.class, e.getCause().getClass());
            verify(computer, atLeast(2)).isOnline();
        }
    }

    @Test
    public void shouldFinishWithNodeWhenSuccessfulConnect() throws InterruptedException, ExecutionException {
        PowerMockito.when(computer.isOnline()).thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, TimeUnit.MINUTES.toMillis(1), 0);

        Assert.assertSame(node, future.get());
    }

    @Test
    public void shouldFinishWithNodeWhenTimeoutIsZeroWithoutCheck() throws InterruptedException, ExecutionException {
        EC2FleetOnlineChecker.start(node, future, 0, 0);

        Assert.assertSame(node, future.get());
        verifyNoInteractions(computer);
    }

    @Test
    public void shouldSuccessfullyFinishAndNoWaitIfIntervalIsZero() throws ExecutionException, InterruptedException {
        EC2FleetOnlineChecker.start(node, future, 10, 0);

        Assert.assertSame(node, future.get());
        verifyNoInteractions(computer);
    }

    @Test
    public void shouldWaitIfOffline() throws InterruptedException, ExecutionException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10);

        Assert.assertSame(node, future.get());
        verify(computer, times(3)).connect(false);
    }

    @Test
    public void shouldWaitIfComputerIsNull() throws InterruptedException, ExecutionException {
        PowerMockito.when(computer.isOnline()).thenReturn(true);

        PowerMockito.when(node.toComputer())
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(computer);

        EC2FleetOnlineChecker.start(node, future, 100, 10);

        Assert.assertSame(node, future.get());
        verify(computer, times(1)).isOnline();
    }

}
