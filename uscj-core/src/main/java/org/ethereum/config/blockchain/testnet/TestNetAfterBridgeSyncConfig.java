/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.ethereum.config.blockchain.testnet;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.BlockDifficulty;
import org.ethereum.config.Constants;
import org.ethereum.config.blockchain.GenesisConfig;
import org.ethereum.core.BlockHeader;

import java.math.BigInteger;


public class TestNetAfterBridgeSyncConfig extends GenesisConfig {


    public static class TestNetConstants extends GenesisConstants {

        private static final BigInteger DIFFICULTY_BOUND_DIVISOR = BigInteger.valueOf(5);
        private static final byte CHAIN_ID = 51;
        private final BlockDifficulty minimumDifficulty = new BlockDifficulty(BigInteger.valueOf(896));

        @Override
        public BridgeConstants getBridgeConstants() {
            return BridgeTestNetConstants.getInstance();
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

        @Override
        public int getNewBlockMaxSecondsInTheFuture() {
            return 540;
        }

        @Override
        public byte getChainId() {
            return TestNetConstants.CHAIN_ID;
        }

    }

    public TestNetAfterBridgeSyncConfig() {
        super(new TestNetConstants());
    }

    protected TestNetAfterBridgeSyncConfig(Constants constants) {
        super(constants);
    }


    @Override
    public BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent) {

        // Created Fork at Thursday, August 16, 2018 3:00:00 PM GMT+08:00
        // Changed 10 minutes to 2.5 minutes

        // If more than 10 minutes, reset to original difficulty 0x00100000
        if(curBlock.getTimestamp() < 1534402800l) {
            if (curBlock.getTimestamp() >= parent.getTimestamp() + 600) {
                return getConstants().getMinimumDifficulty();
            }
        }
        else {
            if (curBlock.getTimestamp() >= parent.getTimestamp() + 150) {
                return getConstants().getMinimumDifficulty();
            }
        }

        return super.calcDifficulty(curBlock, parent);
    }
}
