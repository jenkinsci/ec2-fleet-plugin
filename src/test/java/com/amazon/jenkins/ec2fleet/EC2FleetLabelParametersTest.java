package com.amazon.jenkins.ec2fleet;

import org.junit.Assert;
import org.junit.Test;

public class EC2FleetLabelParametersTest {

    @Test
    public void parse_emptyForEmptyString() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("");
        Assert.assertNull(parameters.get("aa"));
    }

    @Test
    public void parse_emptyForNullString() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters(null);
        Assert.assertNull(parameters.get("aa"));
    }

    @Test
    public void parse_forString() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("a=1,b=2");
        Assert.assertEquals("1", parameters.get("a"));
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void get_caseInsensitive() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("aBc=1");
        Assert.assertEquals("1", parameters.get("aBc"));
        Assert.assertEquals("1", parameters.get("ABC"));
        Assert.assertEquals("1", parameters.get("abc"));
        Assert.assertEquals("1", parameters.get("AbC"));
        Assert.assertEquals("1", parameters.getOrDefault("AbC", "?"));
        Assert.assertEquals(1, parameters.getIntOrDefault("AbC", -1));
    }

    @Test
    public void parse_withFleetNamePrefixSkipItAndProvideParameters() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("AA_a=1,b=2");
        Assert.assertEquals("1", parameters.get("a"));
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void parse_withEmptyFleetNamePrefixSkipItAndProvideParameters() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("_a=1,b=2");
        Assert.assertEquals("1", parameters.get("a"));
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void parse_withEmptyFleetNamePrefixAndEmptyParametersReturnsEmpty() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("_");
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void parse_skipParameterWithoutValue() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("withoutValue,b=2");
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("withoutValue"));
    }

    @Test
    public void parse_skipParameterWithEmptyValue() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("withoutValue=,b=2");
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("withoutValue"));
    }

}
