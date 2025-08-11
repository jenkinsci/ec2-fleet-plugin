package com.amazon.jenkins.ec2fleet;

import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EC2FleetOnlineCheckerTest {

    private MockedStatic<Jenkins> mockedJenkins;

    private CompletableFuture<Node> future = new CompletableFuture<>();

    @Mock
    private EC2FleetNode node;

    @Mock
    private Computer computer;

    @Mock
    private Jenkins jenkins;

    @BeforeEach
    void before() {
        when(node.getDisplayName()).thenReturn("MockEC2FleetCloud i-1");

        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        // final method
        Mockito.when(node.toComputer()).thenReturn(computer);
    }

    @AfterEach
    void after() {
        mockedJenkins.close();
    }

    @Test
    void shouldStopImmediatelyIfFutureIsCancelled() {
        future.cancel(true);

        EC2FleetOnlineChecker.start(node, future, 0, 0);
        assertThrows(CancellationException.class, () -> future.get());
    }

    @Test
    void shouldStopAndFailFutureIfTimeout() {
        EC2FleetOnlineChecker.start(node, future, 100, 50);
        ExecutionException e = assertThrows(ExecutionException.class, () -> future.get());
        assertEquals("Failed to provision node. Could not connect to node '" + node.getDisplayName() + "' before timeout (100ms)", e.getCause().getMessage());
        assertEquals(IllegalStateException.class, e.getCause().getClass());
        verify(computer, atLeast(2)).isOnline();
    }

    @Test
    void shouldFinishWithNodeWhenSuccessfulConnect() throws InterruptedException, ExecutionException {
        Mockito.when(computer.isOnline()).thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, TimeUnit.MINUTES.toMillis(1), 0);

        assertSame(node, future.get());
    }

    @Test
    void shouldFinishWithNodeWhenTimeoutIsZeroWithoutCheck() throws InterruptedException, ExecutionException {
        EC2FleetOnlineChecker.start(node, future, 0, 0);

        assertSame(node, future.get());
        verifyNoInteractions(computer);
    }

    @Test
    void shouldSuccessfullyFinishAndNoWaitIfIntervalIsZero() throws ExecutionException, InterruptedException {
        EC2FleetOnlineChecker.start(node, future, 10, 0);

        assertSame(node, future.get());
        verifyNoInteractions(computer);
    }

    @Test
    void shouldWaitIfOffline() throws InterruptedException, ExecutionException {
        Mockito.when(computer.isOnline())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        EC2FleetOnlineChecker.start(node, future, 100, 10);

        assertSame(node, future.get());
        verify(computer, times(3)).connect(false);
    }

    @Test
    void shouldWaitIfComputerIsNull() throws InterruptedException, ExecutionException {
        Mockito.when(computer.isOnline()).thenReturn(true);

        Mockito.when(node.toComputer())
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(computer);

        EC2FleetOnlineChecker.start(node, future, 100, 10);

        assertSame(node, future.get());
        verify(computer, times(1)).isOnline();
    }

}
