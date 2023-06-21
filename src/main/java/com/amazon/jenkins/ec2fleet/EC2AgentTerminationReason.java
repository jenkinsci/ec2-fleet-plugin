package com.amazon.jenkins.ec2fleet;

/**
 * Enum to represent the reason for termination of an EC2 instance by the plugin.
 */
public enum EC2AgentTerminationReason {
    IDLE_FOR_TOO_LONG("Agent idle for too long"),
    MAX_TOTAL_USES_EXHAUSTED("MaxTotalUses exhausted for agent"),
    EXCESS_CAPACITY("Excess capacity for fleet"),
    AGENT_DELETED("Agent deleted");

    private final String description;

    EC2AgentTerminationReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static EC2AgentTerminationReason fromDescription(String desc) {
        for (EC2AgentTerminationReason reason: values()) {
            if(reason.description.equalsIgnoreCase(desc)) {
                return reason;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.description;
    }
}
