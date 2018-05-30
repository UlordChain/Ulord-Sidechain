/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
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

package co.usc.mine;

import co.usc.config.MiningConfig;
import co.usc.config.UscSystemProperties;
import co.usc.core.Coin;
import co.usc.core.DifficultyCalculator;
import co.usc.core.UscAddress;
import co.usc.core.bc.BlockExecutor;
import co.usc.core.bc.FamilyUtils;
import co.usc.remasc.RemascTransaction;
import co.usc.validators.BlockValidationRule;
import org.apache.commons.collections4.CollectionUtils;
import org.ethereum.core.*;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Clock;
import java.util.*;

/**
 * This component helps build a new block to mine.
 * It can also be used to generate a new block from the pending state, which is useful
 * in places like Web3 with the 'pending' parameter.
 */
@Component
public class BlockToMineBuilder {
    private static final Logger logger = LoggerFactory.getLogger("blocktominebuilder");

    private final MiningConfig miningConfig;
    private final Repository repository;
    private final BlockStore blockStore;
    private final TransactionPool transactionPool;
    private final DifficultyCalculator difficultyCalculator;
    private final GasLimitCalculator gasLimitCalculator;
    private final BlockValidationRule validationRules;

    private final Clock clock;
    private final MinimumGasPriceCalculator minimumGasPriceCalculator;
    private final MinerUtils minerUtils;
    private final BlockExecutor executor;

    private final Coin minerMinGasPriceTarget;

    private long timeAdjustment;
    private long minimumAcceptableTime;

    @Autowired
    public BlockToMineBuilder(
            MiningConfig miningConfig,
            Repository repository,
            BlockStore blockStore,
            TransactionPool transactionPool,
            DifficultyCalculator difficultyCalculator,
            GasLimitCalculator gasLimitCalculator,
            @Qualifier("minerServerBlockValidation") BlockValidationRule validationRules,
            UscSystemProperties config,
            ReceiptStore receiptStore) {
        this.miningConfig = Objects.requireNonNull(miningConfig);
        this.repository = Objects.requireNonNull(repository);
        this.blockStore = Objects.requireNonNull(blockStore);
        this.transactionPool = Objects.requireNonNull(transactionPool);
        this.difficultyCalculator = Objects.requireNonNull(difficultyCalculator);
        this.gasLimitCalculator = Objects.requireNonNull(gasLimitCalculator);
        this.validationRules = Objects.requireNonNull(validationRules);

        this.clock = Clock.systemUTC();
        this.minimumGasPriceCalculator = new MinimumGasPriceCalculator();
        this.minerUtils = new MinerUtils();
        this.executor = new BlockExecutor(config, repository, receiptStore, blockStore, null);

        this.minerMinGasPriceTarget = Coin.valueOf(miningConfig.getMinGasPriceTarget());
    }

    /**
     * build creates a block to mine based on the given block as parent.
     *
     * @param newBlockParent the new block parent.
     * @param extraData      extra data to pass to the block being built
     */
    public Block build(Block newBlockParent, byte[] extraData) {

	//Get the Uncles' info
        List<BlockHeader> uncles = FamilyUtils.getUnclesHeaders(
                blockStore,
                newBlockParent.getNumber() + 1,
                newBlockParent.getHash().getBytes(),
                miningConfig.getUncleGenerationLimit()
        );


        if (uncles.size() > miningConfig.getUncleListLimit()) {
            uncles = uncles.subList(0, miningConfig.getUncleListLimit());
        }

       //get the MinGasPrice
        Coin minimumGasPrice = minimumGasPriceCalculator.calculate(
                newBlockParent.getMinimumGasPrice(),
                minerMinGasPriceTarget
        );

       //Get the txs
        final List<Transaction> txsToRemove = new ArrayList<>();
        final List<Transaction> txs = getTransactions(txsToRemove, newBlockParent, minimumGasPrice);
        minimumAcceptableTime = newBlockParent.getTimestamp() + 1;

       //create the new block
        final Block newBlock = createBlock(newBlockParent, uncles, txs, minimumGasPrice);

        newBlock.setExtraData(extraData);
        removePendingTransactions(txsToRemove);
        executor.executeAndFill(newBlock, newBlockParent);
        return newBlock;
    }

    private List<Transaction> getTransactions(List<Transaction> txsToRemove, Block parent, Coin minGasPrice) {
        logger.debug("getting transactions from pending state");
        List<Transaction> txs = minerUtils.getAllTransactions(transactionPool);
        logger.debug("{} transaction(s) collected from pending state", txs.size());

        Transaction remascTx = new RemascTransaction(parent.getNumber() + 1);
        txs.add(remascTx);

        Map<UscAddress, BigInteger> accountNonces = new HashMap<>();

        Repository originalRepo = repository.getSnapshotTo(parent.getStateRoot());

        return minerUtils.filterTransactions(txsToRemove, txs, accountNonces, originalRepo, minGasPrice);
    }

    private void removePendingTransactions(List<Transaction> transactions) {
        transactionPool.removeTransactions(transactions);
    }

    private Block createBlock(
            Block newBlockParent,
            List<BlockHeader> uncles,
            List<Transaction> txs,
            Coin minimumGasPrice) {
        final BlockHeader newHeader = createHeader(newBlockParent, uncles, txs, minimumGasPrice);
        final Block newBlock = new Block(newHeader, txs, uncles);
        return validationRules.isValid(newBlock) ? newBlock : new Block(newHeader, txs, null);
    }

    private BlockHeader createHeader(
            Block newBlockParent,
            List<BlockHeader> uncles,
            List<Transaction> txs,
            Coin minimumGasPrice) {
        final byte[] unclesListHash = HashUtil.keccak256(BlockHeader.getUnclesEncodedEx(uncles));

        final long timestampSeconds = this.getCurrentTimeInSeconds();

        // Set gas limit before executing block
        BigInteger minGasLimit = BigInteger.valueOf(miningConfig.getGasLimit().getMininimum());
        BigInteger targetGasLimit = BigInteger.valueOf(miningConfig.getGasLimit().getTarget());
        BigInteger parentGasLimit = new BigInteger(1, newBlockParent.getGasLimit());
        BigInteger gasUsed = BigInteger.valueOf(newBlockParent.getGasUsed());
        boolean forceLimit = miningConfig.getGasLimit().isTargetForced();
        BigInteger gasLimit = gasLimitCalculator.calculateBlockGasLimit(parentGasLimit,
                                                                        gasUsed, minGasLimit, targetGasLimit, forceLimit);

        final BlockHeader newHeader = new BlockHeader(
                newBlockParent.getHash().getBytes(),
                unclesListHash,
                miningConfig.getCoinbaseAddress().getBytes(),
                new Bloom().getData(),
                new byte[]{1},
                newBlockParent.getNumber() + 1,
                gasLimit.toByteArray(),
                0,
                timestampSeconds,
                new byte[]{},
                new byte[]{},
                new byte[]{},
                new byte[]{},
                minimumGasPrice.getBytes(),
                CollectionUtils.size(uncles)
        );
        newHeader.setDifficulty(difficultyCalculator.calcDifficulty(newHeader, newBlockParent.getHeader()));
        newHeader.setTransactionsRoot(Block.getTxTrie(txs).getHash().getBytes());
        return newHeader;
    }

    // Note that this needs to be refactored.
    public long getCurrentTimeInSeconds() {
        long ret = clock.millis() / 1000 + timeAdjustment;
        return Long.max(ret, minimumAcceptableTime);
    }

    // Note that this needs to be refactored.
    public long increaseTime(long seconds) {
        if (seconds <= 0) {
            return timeAdjustment;
        }

        timeAdjustment += seconds;
        return timeAdjustment;
    }
}
