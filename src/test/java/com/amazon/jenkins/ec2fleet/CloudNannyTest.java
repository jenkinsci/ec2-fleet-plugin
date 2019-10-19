package com.amazon.jenkins.ec2fleet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
            "f1", 1, new FleetStateStats.State(true, "a"), ImmutableSet.<String>of(), Collections.<String, Double>emptyMap());

    private FleetStateStats stats2 = new FleetStateStats(
            "f2", 1, new FleetStateStats.State(true, "a"), ImmutableSet.<String>of(), Collections.<String, Double>emptyMap());

    private int recurrencePeriod = 45;

    private AtomicInteger recurrenceCounter1 = new AtomicInteger();
    private AtomicInteger recurrenceCounter2 = new AtomicInteger();

    private ConcurrentMap<EC2FleetCloud, AtomicInteger> recurrenceCounters = new MapMaker()
            .weakKeys()
            .concurrencyLevel(2)
            .makeMap();

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

        when(cloud1.getCloudStatusIntervalSec()).thenReturn(recurrencePeriod);
        when(cloud2.getCloudStatusIntervalSec()).thenReturn(recurrencePeriod * 2);

        recurrenceCounters.put(cloud1, recurrenceCounter1);
        recurrenceCounters.put(cloud2, recurrenceCounter2);
    }

    private CloudNanny getMockCloudNannyInstance() {
        CloudNanny cloudNanny = Whitebox.newInstance(CloudNanny.class);

        // next execution should trigger running the status check.
        recurrenceCounter1.set(1);
        recurrenceCounter2.set(1);

        Whitebox.setInternalState(cloudNanny, "recurrenceCounters", recurrenceCounters);

        return cloudNanny;
    }

    @Test
    public void shouldDoNothingIfNoCloudsAndWidgets() {
        getMockCloudNannyInstance().doRun();
    }

    @Test
    public void shouldUpdateCloudAndDoNothingIfNoWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        getMockCloudNannyInstance().doRun();
    }

    @Test
    public void shouldUpdateCloudCollectResultAndUpdateWidgets() {
        clouds.add(cloud1);

        widgets.add(widget1);

        getMockCloudNannyInstance().doRun();

        verify(widget1).setStatusList(ImmutableList.of(new EC2FleetStatusInfo(
                cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired())));
    }

    @Test
    public void shouldUpdateCloudCollectResultAndUpdateAllEC2FleetWidgets() {
        clouds.add(cloud1);

        widgets.add(widget1);
        widgets.add(widget2);

        getMockCloudNannyInstance().doRun();

        verify(widget1).setStatusList(ImmutableList.of(new EC2FleetStatusInfo(
                cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired())));
        verify(widget2).setStatusList(ImmutableList.of(new EC2FleetStatusInfo(
                cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired())));
    }

    @Test
    public void shouldIgnoreNonEC2FleetClouds() {
        clouds.add(cloud1);

        Cloud nonEc2FleetCloud = mock(Cloud.class);
        clouds.add(nonEc2FleetCloud);

        widgets.add(widget2);

        getMockCloudNannyInstance().doRun();

        verify(cloud1).update();
        verifyZeroInteractions(nonEc2FleetCloud);
    }

    @Test
    public void shouldUpdateCloudCollectAllResultAndUpdateWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        widgets.add(widget1);

        getMockCloudNannyInstance().doRun();

        verify(widget1).setStatusList(ImmutableList.of(
                new EC2FleetStatusInfo(cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired()),
                new EC2FleetStatusInfo(cloud2.getFleet(), stats2.getState().getDetailed(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired())
        ));
    }

    @Test
    public void shouldIgnoreExceptionsFromUpdateForOneofCloudAndUpdateOther() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        when(cloud1.update()).thenThrow(new IllegalArgumentException("test"));

        widgets.add(widget1);

        getMockCloudNannyInstance().doRun();

        verify(widget1).setStatusList(ImmutableList.of(
                new EC2FleetStatusInfo(cloud2.getFleet(), stats2.getState().getDetailed(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired())
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIgnoreNonEc2FleetWidgets() {
        clouds.add(cloud1);

        Widget nonEc2FleetWidget = mock(Widget.class);
        widgets.add(nonEc2FleetWidget);

        widgets.add(widget1);

        getMockCloudNannyInstance().doRun();

        verify(widget1).setStatusList(any(List.class));
        verifyZeroInteractions(nonEc2FleetWidget);
    }

    @Test
    public void resetCloudInterval() {
        clouds.add(cloud1);
        clouds.add(cloud2);
        CloudNanny cloudNanny = getMockCloudNannyInstance();

        cloudNanny.doRun();

        verify(cloud1).update();
        verify(cloud1, atLeastOnce()).getCloudStatusIntervalSec();
        verify(cloud2).update();
        verify(cloud2, atLeastOnce()).getCloudStatusIntervalSec();


        assertEquals(cloud1.getCloudStatusIntervalSec(), recurrenceCounter1.get());
        assertEquals(cloud2.getCloudStatusIntervalSec(), recurrenceCounter2.get());
    }

    @Test
    public void skipCloudIntervalExecution() {
        widgets.add(widget1);
        clouds.add(cloud1);
        clouds.add(cloud2);
        CloudNanny cloudNanny = getMockCloudNannyInstance();
        recurrenceCounter1.set(2);
        recurrenceCounter2.set(3);

        cloudNanny.doRun();

        verify(cloud1, atLeastOnce()).getCloudStatusIntervalSec();
        verify(cloud2, atLeastOnce()).getCloudStatusIntervalSec();
        verifyNoMoreInteractions(cloud1, cloud2);

        assertEquals(1, recurrenceCounter1.get());
        assertEquals(2, recurrenceCounter2.get());

        // commented because of bug https://github.com/jenkinsci/ec2-fleet-plugin/issues/147
        // verifyZeroInteractions(widget1, widget2);
    }

    @Test
    public void updateOnlyOneCloud() {
        clouds.add(cloud1);
        clouds.add(cloud2);
        CloudNanny cloudNanny = getMockCloudNannyInstance();
        recurrenceCounter1.set(2);
        recurrenceCounter2.set(1);

        cloudNanny.doRun();

        verify(cloud2, atLeastOnce()).getCloudStatusIntervalSec();
        verify(cloud2).update();

        verify(cloud1, atLeastOnce()).getCloudStatusIntervalSec();
        verifyNoMoreInteractions(cloud1);

        assertEquals(1, recurrenceCounter1.get());
        assertEquals(cloud2.getCloudStatusIntervalSec(), recurrenceCounter2.get());
    }
}
