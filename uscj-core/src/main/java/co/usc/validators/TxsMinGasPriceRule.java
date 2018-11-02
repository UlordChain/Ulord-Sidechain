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

import co.usc.core.Coin;
import co.usc.panic.PanicProcessor;
import co.usc.remasc.RemascTransaction;
import org.apache.commons.collections4.CollectionUtils;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by mario on 26/12/16.
 */
public class TxsMinGasPriceRule implements BlockValidationRule {

    private static final Logger logger = LoggerFactory.getLogger("blockvalidator");
    private static final PanicProcessor panicProcessor = new PanicProcessor();

    @Override
    public boolean isValid(Block block) {
        List<Transaction> txs = block.getTransactionsList();
        if(block.getMinimumGasPrice() == null) {
            logger.warn("Could not retrieve block min gas priceß");
            return false;
        }

        Coin blockMgp = block.getMinimumGasPrice();
        if(CollectionUtils.isNotEmpty(block.getTransactionsList())) {
            for (Transaction tx : txs) {
                if (!(tx instanceof RemascTransaction) && tx.getGasPrice().compareTo(blockMgp) < 0) {
                    logger.warn("Tx gas price is under the Min gas Price of the block tx={}", tx.getHash());
                    return false;
                }
            }
        }

        return true;
    }
}
