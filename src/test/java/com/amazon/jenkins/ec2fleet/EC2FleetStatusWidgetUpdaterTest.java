package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import hudson.widgets.Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EC2FleetStatusWidgetUpdaterTest {

    private MockedStatic<EC2FleetStatusWidgetUpdater> mockedEc2FleetStatusWidgetUpdater;

    @Mock
    private EC2FleetCloud cloud1;

    @Mock
    private EC2FleetCloud cloud2;

    @Mock
    private EC2FleetStatusWidget widget1;

    @Mock
    private EC2FleetStatusWidget widget2;

    private List<Widget> widgets = new ArrayList<>();

    private List<Cloud> clouds = new ArrayList<>();

    private FleetStateStats stats1 = new FleetStateStats(
            "f1", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.<String, Double>emptyMap());

    private FleetStateStats stats2 = new FleetStateStats(
            "f2", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.<String, Double>emptyMap());

    @Before
    public void before() throws Exception {
        mockedEc2FleetStatusWidgetUpdater = Mockito.mockStatic(EC2FleetStatusWidgetUpdater.class);
        mockedEc2FleetStatusWidgetUpdater.when(EC2FleetStatusWidgetUpdater::getClouds).thenReturn(clouds);
        mockedEc2FleetStatusWidgetUpdater.when(EC2FleetStatusWidgetUpdater::getWidgets).thenReturn(widgets);

        when(cloud1.getLabelString()).thenReturn("a");
        when(cloud2.getLabelString()).thenReturn("");
        when(cloud1.getFleet()).thenReturn("f1");
        when(cloud2.getFleet()).thenReturn("f2");

        when(cloud1.getStats()).thenReturn(stats1);
        when(cloud2.getStats()).thenReturn(stats2);
    }

    private EC2FleetStatusWidgetUpdater getMockEC2FleetStatusWidgetUpdater() {
        return new EC2FleetStatusWidgetUpdater();
    }

    @After
    public void after() {
        mockedEc2FleetStatusWidgetUpdater.close();
    }

    @Test
    public void shouldDoNothingIfNoCloudsAndWidgets() {
        getMockEC2FleetStatusWidgetUpdater().doRun();
    }

    @Test
    public void shouldDoNothingIfNoWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verifyNoInteractions(widget1, widget2);
    }

    @Test
    public void shouldIgnoreNonEC2FleetClouds() {
        clouds.add(cloud1);

        Cloud nonEc2FleetCloud = mock(Cloud.class);
        clouds.add(nonEc2FleetCloud);

        widgets.add(widget2);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verify(cloud1).getStats();
        verifyNoInteractions(nonEc2FleetCloud);
    }

    @Test
    public void shouldUpdateCloudCollectAllResultAndUpdateWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        widgets.add(widget1);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verify(widget1).setStatusList(Arrays.asList(
                new EC2FleetStatusInfo(cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired(), stats1.getState().getWarning().get()),
                new EC2FleetStatusInfo(cloud2.getFleet(), stats2.getState().getDetailed(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired(), stats2.getState().getWarning().get())
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIgnoreNonEc2FleetWidgets() {
        clouds.add(cloud1);

        Widget nonEc2FleetWidget = mock(Widget.class);
        widgets.add(nonEc2FleetWidget);

        widgets.add(widget1);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verify(widget1).setStatusList(any(List.class));
        verifyNoInteractions(nonEc2FleetWidget);
    }

}
