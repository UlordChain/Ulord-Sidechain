package co.usc.peg;

import org.ethereum.config.BlockchainConfig;

public class BridgeStorageConfiguration {
    private final boolean isUnlimitedWhitelistEnabled;

    public BridgeStorageConfiguration(boolean isUnlimitedWhitelistEnabled) {
        this.isUnlimitedWhitelistEnabled = isUnlimitedWhitelistEnabled;
    }

    public boolean getUnlimitedWhitelistEnabled() {
        return isUnlimitedWhitelistEnabled;
    }

    public static BridgeStorageConfiguration fromBlockchainConfig(BlockchainConfig config) {
        return new BridgeStorageConfiguration(config.isUscIP87());
    }
}
