package com.amazon.jenkins.ec2fleet;

import com.google.common.util.concurrent.SettableFuture;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private SlaveComputer computer;

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

        when(node.executeScript(anyString())).thenReturn(0);
    }

    @Test
    public void shouldStopImmediatelyIfFutureIsCancelled() throws InterruptedException, ExecutionException {
        future.cancel(true);

        EC2FleetOnlineChecker.start(node, future, 0, 0, null);
        try {
            future.get();
            Assert.fail();
        } catch (CancellationException e) {
            // ok
        }
    }

    @Test
    public void whenNotOnlineShouldStopAndFailFutureIfTimeout() {
        EC2FleetOnlineChecker.start(node, future, 100, 30, null);
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

        EC2FleetOnlineChecker.start(node, future, TimeUnit.MINUTES.toMillis(1), 0, null);

        Assert.assertSame(node, future.get());
    }

    @Test
    public void shouldFinishWithNodeWhenTimeoutIsZeroWithoutCheck() throws InterruptedException, ExecutionException {
        EC2FleetOnlineChecker.start(node, future, 0, 0, null);

        Assert.assertSame(node, future.get());
        verifyZeroInteractions(computer);
    }

    @Test
    public void shouldSuccessfullyFinishAndNoWaitIfIntervalIsZero() throws ExecutionException, InterruptedException {
        PowerMockito.when(computer.isOnline()).thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 10, 0, null);

        Assert.assertSame(node, future.get());
        verifyZeroInteractions(computer);
    }

    @Test
    public void shouldWaitIfOffline() throws InterruptedException, ExecutionException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10, null);

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

        EC2FleetOnlineChecker.start(node, future, 100, 10, null);

        Assert.assertSame(node, future.get());
        verify(computer, times(1)).isOnline();
    }

    @Test
    public void whenOnlineCheckScriptSpecifiedShouldDisableTaskAcceptingUntilOk() throws InterruptedException, ExecutionException, IOException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10, "script");

        Assert.assertSame(node, future.get());
        verify(computer, times(3)).isOnline();
        verify(node, times(1)).executeScript("script");
        verify(computer, times(3)).setAcceptingTasks(false);
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void ifNoCheckScriptShouldNotTouchAcceptingTasks() throws InterruptedException, ExecutionException, IOException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10, null);

        Assert.assertSame(node, future.get());
        verify(computer, times(3)).isOnline();
        verify(node, never()).executeScript("script");
        verify(computer, never()).setTemporarilyOffline(anyBoolean(), any(OfflineCause.class));
        verify(computer, never()).setAcceptingTasks(anyBoolean());
    }

    @Test
    public void whenOnlineCheckScriptSpecifiedShouldWaitUntilCheckScriptExecutionIsOk()
            throws InterruptedException, ExecutionException, IOException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(true);

        when(node.executeScript(anyString()))
                .thenReturn(1)
                .thenReturn(1)
                .thenReturn(0);

        EC2FleetOnlineChecker.start(node, future, 100, 10, "script");

        Assert.assertSame(node, future.get());
        verify(computer, times(1)).isOnline();
        verify(node, times(3)).executeScript("script");
    }

    @Test
    public void whenOnlineCheckScriptSpecifiedShouldWaitUntilCheckScriptExecutionIsOkAndMarkNodeAsTemporaryOffline()
            throws InterruptedException, ExecutionException, IOException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(true);

        when(node.executeScript(anyString()))
                .thenReturn(1)
                .thenReturn(1)
                .thenReturn(0);

        EC2FleetOnlineChecker.start(node, future, 100, 10, "script");

        Assert.assertSame(node, future.get());
        verify(node, times(3)).executeScript("script");
        verify(computer, times(1)).setTemporarilyOffline(true, new EC2OnlineCheckScriptCause("i-1"));
        verify(computer).setTemporarilyOffline(false, null);
    }

    @Test
    public void whenOnlineCheckScriptSpecifiedShouldWaitIfCheckScriptFailWithException()
            throws InterruptedException, ExecutionException, IOException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(true);

        when(node.executeScript(anyString()))
                .thenThrow(new IOException("test"))
                .thenThrow(new IOException("test"))
                .thenReturn(0);

        EC2FleetOnlineChecker.start(node, future, 100, 10, "script");

        Assert.assertSame(node, future.get());
        verify(node, times(3)).executeScript("script");
        verify(computer, times(1)).setTemporarilyOffline(true, new EC2OnlineCheckScriptCause("i-1"));
        verify(computer).setTemporarilyOffline(false, null);
    }

    @Test
    public void whenOnlineCheckScriptSpecifiedShouldAllowTaskAcceptionAndRemoveTempOfflineIfCancelled()
            throws InterruptedException {
        future.cancel(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10, "script");

        Thread.sleep(100);

        verify(computer).setTemporarilyOffline(false, null);
        verify(computer).setAcceptingTasks(true);
    }

    @Test
    public void whenOnlineCheckScriptSpecifiedShouldAllowTaskAcceptconnAndRemoveTempOfflineIfTimeout()
            throws InterruptedException, IOException {
        PowerMockito.when(computer.isOnline())
                .thenReturn(true);

        when(node.executeScript(anyString()))
                .thenThrow(new IOException("test"));

        EC2FleetOnlineChecker.start(node, future, 100, 10, "script");

        try {
            future.get();
            Assert.fail();
        } catch (ExecutionException e) {
            verify(computer).setAcceptingTasks(true);
            verify(computer).setTemporarilyOffline(false, null);
        }
    }

}
