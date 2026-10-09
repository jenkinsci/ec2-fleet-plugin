package com.amazon.jenkins.ec2fleet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EC2FleetConnectionFailureListenerTest {

    private final EC2FleetConnectionFailureListener listener = new EC2FleetConnectionFailureListener();
    private final EC2FleetNodeComputer computer = mock(EC2FleetNodeComputer.class);
    private final EC2FleetNode node = mock(EC2FleetNode.class);
    private final AbstractEC2FleetCloud cloud = mock(AbstractEC2FleetCloud.class);
    private final TaskListener taskListener = mock(TaskListener.class);
    private final SSHLauncher sshLauncher = mock(SSHLauncher.class);

    @BeforeEach
    void setUp() {
        when(computer.getBaseLauncher()).thenReturn(sshLauncher);
        when(sshLauncher.getMaxNumRetries()).thenReturn(2);
        when(sshLauncher.getLaunchTimeoutSeconds()).thenReturn(10);
        when(sshLauncher.getRetryWaitTime()).thenReturn(3);
    }

    @Test
    void waitsForConfiguredSshRetryWindowBeforeSchedulingTermination() {
        when(computer.getNode()).thenReturn(node);
        when(node.getCloud()).thenReturn(cloud);
        when(node.getInstanceId()).thenReturn("i-1");
        when(cloud.isTerminateOnConnectionFailure()).thenReturn(true);
        when(cloud.scheduleToTerminate("i-1", true, EC2AgentTerminationReason.CONNECTION_FAILURE))
                .thenReturn(true);
        ArgumentCaptor<Runnable> check = ArgumentCaptor.forClass(Runnable.class);

        listener.onLaunchFailure(computer, taskListener);

        verify(computer).scheduleConnectionFailureCheck(any(), check.capture(), eq(39_000L));
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any());

        check.getValue().run();

        verify(cloud).scheduleToTerminate("i-1", true, EC2AgentTerminationReason.CONNECTION_FAILURE);
        verify(computer).setAcceptingTasks(false);
    }

    @Test
    void doesNotScheduleWhenDisabled() {
        when(computer.getNode()).thenReturn(node);
        when(node.getCloud()).thenReturn(cloud);
        when(cloud.isTerminateOnConnectionFailure()).thenReturn(false);

        listener.onLaunchFailure(computer, taskListener);

        verify(computer, never()).scheduleConnectionFailureCheck(any(), any(), anyLong());
        verify(cloud, never()).scheduleToTerminate(anyString(), anyBoolean(), any());
    }

    @Test
    void doesNotScheduleAfterSuccessfulConnection() {
        when(computer.hasConnectedSuccessfully()).thenReturn(true);

        listener.onLaunchFailure(computer, taskListener);

        verify(computer, never()).getBaseLauncher();
        verify(node, never()).getCloud();
    }

    @Test
    void doesNotScheduleForNonSshLauncher() {
        when(computer.getBaseLauncher()).thenReturn(mock(hudson.slaves.ComputerLauncher.class));

        listener.onLaunchFailure(computer, taskListener);

        verify(computer, never()).scheduleConnectionFailureCheck(any(), any(), anyLong());
    }

    @Test
    void tracksSuccessfulConnection() {
        listener.onOnline(computer);

        verify(computer).markConnectedSuccessfully();
    }

    @Test
    void ignoresOtherComputerTypes() {
        listener.onLaunchFailure(mock(Computer.class), taskListener);
    }
}
