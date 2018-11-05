package org.ethereum.config.blockchain.devnet;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeDevNetConstants;
import co.usc.core.BlockDifficulty;
import org.ethereum.config.blockchain.GenesisConfig;

import java.math.BigInteger;

public class DevNetGenesisConfig extends GenesisConfig {
    public static class DevNetConstants extends GenesisConstants {

        private static final byte CHAIN_ID = 52;
        private static final BigInteger DIFFICULTY_BOUND_DIVISOR = BigInteger.valueOf(50);
        private final BlockDifficulty minimumDifficulty = new BlockDifficulty(BigInteger.valueOf(131072));

        @Override
        public BridgeConstants getBridgeConstants() {
            return BridgeDevNetConstants.getInstance();
        }

        @Override
        public byte getChainId() {
            return DevNetConstants.CHAIN_ID;
        }

        @Override
        public BlockDifficulty getMinimumDifficulty() {
            return minimumDifficulty;
        }

        @Override
        public int getDurationLimit() {
            return 14;
        }

        @Override
        public BigInteger getDifficultyBoundDivisor() {
            return DIFFICULTY_BOUND_DIVISOR;
        }
    }

    public DevNetGenesisConfig() {
        super(new DevNetConstants());
    }

    @Override
    public boolean areBridgeTxsFree() {
        return true;
    }
}
