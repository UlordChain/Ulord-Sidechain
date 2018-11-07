package org.ethereum.config.blockchain;

public class MainNetUnlimitedWhitelistConfig extends MainNetBeforeBridgeSyncConfig {
    @Override
    public boolean isUscIP87() {return true;}
}