package com.amazon.jenkins.ec2fleet;

import org.junit.Assert;
import org.junit.Test;

public class FleetLabelParametersTest {

    @Test
    public void parse_emptyForEmptyString() {
        final FleetLabelParameters parameters = new FleetLabelParameters("");
        Assert.assertNull(parameters.get("aa"));
    }

    @Test
    public void parse_emptyForNullString() {
        final FleetLabelParameters parameters = new FleetLabelParameters(null);
        Assert.assertNull(parameters.get("aa"));
    }

    @Test
    public void parse_forString() {
        final FleetLabelParameters parameters = new FleetLabelParameters("a=1,b=2");
        Assert.assertEquals("1", parameters.get("a"));
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void get_caseInsensitive() {
        final FleetLabelParameters parameters = new FleetLabelParameters("aBc=1");
        Assert.assertEquals("1", parameters.get("aBc"));
        Assert.assertEquals("1", parameters.get("ABC"));
        Assert.assertEquals("1", parameters.get("abc"));
        Assert.assertEquals("1", parameters.get("AbC"));
        Assert.assertEquals("1", parameters.getOrDefault("AbC", "?"));
        Assert.assertEquals(1, parameters.getIntOrDefault("AbC", -1));
    }

    @Test
    public void parse_withFleetNamePrefixSkipItAndProvideParameters() {
        final FleetLabelParameters parameters = new FleetLabelParameters("AA_a=1,b=2");
        Assert.assertEquals("1", parameters.get("a"));
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void parse_withEmptyFleetNamePrefixSkipItAndProvideParameters() {
        final FleetLabelParameters parameters = new FleetLabelParameters("_a=1,b=2");
        Assert.assertEquals("1", parameters.get("a"));
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void parse_withEmptyFleetNamePrefixAndEmptyParametersReturnsEmpty() {
        final FleetLabelParameters parameters = new FleetLabelParameters("_");
        Assert.assertNull(parameters.get("c"));
    }

    @Test
    public void parse_skipParameterWithoutValue() {
        final FleetLabelParameters parameters = new FleetLabelParameters("withoutValue,b=2");
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("withoutValue"));
    }

    @Test
    public void parse_skipParameterWithEmptyValue() {
        final FleetLabelParameters parameters = new FleetLabelParameters("withoutValue=,b=2");
        Assert.assertEquals("2", parameters.get("b"));
        Assert.assertNull(parameters.get("withoutValue"));
    }

}
