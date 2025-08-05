package com.amazon.jenkins.ec2fleet.monitor;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.HttpResponses;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EC2FleetExecutionErrorMonitorTest {

    private EC2FleetExecutionErrorMonitor monitor;

    @Before
    public void before() {
        monitor = new EC2FleetExecutionErrorMonitor();
        monitor.clear();
    }

    @Test
    public void canProvision_Instance() {
        try(var mockedStatic = mockStatic(ExtensionList.class)) {
            mockedStatic.when(() -> ExtensionList.lookupSingleton(EC2FleetExecutionErrorMonitor.class))
                    .thenReturn(monitor);
            assertNotNull(EC2FleetExecutionErrorMonitor.getInstance());
        }
    }

    @Test
    public void canProvision_DisplayName() {
        assertEquals("EC2 Fleet Plugin", monitor.getDisplayName());
    }

    @Test
    public void canProvision_ErrorMessages() {
        monitor.reportError("Error message 1");
        monitor.reportError("Error message 2");
        monitor.reportError("Error message 3");
        assertEquals(3, monitor.getLastErrorMessage().size());
    }

    @Test
    public void canProvision_ClearErrorMessages() {

        monitor.reportError("Error message 1");
        monitor.reportError("Error message 2");
        assertEquals(2, monitor.getLastErrorMessage().size());
        monitor.clear();
        assertTrue(monitor.getLastErrorMessage().isEmpty());
    }

    @Test
    public void canProvision_IsActivated() {
        monitor.reportError("Error message 1");
        assertTrue(monitor.isActivated());
        monitor.clear();
        assertFalse(monitor.isActivated());
    }

    @Test
    public void test_doActShouldClearAllErrorMessages(){
        var jenkinsMock = mock(Jenkins.class);
        var expectedResult = mock(HttpResponses.HttpResponseException.class);
        try (
                MockedStatic<Jenkins> mockedStatic = mockStatic(Jenkins.class);
                MockedStatic<HttpResponses> mockedResponse = mockStatic(HttpResponses.class)
        ) {

            mockedStatic.when(Jenkins::get).thenReturn(jenkinsMock);
            mockedResponse.when(HttpResponses::redirectToContextRoot).thenReturn(expectedResult);

            monitor.reportError("Error message 1");
            assertTrue(monitor.isActivated());

            // act
            var result = monitor.doAct(null, null);

            // assert
            assertFalse(monitor.isActivated());
            verify(jenkinsMock).checkPermission(any());
            assertEquals(expectedResult, result);
        }
    }

}
