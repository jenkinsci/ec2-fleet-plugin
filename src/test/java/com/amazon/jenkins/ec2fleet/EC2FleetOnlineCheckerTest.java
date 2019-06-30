package com.amazon.jenkins.ec2fleet;

import com.google.common.util.concurrent.SettableFuture;
import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({EC2FleetOnlineChecker.class, EC2FleetNode.class, Jenkins.class, Computer.class})
public class EC2FleetOnlineCheckerTest {

    private SettableFuture<Node> future = SettableFuture.create();

    @Mock
    private EC2FleetNode node;

    @Mock
    private Computer computer;

    @Mock
    private Jenkins jenkins;

    @Before
    public void before() throws Exception {
        when(node.getNodeName()).thenReturn("i-1");

        PowerMockito.mockStatic(Jenkins.class);

        when(Jenkins.getInstance()).thenReturn(jenkins);

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
            Assert.assertEquals("Fail to provision node, cannot connect to i-1 in 100 msec", e.getCause().getMessage());
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
        verifyZeroInteractions(computer);
    }

    @Test
    public void shouldSuccessfullyFinishAndNoWaitIfTimeoutIsZero() throws ExecutionException, InterruptedException {
        PowerMockito.when(computer.isOnline()).thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 0, 10);

        Assert.assertSame(node, future.get());
    }

    @Test
    public void shouldWaitIfConnectionIfFailed() throws InterruptedException, ExecutionException {
        final Future failedFuture = Mockito.mock(Future.class);
        when(failedFuture.get()).thenThrow(new ExecutionException(new UnsupportedOperationException()));

        PowerMockito.when(computer.isOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10);

        Assert.assertSame(node, future.get());
    }

}
