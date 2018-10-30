package org.ethereum.config.blockchain.mainnet;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeMainNetConstants;
import co.usc.core.BlockDifficulty;
import org.ethereum.config.Constants;
import org.ethereum.config.blockchain.GenesisConfig;
import org.ethereum.core.BlockHeader;

import java.math.BigInteger;

public class MainNetAfterBridgeSyncConfig extends GenesisConfig {

    public static class MainNetConstants extends GenesisConstants {
        private static final BigInteger DIFFICULTY_BOUND_DIVISOR = BigInteger.valueOf(50);
        private static final byte CHAIN_ID = 50;

        // 14 kilo evert 14 secs = 1 kilo/s.
        private final BlockDifficulty fallbackMiningDifficulty = new BlockDifficulty(BigInteger.valueOf((long) 14E3));

        // 0.5 kilo/s. This means that on reset difficulty will allow private mining.
        private final BlockDifficulty minimumDifficulty = new BlockDifficulty(BigInteger.valueOf((long) 14E3 / 2 ));

        @Override
        public BlockDifficulty getFallbackMiningDifficulty() { return fallbackMiningDifficulty; }

        @Override
        public BlockDifficulty getMinimumDifficulty() { return minimumDifficulty; }

        @Override
        public BridgeConstants getBridgeConstants() {
            return BridgeMainNetConstants.getInstance();
        }

        @Override
        public int getDurationLimit() {
            return 14;
        }

        @Override
        public BigInteger getDifficultyBoundDivisor() {
            return DIFFICULTY_BOUND_DIVISOR;
        }

        @Override
        public int getNewBlockMaxSecondsInTheFuture() {
            return 60;
        }

        @Override
        public byte getChainId() {
            return MainNetAfterBridgeSyncConfig.MainNetConstants.CHAIN_ID;
        }

    }

    public MainNetAfterBridgeSyncConfig() {
        super(new MainNetAfterBridgeSyncConfig.MainNetConstants());
    }

    protected MainNetAfterBridgeSyncConfig(Constants constants) {
        super(constants);
    }


    @Override
    public BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
        // If more than 2.5 minutes, reset to original difficulty 0x0000001B58
        if (curBlock.getTimestamp() >= parent.getTimestamp() + 150) {
            return getConstants().getMinimumDifficulty();
        }

        return super.calcDifficulty(curBlock, parent);
    }
}
