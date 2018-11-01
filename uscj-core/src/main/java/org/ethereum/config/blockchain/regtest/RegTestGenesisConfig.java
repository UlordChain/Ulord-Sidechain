package org.ethereum.config.blockchain.regtest;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.core.BlockDifficulty;
import org.ethereum.config.blockchain.GenesisConfig;

import java.math.BigInteger;

public class RegTestGenesisConfig extends GenesisConfig {

    public  static class RegTestConstants extends GenesisConstants {
        private final BlockDifficulty minimumDifficulty = new BlockDifficulty(BigInteger.valueOf(1));
        private static final byte CHAIN_ID = 53;

        @Override
        public BlockDifficulty getFallbackMiningDifficulty() { return BlockDifficulty.ZERO; }

        @Override
        public BridgeConstants getBridgeConstants() {
            return BridgeRegTestConstants.getInstance();
        }

        @Override
        public BlockDifficulty getMinimumDifficulty() {
            return minimumDifficulty;
        }

        @Override
        public int getDurationLimit() {
            return 10;
        }

        @Override
        public int getNewBlockMaxSecondsInTheFuture() {
            return 0;
        }

        @Override
        public byte getChainId() {
            return RegTestConstants.CHAIN_ID;
        }
    }

    public RegTestGenesisConfig() {
        super(new RegTestConstants());
    }

    @Override
    public boolean areBridgeTxsFree() {
        return true;
    }
}
