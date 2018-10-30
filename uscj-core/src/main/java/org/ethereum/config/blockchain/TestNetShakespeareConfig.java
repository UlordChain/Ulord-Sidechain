package org.ethereum.config.blockchain;

public class TestNetShakespeareConfig extends TestNetAfterBridgeSyncConfig {

    @Override
    public boolean isUscIP89() {
        return true;
    }
}
