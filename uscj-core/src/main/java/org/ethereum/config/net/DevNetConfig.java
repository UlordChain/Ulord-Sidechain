package org.ethereum.config.net;

import org.ethereum.config.blockchain.HardForkActivationConfig;
import org.ethereum.config.blockchain.devnet.DevNetGenesisConfig;
import org.ethereum.config.blockchain.devnet.DevNetShakespeareConfig;

public class DevNetConfig extends AbstractNetConfig{
    /**
     * By default DevNetConfig should activate every fork at height 0
     * @return a config with all the available forks activated
     */
    public static DevNetConfig getDefaultDevNetConfig() {
        DevNetConfig config = new DevNetConfig();

        config.add(0, new DevNetShakespeareConfig());
        return config;
    }

    public static DevNetConfig getFromConfig(HardForkActivationConfig hardForkActivationConfig) {
        if (hardForkActivationConfig == null) {
            return getDefaultDevNetConfig();
        }
        DevNetConfig customConfig = new DevNetConfig();
        if (hardForkActivationConfig.getShakespeareActivationHeight() != 0) {
            // Only add genesis config if the fork configs are set
            customConfig.add(0, new DevNetGenesisConfig());
        }
        customConfig.add(hardForkActivationConfig.getShakespeareActivationHeight(), new DevNetShakespeareConfig());
        return customConfig;
    }
}
