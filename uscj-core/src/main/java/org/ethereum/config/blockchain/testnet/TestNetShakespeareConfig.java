package org.ethereum.config.blockchain.testnet;

import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;

public class TestNetShakespeareConfig extends TestNetAfterBridgeSyncConfig {
    @Override
    public boolean isUscIP85() {
        return true;
    }

    @Override
    public boolean isUscIP87() { return true; }

    @Override
    public boolean isUscIP89() {
        return true;
    }

    @Override
    public boolean isUscIP90() {
        return true;
    }

    @Override
    public boolean isUscIP91() {
        return true;
    }

    @Override
    public boolean isUscIP92() {
        return true;
    }

    @Override
    public boolean isUscIP93() {
        return true;
    }

    @Override
    public boolean isUscIP94() {
        return true;
    }

    @Override
    public boolean isUscIP98() {
        return true;
    }

    @Override   // UscIP97
    public BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
        return getBlockDifficulty(getConstants(), curBlock, parent);
    }
}
