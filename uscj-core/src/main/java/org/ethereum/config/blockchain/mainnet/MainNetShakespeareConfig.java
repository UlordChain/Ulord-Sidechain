package org.ethereum.config.blockchain.mainnet;

import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;

public class MainNetShakespeareConfig extends MainNetAfterBridgeSyncConfig {

    @Override
    public boolean areBridgeTxsFree() {
        return true;
    }

}
