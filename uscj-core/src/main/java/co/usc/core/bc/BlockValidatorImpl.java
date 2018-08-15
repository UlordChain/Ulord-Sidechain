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

import co.usc.validators.BlockParentDependantValidationRule;
import co.usc.validators.BlockValidationRule;
import co.usc.validators.BlockValidator;
import org.ethereum.core.Block;
import org.ethereum.db.BlockStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * BlockValidator has methods to validate block content before its execution
 *
 * Created by ajlopez on 29/07/2016.
 */
@Component
public class BlockValidatorImpl implements BlockValidator {

    private BlockStore blockStore;

    private BlockParentDependantValidationRule blockParentValidator;

    private BlockValidationRule blockValidator;

    @Autowired
    public BlockValidatorImpl(BlockStore blockStore, BlockParentDependantValidationRule blockParentValidator, @Qualifier("blockValidationRule") BlockValidationRule blockValidator) {
        this.blockStore = blockStore;
        this.blockParentValidator = blockParentValidator;
        this.blockValidator = blockValidator;
    }

    /**
     * Validate a block.
     * The validation includes
     * - Validate the header data relative to parent block
     * - Validate the transaction root hash to transaction list
     * - Validate uncles
     * - Validate transactions
     *
     * @param block        Block to validate
     * @return true if the block is valid, false if the block is invalid
     */
    @Override
    public boolean isValid(Block block) {
        if (block.isGenesis()) {
            return true;
        }

        Block parent = getParent(block);

        if(!this.blockParentValidator.isValid(block, parent)) {
            return false;
        }

        if(!this.blockValidator.isValid(block)) {
            return false;
        }

        return true;
    }

    private Block getParent(Block block) {
        if (this.blockStore == null) {
            return null;
        }

        return blockStore.getBlockByHash(block.getParentHash().getBytes());
    }
}

