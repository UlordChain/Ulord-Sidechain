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

package co.usc.test.builders;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import co.usc.config.UscSystemProperties;
import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.core.bc.*;
import co.usc.db.RepositoryImpl;
import co.usc.peg.RepositoryBlockStore;
import co.usc.trie.TrieStoreImpl;
import co.usc.validators.BlockValidator;
import co.usc.validators.DummyBlockValidator;
import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import org.ethereum.core.*;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.db.*;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.TestCompositeEthereumListener;
import org.ethereum.manager.AdminInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.junit.Assert;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ajlopez on 8/6/2016.
 */
public class BlockChainBuilder {
    private final TestSystemProperties config = new TestSystemProperties();
    private boolean testing;

    private List<Block> blocks;
    private List<TransactionInfo> txinfos;

    private AdminInfo adminInfo;
    private Repository repository;
    private BlockStore blockStore;
    private Genesis genesis;
    private ReceiptStore receiptStore;

    public BlockChainBuilder setAdminInfo(AdminInfo adminInfo) {
        this.adminInfo = adminInfo;
        return this;
    }

    public BlockChainBuilder setTesting(boolean value) {
        this.testing = value;
        return this;
    }

    public BlockChainBuilder setBlocks(List<Block> blocks) {
        this.blocks = blocks;
        return this;
    }

    public BlockChainBuilder setRepository(Repository repository) {
        this.repository = repository;
        return this;
    }

    public BlockChainBuilder setBlockStore(BlockStore blockStore) {
        this.blockStore = blockStore;
        return this;
    }

    public BlockChainBuilder setTransactionInfos(List<TransactionInfo> txinfos) {
        this.txinfos = txinfos;
        return this;
    }

    public BlockChainBuilder setGenesis(Genesis genesis) {
        this.genesis = genesis;
        return this;
    }

    public BlockChainBuilder setReceiptStore(ReceiptStore receiptStore) {
        this.receiptStore = receiptStore;
        return this;
    }

    public UscSystemProperties getConfig() {
        return config;
    }

    public BlockChainImpl build() {
        return build(false);
    }

    public BlockChainImpl build(boolean withoutCleaner) {
        if (repository == null)
            repository = new RepositoryImpl(config, new TrieStoreImpl(new HashMapDB().setClearOnClose(false)));

        if (blockStore == null) {
            blockStore = new IndexedBlockStore(new HashMap<>(), new HashMapDB(), null);
        }

        if (receiptStore == null) {
            KeyValueDataSource ds = new HashMapDB();
            ds.init();
            receiptStore = new ReceiptStoreImpl(ds);
        }

        if (txinfos != null && !txinfos.isEmpty())
            for (TransactionInfo txinfo : txinfos)
                receiptStore.add(txinfo.getBlockHash(), txinfo.getIndex(), txinfo.getReceipt());

        EthereumListener listener = new BlockExecutorTest.SimpleEthereumListener();

        BlockValidatorBuilder validatorBuilder = new BlockValidatorBuilder();

        validatorBuilder.addBlockRootValidationRule().addBlockUnclesValidationRule(blockStore)
                .addBlockTxsValidationRule(repository).blockStore(blockStore);

        BlockValidator blockValidator = validatorBuilder.build();

        if (this.adminInfo == null)
            this.adminInfo = new AdminInfo();


        TransactionPoolImpl transactionPool;
        if (withoutCleaner) {
            transactionPool = new TransactionPoolImplNoCleaner(config, this.repository, this.blockStore, receiptStore, new ProgramInvokeFactoryImpl(), new TestCompositeEthereumListener(), 10, 100);
        } else {
            transactionPool = new TransactionPoolImpl(config, this.repository, this.blockStore, receiptStore, new ProgramInvokeFactoryImpl(), new TestCompositeEthereumListener(), 10, 100);
        }

        final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();
        BlockChainImpl blockChain = new BlockChainImpl(this.repository, this.blockStore, receiptStore, transactionPool, listener, blockValidator, false, 1, new BlockExecutor(this.repository, (tx1, txindex1, coinbase, track1, block1, totalGasUsed1) -> new TransactionExecutor(
                tx1,
                txindex1,
                block1.getCoinbase(),
                track1,
                this.blockStore,
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
        )));

        if (this.testing) {
            blockChain.setBlockValidator(new DummyBlockValidator());
            blockChain.setNoValidation(true);
        }

        if (this.genesis != null) {
            for (UscAddress addr : this.genesis.getPremine().keySet()) {
                this.repository.createAccount(addr);
                this.repository.addBalance(addr, this.genesis.getPremine().get(addr).getAccountState().getBalance());
            }

            Repository track = this.repository.startTracking();
            new RepositoryBlockStore(config, track, PrecompiledContracts.BRIDGE_ADDR);
            track.commit();

            this.genesis.setStateRoot(this.repository.getRoot());
            this.genesis.flushRLP();
            blockChain.setBestBlock(this.genesis);

            blockChain.setTotalDifficulty(this.genesis.getCumulativeDifficulty());
        }

        if (this.blocks != null) {
            final ProgramInvokeFactoryImpl programInvokeFactory1 = new ProgramInvokeFactoryImpl();
            BlockExecutor blockExecutor = new BlockExecutor(repository, (tx1, txindex1, coinbase, track1, block1, totalGasUsed1) -> new TransactionExecutor(
                    tx1,
                    txindex1,
                    block1.getCoinbase(),
                    track1,
                    blockStore,
                    receiptStore,
                    programInvokeFactory1,
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
            ));

            for (Block b : this.blocks) {
                blockExecutor.executeAndFillAll(b, blockChain.getBestBlock());
                blockChain.tryToConnect(b);
            }
        }

        return blockChain;
    }

    public static Blockchain ofSizeWithNoTransactionPoolCleaner(int size) {
        return ofSize(size, false, true);
    }

    public static Blockchain ofSize(int size) {
        return ofSize(size, false, false);
    }

    public static Blockchain ofSize(int size, boolean mining) {
        return ofSize(size, mining, null, null, false);
    }

    public static Blockchain ofSize(int size, boolean mining, boolean withoutCleaner) {
        return ofSize(size, mining, null, null, withoutCleaner);
    }

    public static Blockchain ofSize(int size, boolean mining, List<Account> accounts, List<Coin> balances) {
        return ofSize(size, mining, accounts, balances, false);
    }

    public static Blockchain ofSize(int size, boolean mining, List<Account> accounts, List<Coin> balances, boolean withoutCleaner) {
        BlockChainBuilder builder = new BlockChainBuilder();
        BlockChainImpl blockChain = builder.build(withoutCleaner);

        BlockGenerator blockGenerator = new BlockGenerator();
        Block genesis = blockGenerator.getGenesisBlock();

        if (accounts != null)
            for (int k = 0; k < accounts.size(); k++) {
                Account account = accounts.get(k);
                Coin balance = balances.get(k);
                blockChain.getRepository().createAccount(account.getAddress());
                blockChain.getRepository().addBalance(account.getAddress(), balance);
            }

        genesis.setStateRoot(blockChain.getRepository().getRoot());
        genesis.flushRLP();

        Assert.assertEquals(ImportResult.IMPORTED_BEST, blockChain.tryToConnect(genesis));

        if (size > 0) {
            List<Block> blocks = mining ? blockGenerator.getMinedBlockChain(genesis, size) : blockGenerator.getBlockChain(genesis, size);

            for (Block block: blocks)
                Assert.assertEquals(ImportResult.IMPORTED_BEST, blockChain.tryToConnect(block));
        }

        return blockChain;
    }

    public static Blockchain copy(Blockchain original) {
        BlockChainBuilder builder = new BlockChainBuilder();
        BlockChainImpl blockChain = builder.build();

        long height = original.getStatus().getBestBlockNumber();

        for (long k = 0; k <= height; k++)
            blockChain.tryToConnect(original.getBlockByNumber(k));

        return blockChain;
    }

    public static Blockchain copyAndExtend(Blockchain original, int size) {
        return copyAndExtend(original, size, false);
    }

    public static Blockchain copyAndExtend(Blockchain original, int size, boolean mining) {
        Blockchain blockchain = copy(original);
        extend(blockchain, size, false, mining);
        return blockchain;
    }

    public static void extend(Blockchain blockchain, int size, boolean withUncles, boolean mining) {
        Block initial = blockchain.getBestBlock();
        List<Block> blocks = new BlockGenerator().getBlockChain(initial, size, 0, withUncles, mining, null);

        for (Block block: blocks)
            blockchain.tryToConnect(block);
    }
}
