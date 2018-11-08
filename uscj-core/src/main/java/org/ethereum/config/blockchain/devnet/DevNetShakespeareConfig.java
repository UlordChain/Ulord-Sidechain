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

package org.ethereum.config.blockchain.devnet;

import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;

public class DevNetShakespeareConfig extends DevNetGenesisConfig {

    @Override
    public boolean isUscIP85() {
        return true;
    }

    @Override
    public boolean isUscIP87() { return true; }

    @Override
    public boolean isUscIP88() { return true; }

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
