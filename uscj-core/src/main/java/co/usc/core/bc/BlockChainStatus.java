/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
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

package co.usc.core.bc;

import co.usc.core.BlockDifficulty;
import co.usc.net.Status;
import org.ethereum.core.Block;

/**
 * Created by ajlopez on 29/07/2016.
 */

public class BlockChainStatus {
    private final Block bestBlock;
    private final BlockDifficulty totalDifficulty;

    public BlockChainStatus(Block bestBlock, BlockDifficulty totalDifficulty) {
        this.bestBlock = bestBlock;
        this.totalDifficulty = totalDifficulty;
    }

    public Block getBestBlock() {
        return bestBlock;
    }

    public long getBestBlockNumber() {
        return bestBlock.getNumber();
    }

    public BlockDifficulty getTotalDifficulty() {
        return totalDifficulty;
    }

    public boolean hasLowerTotalDifficultyThan(Status status) {
        return this.totalDifficulty.compareTo(status.getTotalDifficulty()) < 0;
    }
}
