package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableRow;
import org.htmlunit.html.HtmlTextInput;
import hudson.PluginWrapper;
import hudson.model.Node;
import hudson.slaves.Cloud;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.BuildWatcherExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;
import org.xml.sax.SAXException;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Detailed guides https://jenkins.io/doc/developer/testing/ https://wiki.jenkins.io/display/JENKINS/Unit+Test#UnitTest-DealingwithproblemsinJavaScript
 */
@WithJenkins
class UiIntegrationTest {

    private static final BuildWatcherExtension BW = new BuildWatcherExtension();

    private final EC2FleetCloud.ExecutorScaler noScaling = new EC2FleetCloud.NoScaler();

    private JenkinsRule j;

    @BeforeEach
    void before(JenkinsRule rule) {
        j = rule;
        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);
        final EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);
        final Ec2Client amazonEC2 = mock(Ec2Client.class);

        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);
    }

    @Test
    void shouldFindThePluginByShortName() {
        PluginWrapper wrapper = j.getPluginManager().getPlugin("ec2-fleet");
        assertNotNull(wrapper, "should have a valid plugin");
    }

    @Test
    void shouldShowNodeConfigurationPage() throws Exception {
        final String nodeName = "node-name";
        EC2FleetCloud cloud = new EC2FleetCloud("test-cloud", null, null, null, null, null,
                "test-label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        j.jenkins.addNode(new EC2FleetNode(nodeName, "", "", 1,
                Node.Mode.EXCLUSIVE, "label", new ArrayList<>(), cloud.name,
                j.createComputerLauncher(null), -1));

        HtmlPage page = j.createWebClient().goTo("computer/" + nodeName + "/configure");

        assertTrue(StringUtils.isNotBlank(((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.name").get(0)).getText()));
    }

    @Test
    void shouldReplaceCloudForNodesAfterConfigurationSave() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("test-cloud", null, null, null, null, "",
                "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        j.jenkins.addNode(new EC2FleetNode("mock", "", "", 1,
                Node.Mode.EXCLUSIVE, "", new ArrayList<>(), cloud.name,
                j.createComputerLauncher(null), -1));

        HtmlPage page = j.createWebClient().goTo("cloud/test-cloud/configure");
        HtmlForm form = page.getFormByName("config");

        ((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.labelString").get(0)).setText("new-label");

        HtmlFormUtil.submit(form);

        final Cloud newCloud = j.jenkins.clouds.get(0);
        assertNotNull(newCloud);
        assertNotSame(cloud, newCloud);
        assertSame(newCloud, ((EC2FleetNode) j.jenkins.getNode("mock")).getCloud());
    }

    @Test
    void shouldShowInConfigurationClouds() throws IOException, SAXException {
        Cloud cloud = new EC2FleetCloud("TestCloud", null, null, null, null, null,
                null, null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        HtmlPage page = j.createWebClient().goTo("cloud/TestCloud/configure");

        assertEquals("ec2-fleet", ((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.labelString").get(0)).getText());
    }

    @Test
    void shouldShowMultipleClouds() throws IOException, SAXException {
        Cloud cloud1 = new EC2FleetCloud("a", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        Cloud cloud2 = new EC2FleetCloud("b", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud2);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "name");
        assertEquals(2, elementsByName.size());
        assertEquals("a", ((HtmlInput) elementsByName.get(0)).getValueAttribute());
        assertEquals("b", ((HtmlInput) elementsByName.get(1)).getValueAttribute());
    }

    @Test
    void shouldShowMultipleCloudsWithDefaultName() throws IOException, SAXException {
        Cloud cloud1 = new EC2FleetCloud("TestCloud1", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        Cloud cloud2 = new EC2FleetCloud("TestCloud2", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud2);

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "name");
        assertEquals(2, elementsByName.size());
        assertEquals("TestCloud1", ((HtmlInput) elementsByName.get(0)).getValueAttribute());
        assertEquals("TestCloud2", ((HtmlInput) elementsByName.get(1)).getValueAttribute());
    }

    @Test
    void shouldUpdateProperCloudWhenMultiple() throws Exception {
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud1", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        EC2FleetCloud cloud2 = new EC2FleetCloud("TestCloud2", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud2);

        HtmlPage page = j.createWebClient().goTo("cloud/TestCloud1/configure");
        HtmlForm form = page.getFormByName("config");

        ((HtmlTextInput) IntegrationTest.getElementsByNameWithoutJdk(page, "_.labelString").get(0)).setText("new-label");

        HtmlFormUtil.submit(form);

        assertEquals("new-label", ((EC2FleetCloud)j.jenkins.clouds.get(0)).getLabelString());
        assertEquals("label", ((EC2FleetCloud)j.jenkins.clouds.get(1)).getLabelString());    }

    @Test
    void shouldContainRegionValueInRegionLabel() throws IOException, SAXException {
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud", "uh", null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        HtmlPage page = j.createWebClient().goTo("cloud/TestCloud/configure");

        final List<DomElement> regionDropDown = IntegrationTest.getElementsByNameWithoutJdk(page, "_.region");

        for (final DomElement regionElement : regionDropDown.get(0).getChildElements()) {
            final String displayName = regionElement.getAttributes().getNamedItem("label").getTextContent();
            final String value = regionElement.getAttributes().getNamedItem("value").getTextContent();
            assertTrue(displayName.contains(value));
        }
    }

    @Test
    void shouldHaveRegionCodeAndRegionDescriptionInRegionLabel() throws IOException, SAXException, InterruptedException {
        final String regionName = "us-east-1";
        final String displayName = "us-east-1 US East (N. Virginia)";
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud", "uh", null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        HtmlPage page = j.createWebClient().goTo("cloud/TestCloud/configure");
        boolean found = false;
        for (int i = 0; i < 50; i++) {
            List<DomElement> regionDropDown = IntegrationTest.getElementsByNameWithoutJdk(page, "_.region");
            for (DomElement regionElement : regionDropDown.get(0).getChildElements()) {
                String value = regionElement.getAttributes().getNamedItem("value").getTextContent();
                if ("us-east-1".equals(value)) {
                    found = true;
                    break;
                }
            }
            if (found) break;
            Thread.sleep(100); // wait 100ms
        }
        assertTrue(found, "us-east-1 is missing among the regions");
    }

    // Note: multiple clouds with same name can be created via JCasC only.
    @Test
    void shouldGetFirstWhenMultipleCloudWithSameName() {
        EC2FleetCloud cloud1 = new EC2FleetCloud("TestCloud", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        EC2FleetCloud cloud2 = new EC2FleetCloud("TestCloud", null, null, null, null,
                null, "label", null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud2);

        assertSame(cloud1, j.jenkins.getCloud("TestCloud"));
    }

    @Test
    void shouldGetProperWhenMultipleWithDiffName() {
        EC2FleetCloud cloud1 = new EC2FleetCloud("a", null, null, null, null,
                null, null, null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud1);

        EC2FleetCloud cloud2 = new EC2FleetCloud("b", null, null, null, null,
                null, null, null, null, false, false,
                0, 0, 0, 0, 0, true, false,
                "-1", false, 0, 0,
                10, false, false, noScaling);
        j.jenkins.clouds.add(cloud2);

        assertSame(cloud1, j.jenkins.getCloud("a"));
        assertSame(cloud2, j.jenkins.getCloud("b"));
    }

    @Test
    void verifyCloudNameReadOnlyAfterCloudCreated() throws Exception {
        EC2FleetCloud cloud = new EC2FleetCloud("test-cloud", null, null, null, null, "",
            "label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling);
        j.jenkins.clouds.add(cloud);

        HtmlPage page = j.createWebClient().goTo("cloud/test-cloud/configure");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "_.name");
        assertTrue(((HtmlTextInput) elementsByName.get(0)).isReadOnly());
    }

    @Test
    void verifyExistingDuplicateCloudNamesEditable() throws Exception {
        j.jenkins.clouds.add(new EC2FleetCloud("test-cloud", null, null, null, null, "",
            "label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

        j.jenkins.clouds.add(new EC2FleetCloud("test-cloud", null, null, null, null, "",
            "label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0,
            10, false, false, noScaling));

        HtmlPage page = j.createWebClient().goTo("configureClouds");

        List<DomElement> elementsByName = IntegrationTest.getElementsByNameWithoutJdk(page, "name");
        assertEquals(2, elementsByName.size());
        assertEquals("test-cloud", ((HtmlInput) elementsByName.get(0)).getValueAttribute());
        assertEquals("test-cloud", ((HtmlInput) elementsByName.get(1)).getValueAttribute());

        List<HtmlTableRow> rows = page.getByXPath("//table[@id='clouds']/tbody/tr[@class='repeated-chunk']");
        assertEquals(2, rows.size());
        for (HtmlTableRow row : rows) {
            List<HtmlAnchor> configureLinks = row.getByXPath(".//a[contains(@href, '/configure')]");
            assertEquals(1, configureLinks.size());
        }
    }
}
