package com.amazon.jenkins.ec2fleet;

import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
public class EC2FleetLabelParameters {

    private final Map<String, String> parameters;

    public EC2FleetLabelParameters(final String label) {
        parameters = parse(label);
    }

    public String get(final String name) {
        // todo add fail on null name
        return parameters.get(name.toLowerCase());
    }

    public String getOrDefault(String name, String defaultValue) {
        final String value = get(name);
        return value == null ? defaultValue : value;
    }

    public int getIntOrDefault(String name, int defaultValue) {
        final String value = get(name);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private static Map<String, String> parse(final String label) {
        final Map<String, String> p = new HashMap<>();
        if (label == null) return p;

        final String[] parameters = label.substring(label.indexOf('_') + 1).split(",");
        for (final String parameter : parameters) {
            String[] keyValue = parameter.split("=");
            if (keyValue.length == 2) {
                p.put(keyValue[0].toLowerCase(), keyValue[1]);
            }
        }
        return p;
    }
}
