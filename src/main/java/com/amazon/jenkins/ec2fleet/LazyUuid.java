package com.amazon.jenkins.ec2fleet;

import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

/**
 * Provide uuid string, create it when first call happens
 */
@ThreadSafe
@SuppressWarnings("WeakerAccess")
public class LazyUuid {

    private String value;

    public synchronized String getValue() {
        if (value == null) {
            value = UUID.randomUUID().toString();
        }
        return value;
    }

    public synchronized void setValue(String value) {
        this.value = value;
    }
}
