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

package org.ethereum.core.genesis;

import co.usc.config.UscSystemProperties;
import co.usc.core.BlockDifficulty;
import co.usc.core.UscAddress;
import co.usc.core.bc.BlockChainImpl;
import co.usc.core.bc.BlockExecutor;
import co.usc.validators.BlockValidator;
import org.apache.commons.lang3.StringUtils;
import org.ethereum.core.*;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.ethereum.listener.EthereumListener;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;

/**
 * Created by mario on 13/01/17.
 */
@Component
public class BlockChainLoader {

    private static final Logger logger = LoggerFactory.getLogger("general");

    private final UscSystemProperties config;
    private final BlockStore blockStore;
    private final Repository repository;
    private final ReceiptStore receiptStore;
    private final TransactionPool transactionPool;
    private final EthereumListener listener;
    private final BlockValidator blockValidator;

    @Autowired
    public BlockChainLoader(
            UscSystemProperties config,
            org.ethereum.core.Repository repository,
            org.ethereum.db.BlockStore blockStore,
            ReceiptStore receiptStore,
            TransactionPool transactionPool,
            EthereumListener listener,
            BlockValidator blockValidator) {

        this.config = config;
        this.blockStore = blockStore;
        this.repository = repository;
        this.receiptStore = receiptStore;
        this.transactionPool = transactionPool;
        this.listener = listener;
        this.blockValidator = blockValidator;
    }

    public BlockChainImpl loadBlockchain() {
        final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();
        BlockChainImpl blockchain = new BlockChainImpl(
                repository,
                blockStore,
                receiptStore,
                transactionPool,
                listener,
                blockValidator,
                config.isFlushEnabled(),
                config.flushNumberOfBlocks(),
                new BlockExecutor(
                    repository,
                        (tx1, txindex1, coinbase, track1, block1, totalGasUsed1) -> new TransactionExecutor(
                            tx1,
                            txindex1,
                            block1.getCoinbase(),
                            track1,
                            blockStore,
                            receiptStore,
                            programInvokeFactory,
                            block1,
                            listener,
                            totalGasUsed1,
                            config.getVmConfig(),
                            config.getBlockchainConfig(),
                            config.playVM(),
                            config.isRemascEnabled(),
                            config.vmTrace(),
                            new PrecompiledContracts(config),
                            config.databaseDir(),
                            config.vmTraceDir(),
                            config.vmTraceCompressed()
                        )
                )
        );

        Block bestBlock = blockStore.getBestBlock();
        if (bestBlock == null) {
            logger.info("DB is empty - adding Genesis");

            BigInteger initialNonce = config.getBlockchainConfig().getCommonConstants().getInitialNonce();
            Genesis genesis = GenesisLoader.loadGenesis(config, config.genesisInfo(), initialNonce, true);
            for (UscAddress addr : genesis.getPremine().keySet()) {
                repository.createAccount(addr);
                InitialAddressState initialAddressState = genesis.getPremine().get(addr);
                repository.addBalance(addr, initialAddressState.getAccountState().getBalance());
                AccountState accountState = repository.getAccountState(addr);
                accountState.setNonce(initialAddressState.getAccountState().getNonce());

                if (initialAddressState.getContractDetails()!=null) {
                    repository.updateContractDetails(addr, initialAddressState.getContractDetails());
                    accountState.setStateRoot(initialAddressState.getAccountState().getStateRoot());
                    accountState.setCodeHash(initialAddressState.getAccountState().getCodeHash());
                }

                repository.updateAccountState(addr, accountState);
            }

            genesis.setStateRoot(repository.getRoot());
            genesis.flushRLP();

            blockStore.saveBlock(genesis, genesis.getCumulativeDifficulty(), true);
            blockchain.setBestBlock(genesis);
            blockchain.setTotalDifficulty(genesis.getCumulativeDifficulty());

            listener.onBlock(genesis, new ArrayList<TransactionReceipt>() );
            repository.dumpState(genesis, 0, 0, null);

            logger.info("Genesis block loaded");
        } else {
            BlockDifficulty totalDifficulty = blockStore.getTotalDifficultyForHash(bestBlock.getHash().getBytes());

            blockchain.setBestBlock(bestBlock);
            blockchain.setTotalDifficulty(totalDifficulty);

            logger.info("*** Loaded up to block [{}] totalDifficulty [{}] with stateRoot [{}]",
                    blockchain.getBestBlock().getNumber(),
                    blockchain.getTotalDifficulty().toString(),
                    Hex.toHexString(blockchain.getBestBlock().getStateRoot()));
        }

        String rootHash = config.rootHashStart();
        if (StringUtils.isNotBlank(rootHash)) {

            // update world state by dummy hash
            byte[] rootHashArray = Hex.decode(rootHash);
            logger.info("Loading root hash from property file: [{}]", rootHash);
            this.repository.syncToRoot(rootHashArray);

        } else {

            // Update world state to latest loaded block from db
            // if state is not generated from empty premine list
            // todo this is just a workaround, move EMPTY_TRIE_HASH logic to Trie implementation
            if (!Arrays.equals(blockchain.getBestBlock().getStateRoot(), EMPTY_TRIE_HASH)) {
                this.repository.syncToRoot(blockchain.getBestBlock().getStateRoot());
            }
        }
        return blockchain;
    }
}
