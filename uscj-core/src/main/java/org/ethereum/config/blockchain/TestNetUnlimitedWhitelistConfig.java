package org.ethereum.config.blockchain;

public class TestNetUnlimitedWhitelistConfig extends TestNetAfterBridgeSyncConfig {
    @Override
    public boolean isUscIP87() { return true; }
}
