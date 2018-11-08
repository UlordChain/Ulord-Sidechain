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

package org.ethereum.config.blockchain.regtest;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.core.BlockDifficulty;
import org.ethereum.config.blockchain.GenesisConfig;

import java.math.BigInteger;

public class RegTestGenesisConfig extends GenesisConfig {

    public static class RegTestConstants extends GenesisConstants {

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
