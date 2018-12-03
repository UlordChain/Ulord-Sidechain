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


    @Override   // UscIP97 Remove difficulty drop
    public BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
        return getBlockDifficulty(getConstants(), curBlock, parent);
    }

    // Whitelisting adds unlimited option
    @Override
    public boolean isUscIP87() {return true;}

    // Improvements to REMASC contract
    @Override
    public boolean isUscIP85() {
        return true;
    }

    // Bridge local calls
    @Override
    public boolean isUscIP88() { return true; }

    // Improve blockchain block locator
    @Override
    public boolean isUscIP89() {
        return true;
    }

    // Add support for return EXTCODESIZE for precompiled contracts
    @Override
    public boolean isUscIP90() {
        return true;
    }

    // Add support for STATIC_CALL opcode
    @Override
    public boolean isUscIP91() {
        return true;
    }

    // Storage improvements
    @Override
    public boolean isUscIP92() {
        return true;
    }

    // Code Refactor, removes the sample contract
    @Override
    public boolean isUscIP93() {
        return true;
    }

    // Disable OP_CODEREPLACE
    @Override
    public boolean isUscIP94() {
        return true;
    }

    // Disable fallback mining in advance
    @Override
    public boolean isUscIP98() {
        return true;
    }
}
