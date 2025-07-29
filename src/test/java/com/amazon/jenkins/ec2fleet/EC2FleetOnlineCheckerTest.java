package com.amazon.jenkins.ec2fleet;

import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class EC2FleetOnlineCheckerTest {

    private MockedStatic<Jenkins> mockedJenkins;

    private CompletableFuture<Node> future = new CompletableFuture<>();

    @Mock
    private EC2FleetNode node;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private Computer computer;

    @Mock
    private Jenkins jenkins;

    @Before
    public void before() throws Exception {
        when(node.getDisplayName()).thenReturn("MockEC2FleetCloud i-1");

        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        // final method
        Mockito.when(node.toComputer()).thenReturn(computer);
    }

    @After
    public void after() {
        mockedJenkins.close();
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
        Mockito.when(computer.isOnline()).thenReturn(true);

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
        Mockito.when(computer.isOnline())
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
        Mockito.when(computer.isOnline()).thenReturn(true);

        Mockito.when(node.toComputer())
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(computer);

        EC2FleetOnlineChecker.start(node, future, 100, 10);

        Assert.assertSame(node, future.get());
        verify(computer, times(1)).isOnline();
    }

}
