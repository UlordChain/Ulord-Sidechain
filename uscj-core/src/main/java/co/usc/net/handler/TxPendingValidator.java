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

package co.usc.net.handler;

import co.usc.config.UscSystemProperties;
import co.usc.core.Coin;
import co.usc.net.handler.txvalidator.*;
import org.ethereum.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.BigIntegers;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * Validator for using in pending state.
 *
 * Add/remove checks here.
 */
public class TxPendingValidator {
    private static final Logger logger = LoggerFactory.getLogger("txpendingvalidator");

    private final List<TxValidatorStep> validatorSteps = new LinkedList<>();

    private final UscSystemProperties config;

    public TxPendingValidator(UscSystemProperties config) {
        this.config = config;

        validatorSteps.add(new TxNotNullValidator());
        validatorSteps.add(new TxValidatorNotRemascTxValidator());
        validatorSteps.add(new TxValidatorGasLimitValidator());
        validatorSteps.add(new TxValidatorAccountStateValidator());
        validatorSteps.add(new TxValidatorNonceRangeValidator());
        validatorSteps.add(new TxValidatorAccountBalanceValidator());
        validatorSteps.add(new TxValidatorMinimuGasPriceValidator());
        validatorSteps.add(new TxValidatorIntrinsicGasLimitValidator(config));
    }

    public boolean isValid(Transaction tx, Block executionBlock, @Nullable AccountState state) {
        BigInteger blockGasLimit = BigIntegers.fromUnsignedByteArray(executionBlock.getGasLimit());
        Coin minimumGasPrice = executionBlock.getMinimumGasPrice();
        long bestBlockNumber = executionBlock.getNumber();
        long basicTxCost = tx.transactionCost(executionBlock, config.getBlockchainConfig());

        if (state == null && basicTxCost != 0) {
            logger.warn("[tx={}, sender={}] account doesn't exist", tx.getHash(), tx.getSender());
            return false;
        }

        for (TxValidatorStep step : validatorSteps) {
            if (!step.validate(tx, state, blockGasLimit, minimumGasPrice, bestBlockNumber, basicTxCost == 0)) {
                logger.warn("[tx={}] {} failed", tx.getHash(), step.getClass());
                return false;
            }
        }

        return true;
    }
}
