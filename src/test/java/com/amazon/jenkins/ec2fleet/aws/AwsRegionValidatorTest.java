package com.amazon.jenkins.ec2fleet.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AwsRegionValidatorTest {

    @Test
    void isValidRegionName_acceptsStandardAwsRegion() {
        assertTrue(AwsRegionValidator.isValidRegionName("us-east-1"));
    }

    @Test
    void isValidRegionName_acceptsGovAndIsoStyleRegions() {
        assertTrue(AwsRegionValidator.isValidRegionName("us-gov-west-1"));
        assertTrue(AwsRegionValidator.isValidRegionName("us-isob-east-1"));
    }

    @Test
    void isValidRegionName_rejectsCharactersThatCanAffectHostConstruction() {
        assertFalse(AwsRegionValidator.isValidRegionName("us-east-1.amazonaws.com"));
        assertFalse(AwsRegionValidator.isValidRegionName("us-east-1/../../"));
        assertFalse(AwsRegionValidator.isValidRegionName("us-east-1:443"));
    }

    @Test
    void normalizeRegionName_trimsAndLowercases() {
        assertEquals("us-east-1", AwsRegionValidator.normalizeRegionName("  US-EAST-1  "));
    }

    @Test
    void normalizeRegionName_returnsNullForBlankInput() {
        assertNull(AwsRegionValidator.normalizeRegionName("   "));
    }
}
