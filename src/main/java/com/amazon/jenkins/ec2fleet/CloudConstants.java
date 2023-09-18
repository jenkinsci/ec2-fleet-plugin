package com.amazon.jenkins.ec2fleet;

class CloudConstants {

    static final String EC2_INSTANCE_TAG_NAMESPACE = "ec2-fleet-plugin";
    static final String EC2_INSTANCE_CLOUD_NAME_TAG = EC2_INSTANCE_TAG_NAMESPACE + ":cloud-name";
    static final boolean DEFAULT_PRIVATE_IP_USED = false;

    static final boolean DEFAULT_ALWAYS_RECONNECT = false;

    static final int DEFAULT_IDLE_MINUTES = 0;

    static final int DEFAULT_MIN_SIZE = 0;

    static final int DEFAULT_MAX_SIZE = 1;

    static final int DEFAULT_MIN_SPARE_SIZE = 0;

    static final int DEFAULT_NUM_EXECUTORS = 1;

    static final boolean DEFAULT_ADD_NODE_ONLY_IF_RUNNING = false;

    static final boolean DEFAULT_RESTRICT_USAGE = false;

    static final boolean DEFAULT_SCALE_EXECUTORS_BY_WEIGHT = false;

    static final int DEFAULT_CLOUD_STATUS_INTERVAL_SEC = 10;

    static final int DEFAULT_INIT_ONLINE_TIMEOUT_SEC = 3 * 60;
    static final int DEFAULT_INIT_ONLINE_CHECK_INTERVAL_SEC = 15;

    static final int DEFAULT_MAX_TOTAL_USES = -1;
}
