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
import co.usc.remasc.RemascTransaction;
import org.apache.commons.collections4.CollectionUtils;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by mario on 30/12/16.
 */
public class RemascValidationRule implements BlockValidationRule{

    private static final Logger logger = LoggerFactory.getLogger("blockvalidator");
    private static final PanicProcessor panicProcessor = new PanicProcessor();


    @Override
    public boolean isValid(Block block) {
        List<Transaction> txs = block.getTransactionsList();
        boolean result = CollectionUtils.isNotEmpty(txs) && (txs.get(txs.size()-1) instanceof RemascTransaction);
        if(!result) {
            logger.warn("Remasc tx not found in block");
            panicProcessor.panic("invalidremasctx", "Remasc tx not found in block");
        }
        return result;
    }
}
