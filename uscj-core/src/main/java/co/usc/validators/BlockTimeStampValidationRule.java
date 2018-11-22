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

package co.usc.validators;

import co.usc.panic.PanicProcessor;
import org.ethereum.core.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mario on 23/01/17.
 */
public class BlockTimeStampValidationRule implements BlockParentDependantValidationRule, BlockValidationRule{

    private static final Logger logger = LoggerFactory.getLogger("blockvalidator");
    private static final PanicProcessor panicProcessor = new PanicProcessor();


    private int validPeriodLength;

    public BlockTimeStampValidationRule(int validPeriodLength) {
        this.validPeriodLength = validPeriodLength;
    }

    @Override
    public boolean isValid(Block block) {
        if (this.validPeriodLength == 0) {
            return true;
        }

        final long currentTime = System.currentTimeMillis() / 1000;
        final long blockTime = block.getTimestamp();

        boolean result = blockTime - currentTime <= this.validPeriodLength;

        if(!result) {
            logger.warn("Error validating block. Invalid timestamp {}.", blockTime);
        }

        return result;
    }

    @Override
    public boolean isValid(Block block, Block parent) {
        if (this.validPeriodLength == 0) {
            return true;
        }

        boolean result = this.isValid(block);

        final long blockTime = block.getTimestamp();
        final long parentTime = parent.getTimestamp();
        result = result && (blockTime > parentTime);

        if (!result) {
            logger.warn("Error validating block. Invalid timestamp {} for parent timestamp {}", blockTime, parentTime);
        }

        return result;
    }
}
