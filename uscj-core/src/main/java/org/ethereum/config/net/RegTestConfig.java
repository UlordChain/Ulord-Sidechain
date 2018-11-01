package org.ethereum.config.net;

import org.ethereum.config.blockchain.HardForkActivationConfig;
import org.ethereum.config.blockchain.regtest.RegTestShakespeareConfig;

public class RegTestConfig extends AbstractNetConfig {
    /**
     * By default RegTestConfig should activate every fork at height 0
     * @return a config with all the available forks activated
     */
    public static RegTestConfig getDefaultRegTestConfig() {
        RegTestConfig config = new RegTestConfig();

        config.add(0, new RegTestShakespeareConfig());
        return config;
    }

    public static RegTestConfig getFromConfig(HardForkActivationConfig hardForkActivationConfig) {
        if (hardForkActivationConfig == null) {
            return getDefaultRegTestConfig();
        }
        RegTestConfig customConfig = new RegTestConfig();
        if (hardForkActivationConfig.getShakespeareActivationHeight() != 0) {
            // Only add genesis config if the fork configs are set
            customConfig.add(0, new org.ethereum.config.blockchain.regtest.RegTestGenesisConfig());
        }
        customConfig.add(hardForkActivationConfig.getShakespeareActivationHeight(), new RegTestShakespeareConfig());
        return customConfig;
    }
}
