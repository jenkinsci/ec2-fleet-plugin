package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazonaws.services.ec2.AmazonEC2;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlFormUtil;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.PluginWrapper;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProperty;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Detailed guides https://jenkins.io/doc/developer/testing/ https://wiki.jenkins.io/display/JENKINS/Unit+Test#UnitTest-DealingwithproblemsinJavaScript
 */
public class UiIntegrationTest {

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void before() {
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        final AmazonEC2 amazonEC2 = mock(AmazonEC2.class);

        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);
    }

    @Test
    public void shouldFindThePluginByShortName() {
        PluginWrapper wrapper = j.getPluginManager().getPlugin("ec2-fleet");
        assertNotNull("should have a valid plugin", wrapper);
    }

    @Test
    public void shouldShowNodeConfigurationPage() throws Exception {
        final String nodeName = "node-name";
        EC2FleetCloud cloud = new EC2FleetCloud("test-cloud", null, null, null, null, null,
                "test-label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud);

        j.jenkins.addNode(new EC2FleetNode(nodeName, "", "", 1,
                Node.Mode.EXCLUSIVE, "label", new ArrayList<NodeProperty<?>>(), cloud.name,
                j.createComputerLauncher(null), -1));

        HtmlPage page = j.createWebClient().goTo("computer/" + nodeName + "/configure");

        assertTrue(StringUtils.isNotBlank(((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.name").get(0)).getText()));
    }

    @Test
    public void shouldReplaceCloudForNodesAfterConfigurationSave() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("test-cloud", null, null, null, null, "",
                "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud);

        j.jenkins.addNode(new EC2FleetNode("mock", "", "", 1,
                Node.Mode.EXCLUSIVE, "", new ArrayList<NodeProperty<?>>(), cloud.name,
                j.createComputerLauncher(null), -1));

        HtmlPage page = j.createWebClient().goTo("configureClouds");
        HtmlForm form = page.getFormByName("config");

        ((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.labelString").get(0)).setText("new-label");

        HtmlFormUtil.submit(form);

        final Cloud newCloud = j.jenkins.clouds.get(0);
        assertNotNull(newCloud);
        assertNotSame(cloud, newCloud);
        assertSame(newCloud, ((EC2FleetNode) j.jenkins.getNode("mock")).getCloud());
    }

    @Test
    public void shouldShowInConfigurationClouds() throws IOException, SAXException {
        Cloud cloud = new EC2FleetCloud("TestCloud", null, null, null, null, null,
                null, null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        assertEquals("ec2-fleet", ((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.labelString").get(0)).getText());
    }

    @Test
    public void shouldShowMultipleClouds() throws IOException, SAXException {
        Cloud cloud1 = new EC2FleetCloud("a", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        Cloud cloud2 = new EC2FleetCloud("b", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud2);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "_.name");
        assertEquals(2, elementsByName.size());
        assertEquals("a", ((HtmlTextInput) elementsByName.get(0)).getText());
        assertEquals("b", ((HtmlTextInput) elementsByName.get(1)).getText());
    }

    @Test
    public void shouldShowMultipleCloudsWithDefaultName() throws IOException, SAXException {
        Cloud cloud1 = new EC2FleetCloud("TestCloud1", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        Cloud cloud2 = new EC2FleetCloud("TestCloud2", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud2);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "_.name");
        assertEquals(2, elementsByName.size());
        assertEquals("TestCloud1", ((HtmlTextInput) elementsByName.get(0)).getText());
        assertEquals("TestCloud2", ((HtmlTextInput) elementsByName.get(1)).getText());
    }

    @Test
    public void shouldUpdateProperCloudWhenMultiple() throws Exception {
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud1", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        EC2FleetCloud cloud2 = new EC2FleetCloud("TestCloud2", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud2);

        HtmlPage page = j.createWebClient().goTo("configureClouds");
        HtmlForm form = page.getFormByName("config");

        ((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.labelString").get(0)).setText("new-label");

        HtmlFormUtil.submit(form);

        assertEquals("new-label", ((EC2FleetCloud)j.jenkins.clouds.get(0)).getLabelString());
        assertEquals("label", ((EC2FleetCloud)j.jenkins.clouds.get(1)).getLabelString());    }

    @Test
    public void shouldContainRegionValueInRegionLabel() throws IOException, SAXException {
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud", "uh", null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        final List<DomElement> regionDropDown = IntegrationTest.getElementsByNameWithoutJdk(page, "_.region");

        for (final DomElement regionElement : regionDropDown.get(0).getChildElements()) {
            final String displayName = regionElement.getAttributes().getNamedItem("label").getTextContent();
            final String value = regionElement.getAttributes().getNamedItem("value").getTextContent();
            assertTrue(displayName.contains(value));
        }
    }

    @Test
    public void shouldHaveRegionCodeAndRegionDescriptionInRegionLabel() throws IOException, SAXException {
        final String regionName = "us-east-1";
        final String displayName = "us-east-1 US East (N. Virginia)";
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud", "uh", null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        HtmlPage page = j.createWebClient().goTo("configureClouds");
        boolean isPresent = false;

        final List<DomElement> regionDropDown = IntegrationTest.getElementsByNameWithoutJdk(page, "_.region");

        for (final DomElement regionElement : regionDropDown.get(0).getChildElements()) {
            final String label = regionElement.getAttributes().getNamedItem("label").getTextContent();
            final String value = regionElement.getAttributes().getNamedItem("value").getTextContent();
            if (StringUtils.equals(value, regionName)) {
                isPresent = true;
                assertEquals(displayName, label);
            }
        }
        if (!isPresent) {
            fail(String.format("%s is missing among the regions", regionName));
        }
    }

    // Note: multiple clouds with same name can be created via JCasC only.
    @Test
    public void shouldGetFirstWhenMultipleCloudWithSameName() {
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        EC2FleetCloud cloud2 = new EC2FleetCloud("TestCloud", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud2);

        assertSame(cloud1, j.jenkins.getCloud("TestCloud"));
    }

    @Test
    public void shouldGetProperWhenMultipleWithDiffName() {
        EC2FleetCloud cloud1 = new EC2FleetCloud("a", null, null, null, null,
                null, null, null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud1);

        EC2FleetCloud cloud2 = new EC2FleetCloud("b", null, null, null, null,
                null, null, null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0, false,
                10, false);
        j.jenkins.clouds.add(cloud2);

        assertSame(cloud1, j.jenkins.getCloud("a"));
        assertSame(cloud2, j.jenkins.getCloud("b"));
    }

    @Test
    public void verifyCloudNameReadOnlyAfterCloudCreated() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("test-cloud", null, null, null, null, "",
            "label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false);
        j.jenkins.clouds.add(cloud);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "_.name");
        assertTrue(((HtmlTextInput) elementsByName.get(0)).isReadOnly());
    }

    @Test
    public void verifyExistingDuplicateCloudNamesEditable() throws Exception {
        j.jenkins.clouds.add(new EC2FleetCloud("test-cloud", null, null, null, null, "",
            "label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));
        j.jenkins.clouds.add(new EC2FleetCloud("test-cloud", null, null, null, null, "",
            "label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "_.name");
        assertFalse(((HtmlTextInput) elementsByName.get(0)).isReadOnly());
        assertFalse(((HtmlTextInput) elementsByName.get(1)).isReadOnly());
    }
}
