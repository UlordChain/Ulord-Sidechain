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

package org.ethereum.config;

import co.usc.core.BlockDifficulty;
import org.ethereum.core.BlockHeader;

/**
 * Describes constants and algorithms used for a specific blockchain at specific stage
 *
 * Created by Anton Nashatyrev on 25.02.2016.
 */
public interface BlockchainConfig {

    /**
     * Get blockchain constants
     */
    Constants getConstants();

    /**
     * Calculates the difficulty for the block depending on the parent
     */
    BlockDifficulty calcDifficulty(BlockHeader curBlock, BlockHeader parent);

    boolean areBridgeTxsFree();

    boolean isUscIP85();

    boolean isUscIP87();

    boolean isUscIP88();

    boolean isUscIP89();

    boolean isUscIP90();

    boolean isUscIP91();

    boolean isUscIP92();

    boolean isUscIP93();

    boolean isUscIP94();

    boolean isUscIP98();

}
