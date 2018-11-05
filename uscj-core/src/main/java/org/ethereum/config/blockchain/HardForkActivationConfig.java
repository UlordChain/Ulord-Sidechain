package org.ethereum.config.blockchain;

import com.typesafe.config.Config;

public class HardForkActivationConfig {
    private final int shakespeareActivationHeight;

    private static final String PROPERTY_SHAKESPEARE_NAME = "shakespeare";

    public HardForkActivationConfig(Config config) {
        // If I don't have any config for shakespeareActivationHeight I will set it to 0
        this(config.hasPath(PROPERTY_SHAKESPEARE_NAME) ? config.getInt(PROPERTY_SHAKESPEARE_NAME) : 0);
    }

    public HardForkActivationConfig(int shakespeareActivationHeight) {
        this.shakespeareActivationHeight = shakespeareActivationHeight;
    }

    public int getShakespeareActivationHeight() {
        return shakespeareActivationHeight;
    }

}
