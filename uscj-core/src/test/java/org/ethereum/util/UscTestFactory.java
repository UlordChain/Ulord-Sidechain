package org.ethereum.util;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import co.usc.core.Coin;
import co.usc.core.ReversibleTransactionExecutor;
import co.usc.core.UscAddress;
import co.usc.core.UscImpl;
import co.usc.core.bc.BlockChainImpl;
import co.usc.core.bc.BlockExecutor;
import co.usc.core.bc.TransactionPoolImpl;
import co.usc.db.RepositoryImpl;
import co.usc.db.TrieStorePoolOnMemory;
import co.usc.net.BlockNodeInformation;
import co.usc.net.BlockSyncService;
import co.usc.net.NodeBlockProcessor;
import co.usc.net.sync.SyncConfiguration;
import co.usc.test.builders.AccountBuilder;
import co.usc.test.builders.TransactionBuilder;
import co.usc.trie.TrieStoreImpl;
import co.usc.validators.DummyBlockValidator;
import org.ethereum.core.*;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.db.*;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.listener.TestCompositeEthereumListener;
import org.ethereum.rpc.TypeConverter;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * This is the test version of {@link co.usc.core.UscFactory}, but without Spring.
 *
 * We try to recreate the objects used in production as best as we can,
 * replacing persistent storage with in-memory storage.
 * There are many nulls in place of objects that aren't part of our
 * tests yet.
 */
public class UscTestFactory {
    private final TestSystemProperties config = new TestSystemProperties();
    private BlockChainImpl blockchain;
    private IndexedBlockStore blockStore;
    private TransactionPool transactionPool;
    private RepositoryImpl repository;
    private ProgramInvokeFactoryImpl programInvokeFactory;
    private ReversibleTransactionExecutor reversibleTransactionExecutor;
    private NodeBlockProcessor blockProcessor;
    private UscImpl uscImpl;
    private CompositeEthereumListener compositeEthereumListener;
    private ReceiptStoreImpl receiptStore;

    public UscTestFactory() {
        Genesis genesis = new BlockGenerator().getGenesisBlock();
        genesis.setStateRoot(getRepository().getRoot());
        genesis.flushRLP();
        getBlockchain().setBestBlock(genesis);
        getBlockchain().setTotalDifficulty(genesis.getCumulativeDifficulty());
    }

    public ContractDetails addContract(String runtimeBytecode) {
        Account contractAccount = new AccountBuilder(getBlockchain())
                .name(runtimeBytecode)
                .balance(Coin.valueOf(10))
                .code(TypeConverter.stringHexToByteArray(runtimeBytecode))
                .build();

        return getRepository().getContractDetails(contractAccount.getAddress());
    }

    public ProgramResult executeRawContract(byte[] bytecode, byte[] encodedCall, BigInteger value) {
        Account sender = new AccountBuilder(getBlockchain())
                .name("sender")
                // a large balance will allow running any contract
                .balance(Coin.valueOf(10000000L))
                .build();
        BigInteger nonceCreate = getRepository().getNonce(sender.getAddress());
        Transaction creationTx = new TransactionBuilder()
                .gasLimit(BigInteger.valueOf(3000000))
                .sender(sender)
                .data(bytecode)
                .nonce(nonceCreate.longValue())
                .build();
        executeTransaction(creationTx);
        BigInteger nonceExecute = getRepository().getNonce(sender.getAddress());
        Transaction transaction = new TransactionBuilder()
                // a large gas limit will allow running any contract
                .gasLimit(BigInteger.valueOf(3000000))
                .sender(sender)
                .receiverAddress(creationTx.getContractAddress().getBytes())
                .data(encodedCall)
                .nonce(nonceExecute.longValue())
                .value(value)
                .build();
        return executeTransaction(transaction).getResult();
    }

    private TransactionExecutor executeTransaction(Transaction transaction) {
        Repository track = getRepository().startTracking();
        TransactionExecutor executor = new TransactionExecutor(
                transaction,
                0,
                UscAddress.nullAddress(),
                getRepository(),
                getBlockStore(),
                getReceiptStore(),
                getProgramInvokeFactory(),
                getBlockchain().getBestBlock(),
                new EthereumListenerAdapter(),
                0,
                config.getVmConfig(),
                config.getBlockchainConfig(),
                config.playVM(),
                config.isRemascEnabled(),
                config.vmTrace(),
                new PrecompiledContracts(config),
                config.databaseDir(),
                config.vmTraceDir(),
                config.vmTraceCompressed()
        );
        executor.init();
        executor.execute();
        executor.go();
        executor.finalization();
        track.commit();
        return executor;
    }

    private ProgramInvokeFactoryImpl getProgramInvokeFactory() {
        if (programInvokeFactory == null) {
            programInvokeFactory = new ProgramInvokeFactoryImpl();
        }

        return programInvokeFactory;
    }

    public BlockChainImpl getBlockchain() {
        if (blockchain == null) {
            final ProgramInvokeFactoryImpl programInvokeFactory1 = new ProgramInvokeFactoryImpl();
            blockchain = new BlockChainImpl(
                    getRepository(),
                    getBlockStore(),
                    getReceiptStore(),
                    getTransactionPool(),
                    getCompositeEthereumListener(),
                    new DummyBlockValidator(),
                    false,
                    1,
                    new BlockExecutor(getRepository(), (tx, txindex, coinbase, repository, block, totalGasUsed) -> new TransactionExecutor(
                            tx,
                            txindex,
                            block.getCoinbase(),
                            repository,
                            getBlockStore(),
                            getReceiptStore(),
                            programInvokeFactory1,
                            block,
                            getCompositeEthereumListener(),
                            totalGasUsed,
                            config.getVmConfig(),
                            config.getBlockchainConfig(),
                            config.playVM(),
                            config.isRemascEnabled(),
                            config.vmTrace(),
                            new PrecompiledContracts(config),
                            config.databaseDir(),
                            config.vmTraceDir(),
                            config.vmTraceCompressed()
                    ))
            );
        }

        return blockchain;
    }

    public ReceiptStore getReceiptStore() {
        if (receiptStore == null) {
            HashMapDB inMemoryStore = new HashMapDB();
            receiptStore = new ReceiptStoreImpl(inMemoryStore);
        }

        return receiptStore;
    }

    public BlockStore getBlockStore() {
        if (blockStore == null) {
            this.blockStore = new IndexedBlockStore(new HashMap<>(), new HashMapDB(), null);
        }

        return blockStore;
    }

    public NodeBlockProcessor getBlockProcessor() {
        if (blockProcessor == null) {
            co.usc.net.BlockStore store = new co.usc.net.BlockStore();
            BlockNodeInformation nodeInformation = new BlockNodeInformation();
            SyncConfiguration syncConfiguration = SyncConfiguration.IMMEDIATE_FOR_TESTING;
            BlockSyncService blockSyncService = new BlockSyncService(config, store, getBlockchain(), nodeInformation, syncConfiguration);
            this.blockProcessor = new NodeBlockProcessor(store, getBlockchain(), nodeInformation, blockSyncService, syncConfiguration);
        }

        return blockProcessor;
    }

    public TransactionPool getTransactionPool() {
        if (transactionPool == null) {
            transactionPool = new TransactionPoolImpl(
                    getBlockStore(),
                    getReceiptStore(),
                    getCompositeEthereumListener(),
                    getProgramInvokeFactory(),
                    getRepository(),
                    config
            );
        }

        return transactionPool;
    }

    public Repository getRepository() {
        if (repository == null) {
            HashMapDB stateStore = new HashMapDB();
            repository = new RepositoryImpl(new TrieStoreImpl(stateStore), new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());
        }

        return repository;
    }

    public ReversibleTransactionExecutor getReversibleTransactionExecutor() {
        if (reversibleTransactionExecutor == null) {
            reversibleTransactionExecutor = new ReversibleTransactionExecutor(
                    config,
                    getRepository(),
                    getBlockStore(),
                    getReceiptStore(),
                    getProgramInvokeFactory()
            );
        }

        return reversibleTransactionExecutor;
    }

    public UscImpl getUscImpl() {
        if (uscImpl == null) {
            uscImpl = new UscImpl(
                    null,
                    null,
                    getTransactionPool(),
                    config,
                    getCompositeEthereumListener(),
                    getBlockProcessor(),
                    getReversibleTransactionExecutor(),
                    getBlockchain()
            );
        }

        return uscImpl;
    }

    private CompositeEthereumListener getCompositeEthereumListener() {
        if (compositeEthereumListener == null) {
            compositeEthereumListener = new TestCompositeEthereumListener();
        }

        return compositeEthereumListener;
    }
}
