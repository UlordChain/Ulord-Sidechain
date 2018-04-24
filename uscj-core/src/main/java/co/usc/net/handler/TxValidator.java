/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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

import co.usc.config.RskSystemProperties;
import co.usc.core.Coin;
import co.usc.core.RskAddress;
import co.usc.net.handler.txvalidator.*;
import co.usc.net.handler.txvalidator.*;
import org.ethereum.core.AccountState;
import org.ethereum.core.Blockchain;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.rpc.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Used to validate transactions before relaying. This class is highly
 * coupled with TxHandlerImpl. Check that class before modifying. Think
 * twice before reusing.
 */
class TxValidator {

    private static final Logger logger = LoggerFactory.getLogger("txvalidator");

    private final RskSystemProperties config;
    private final Repository repository;
    private final Blockchain blockchain;

    private final List<TxValidatorStep> validatorSteps = new LinkedList<>();
    private final List<TxFilter> txFilters = new LinkedList<>();

    public TxValidator(RskSystemProperties config, Repository repository, Blockchain blockchain) {
        this.config = config;
        this.repository = repository;
        this.blockchain = blockchain;
        validatorSteps.add(new TxValidatorAccountStateValidator());
        validatorSteps.add(new TxValidatorNonceRangeValidator());
        validatorSteps.add(new TxValidatorGasLimitValidator());
        validatorSteps.add(new TxValidatorAccountBalanceValidator());
        validatorSteps.add(new TxValidatorMinimuGasPriceValidator());
        validatorSteps.add(new TxValidatorIntrinsicGasLimitValidator(config));

        txFilters.add(new TxFilterAccumCostFilter(config));
    }

    /**
     * Where the magic occurs, will filter out invalid txs
     */
    List<Transaction> filterTxs(List<Transaction> txs) {
        List<Transaction> acceptedTxs = new LinkedList<>();

        for (Transaction tx : txs) {
            String hash = tx.getHash().toJsonString();

            AccountState state = repository.getAccountState(tx.getSender());

            if (state == null) {
                state = new AccountState();
            }

            BigInteger blockGasLimit = BigIntegers.fromUnsignedByteArray(blockchain.getBestBlock().getGasLimit());
            Coin minimumGasPrice = blockchain.getBestBlock().getMinimumGasPrice();
            long bestBlockNumber = blockchain.getBestBlock().getNumber();
            long basicTxCost = tx.transactionCost(config, blockchain.getBestBlock());

            boolean valid = true;

            for (TxValidatorStep step : validatorSteps) {
                if (!step.validate(tx, state, blockGasLimit, minimumGasPrice, bestBlockNumber, basicTxCost == 0)) {
                    logger.info("Tx validation failed: validator {} tx={}", step.getClass().getName(), tx.getHash());
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                continue;
            }

            acceptedTxs.add(tx);
        }

        return acceptedTxs;
    }
}
