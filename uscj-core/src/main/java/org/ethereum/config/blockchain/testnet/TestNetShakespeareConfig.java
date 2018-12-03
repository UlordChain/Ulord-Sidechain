package org.ethereum.config.blockchain.testnet;

import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;

public class TestNetShakespeareConfig extends TestNetAfterBridgeSyncConfig {
    // Improvements to REMASC contract
    @Override
    public boolean isUscIP85() {
        return true;
    }

    // Whitelisting adds unlimited option
    @Override
    public boolean isUscIP87() { return true; }

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

    @Override   // UscIP97  Remove difficulty drop
    public BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
        return getBlockDifficulty(getConstants(), curBlock, parent);
    }
}
