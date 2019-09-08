package com.amazon.jenkins.ec2fleet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.slaves.Cloud;
import hudson.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudNanny.class)
public class CloudNannyTest {

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
            "f1", 1, "a", ImmutableSet.<String>of(), Collections.<String, Double>emptyMap());

    private FleetStateStats stats2 = new FleetStateStats(
            "f2", 1, "a", ImmutableSet.<String>of(), Collections.<String, Double>emptyMap());

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(CloudNanny.class);
        PowerMockito.when(CloudNanny.class, "getClouds").thenReturn(clouds);
        PowerMockito.when(CloudNanny.class, "getWidgets").thenReturn(widgets);

        when(cloud1.getLabelString()).thenReturn("a");
        when(cloud2.getLabelString()).thenReturn("");
        when(cloud1.getFleet()).thenReturn("f1");
        when(cloud2.getFleet()).thenReturn("f2");

        when(cloud1.update()).thenReturn(stats1);
        when(cloud2.update()).thenReturn(stats2);
    }

    @Test
    public void shouldDoNothingIfNoCloudsAndWidgets() throws Exception {
        Whitebox.newInstance(CloudNanny.class).doRun();
    }

    @Test
    public void shouldUpdateCloudAndDoNothingIfNoWidgets() throws Exception {
        clouds.add(cloud1);
        clouds.add(cloud2);

        Whitebox.newInstance(CloudNanny.class).doRun();
    }

    @Test
    public void shouldUpdateCloudCollectResultAndUpdateWidgets() throws Exception {
        clouds.add(cloud1);

        widgets.add(widget1);

        Whitebox.newInstance(CloudNanny.class).doRun();

        verify(widget1).setStatusList(ImmutableList.of(new EC2FleetStatusInfo(
                cloud1.getFleet(), stats1.getState(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired())));
    }

    @Test
    public void shouldUpdateCloudCollectResultAndUpdateAllEC2FleetWidgets() throws Exception {
        clouds.add(cloud1);

        widgets.add(widget1);
        widgets.add(widget2);

        Whitebox.newInstance(CloudNanny.class).doRun();

        verify(widget1).setStatusList(ImmutableList.of(new EC2FleetStatusInfo(
                cloud1.getFleet(), stats1.getState(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired())));
        verify(widget2).setStatusList(ImmutableList.of(new EC2FleetStatusInfo(
                cloud1.getFleet(), stats1.getState(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired())));
    }

    @Test
    public void shouldIgnoreNonEC2FleetClouds() throws Exception {
        clouds.add(cloud1);

        Cloud nonEc2FleetCloud = mock(Cloud.class);
        clouds.add(nonEc2FleetCloud);

        widgets.add(widget2);

        Whitebox.newInstance(CloudNanny.class).doRun();

        verify(cloud1).update();
        verifyZeroInteractions(nonEc2FleetCloud);
    }

    @Test
    public void shouldUpdateCloudCollectAllResultAndUpdateWidgets() throws Exception {
        clouds.add(cloud1);
        clouds.add(cloud2);

        widgets.add(widget1);

        Whitebox.newInstance(CloudNanny.class).doRun();

        verify(widget1).setStatusList(ImmutableList.of(
                new EC2FleetStatusInfo(cloud1.getFleet(), stats1.getState(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired()),
                new EC2FleetStatusInfo(cloud2.getFleet(), stats2.getState(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired())
        ));
    }

    @Test
    public void shouldIgnoreExceptionsFromUpdateForOneofCloudAndUpdateOther() throws Exception {
        clouds.add(cloud1);
        clouds.add(cloud2);

        when(cloud1.update()).thenThrow(new IllegalArgumentException("test"));

        widgets.add(widget1);

        Whitebox.newInstance(CloudNanny.class).doRun();

        verify(widget1).setStatusList(ImmutableList.of(
                new EC2FleetStatusInfo(cloud2.getFleet(), stats2.getState(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired())
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIgnoreNonEc2FleetWidgets() throws Exception {
        clouds.add(cloud1);

        Widget nonEc2FleetWidget = mock(Widget.class);
        widgets.add(nonEc2FleetWidget);

        widgets.add(widget1);

        Whitebox.newInstance(CloudNanny.class).doRun();

        verify(widget1).setStatusList(any(List.class));
        verifyZeroInteractions(nonEc2FleetWidget);
    }

}
