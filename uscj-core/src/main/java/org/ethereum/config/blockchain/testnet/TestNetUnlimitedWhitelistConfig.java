package org.ethereum.config.blockchain.testnet;

public class TestNetUnlimitedWhitelistConfig extends TestNetAfterBridgeSyncConfig {
    @Override
    public boolean isUscIP87() { return true; }
}
