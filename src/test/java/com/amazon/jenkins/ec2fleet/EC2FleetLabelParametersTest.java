package com.amazon.jenkins.ec2fleet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EC2FleetLabelParametersTest {

    @Test
    void parse_emptyForEmptyString() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("");
        assertNull(parameters.get("aa"));
    }

    @Test
    void parse_emptyForNullString() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters(null);
        assertNull(parameters.get("aa"));
    }

    @Test
    void parse_forString() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("a=1,b=2");
        assertEquals("1", parameters.get("a"));
        assertEquals("2", parameters.get("b"));
        assertNull(parameters.get("c"));
    }

    @Test
    void get_caseInsensitive() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("aBc=1");
        assertEquals("1", parameters.get("aBc"));
        assertEquals("1", parameters.get("ABC"));
        assertEquals("1", parameters.get("abc"));
        assertEquals("1", parameters.get("AbC"));
        assertEquals("1", parameters.getOrDefault("AbC", "?"));
        assertEquals(1, parameters.getIntOrDefault("AbC", -1));
    }

    @Test
    void parse_withFleetNamePrefixSkipItAndProvideParameters() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("AA_a=1,b=2");
        assertEquals("1", parameters.get("a"));
        assertEquals("2", parameters.get("b"));
        assertNull(parameters.get("c"));
    }

    @Test
    void parse_withEmptyFleetNamePrefixSkipItAndProvideParameters() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("_a=1,b=2");
        assertEquals("1", parameters.get("a"));
        assertEquals("2", parameters.get("b"));
        assertNull(parameters.get("c"));
    }

    @Test
    void parse_withEmptyFleetNamePrefixAndEmptyParametersReturnsEmpty() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("_");
        assertNull(parameters.get("c"));
    }

    @Test
    void parse_skipParameterWithoutValue() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("withoutValue,b=2");
        assertEquals("2", parameters.get("b"));
        assertNull(parameters.get("withoutValue"));
    }

    @Test
    void parse_skipParameterWithEmptyValue() {
        final EC2FleetLabelParameters parameters = new EC2FleetLabelParameters("withoutValue=,b=2");
        assertEquals("2", parameters.get("b"));
        assertNull(parameters.get("withoutValue"));
    }

}
