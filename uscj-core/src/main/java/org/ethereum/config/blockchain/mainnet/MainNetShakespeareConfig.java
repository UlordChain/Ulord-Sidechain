package org.ethereum.config.blockchain.mainnet;

import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;

public class MainNetShakespeareConfig extends MainNetAfterBridgeSyncConfig {
    @Override
    public boolean isUscIP89() {
        return true;
    }

    @Override
    public boolean isUscIP91() {
        return true;
    }

    @Override
    public boolean isUscIP94() {
        return true;
    }

    @Override   // UscIP97
    public BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
        return getBlockDifficulty(getConstants(), curBlock, parent);
    }
}
