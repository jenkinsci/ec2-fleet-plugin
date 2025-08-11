package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import hudson.widgets.Widget;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EC2FleetStatusWidgetUpdaterTest {

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
            "f1", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.emptyMap());

    private FleetStateStats stats2 = new FleetStateStats(
            "f2", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.emptyMap());

    @BeforeEach
    void before() {
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

    @AfterEach
    void after() {
        mockedEc2FleetStatusWidgetUpdater.close();
    }

    @Test
    void shouldDoNothingIfNoCloudsAndWidgets() {
        getMockEC2FleetStatusWidgetUpdater().doRun();
    }

    @Test
    void shouldDoNothingIfNoWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verifyNoInteractions(widget1, widget2);
    }

    @Test
    void shouldIgnoreNonEC2FleetClouds() {
        clouds.add(cloud1);

        Cloud nonEc2FleetCloud = mock(Cloud.class);
        clouds.add(nonEc2FleetCloud);

        widgets.add(widget2);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verify(cloud1).getStats();
        verifyNoInteractions(nonEc2FleetCloud);
    }

    @Test
    void shouldUpdateCloudCollectAllResultAndUpdateWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        widgets.add(widget1);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verify(widget1).setStatusList(Arrays.asList(
                new EC2FleetStatusInfo(cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired()),
                new EC2FleetStatusInfo(cloud2.getFleet(), stats2.getState().getDetailed(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired())
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldIgnoreNonEc2FleetWidgets() {
        clouds.add(cloud1);

        Widget nonEc2FleetWidget = mock(Widget.class);
        widgets.add(nonEc2FleetWidget);

        widgets.add(widget1);

        getMockEC2FleetStatusWidgetUpdater().doRun();

        verify(widget1).setStatusList(any(List.class));
        verifyNoInteractions(nonEc2FleetWidget);
    }

}
