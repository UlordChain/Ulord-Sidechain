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

package co.usc.mine;

import co.usc.TestHelpers.Tx;
import co.usc.ulordj.core.*;
import co.usc.ulordj.params.RegTestParams;
import co.usc.config.ConfigUtils;
import co.usc.config.TestSystemProperties;
import co.usc.core.Coin;
import co.usc.core.DifficultyCalculator;
import co.usc.core.bc.BlockChainImpl;
import co.usc.crypto.Keccak256;
import co.usc.remasc.RemascTransaction;
import co.usc.validators.BlockUnclesValidationRule;
import co.usc.validators.BlockValidationRule;
import co.usc.validators.ProofOfWorkRule;
import org.ethereum.core.*;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumImpl;
import org.ethereum.rpc.TypeConverter;
import org.ethereum.util.UscTestFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * Created by adrian.eidelman on 3/16/2016.
 */
public class MinerServerTest {
    private static final TestSystemProperties config = new TestSystemProperties();
    private static final DifficultyCalculator DIFFICULTY_CALCULATOR = new DifficultyCalculator(config);

    private BlockChainImpl blockchain;
    private Repository repository;
    private BlockStore blockStore;
    private TransactionPool transactionPool;

    @Before
    public void setUp() {
        UscTestFactory factory = new UscTestFactory();
        blockchain = factory.getBlockchain();
        repository = factory.getRepository();
        blockStore = factory.getBlockStore();
        transactionPool = factory.getTransactionPool();
    }

    @Test
    public void buildBlockToMineCheckThatLastTransactionIsForREMASC() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Repository repository = Mockito.mock(Repository.class);
        Mockito.when(repository.getSnapshotTo(Mockito.any())).thenReturn(repository);
        Mockito.when(repository.getRoot()).thenReturn(this.repository.getRoot());
        Mockito.when(repository.startTracking()).thenReturn(repository);

        Transaction tx1 = Tx.create(config, 0, 21000, 100, 0, 0, 0);
        byte[] s1 = new byte[32];
        s1[0] = 0;
        Mockito.when(tx1.getHash()).thenReturn(new Keccak256(s1));
        Mockito.when(tx1.getEncoded()).thenReturn(new byte[32]);

        Mockito.when(repository.getNonce(tx1.getSender())).thenReturn(BigInteger.ZERO);
        Mockito.when(repository.getNonce(RemascTransaction.REMASC_ADDRESS)).thenReturn(BigInteger.ZERO);
        Mockito.when(repository.getBalance(tx1.getSender())).thenReturn(Coin.valueOf(4200000L));
        Mockito.when(repository.getBalance(RemascTransaction.REMASC_ADDRESS)).thenReturn(Coin.valueOf(4200000L));

        List<Transaction> txs = new ArrayList<>(Collections.singletonList(tx1));

        TransactionPool localTransactionPool = Mockito.mock(TransactionPool.class);
        Mockito.when(localTransactionPool.getPendingTransactions()).thenReturn(txs);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServerImpl minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                this.blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        localTransactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );

        minerServer.buildBlockToMine(blockchain.getBestBlock(), false);
        Block blockAtHeightOne = minerServer.getBlocksWaitingforPoW().entrySet().iterator().next().getValue();

        List<Transaction> blockTransactions = blockAtHeightOne.getTransactionsList();
        assertNotNull(blockTransactions);
        assertEquals(2, blockTransactions.size());

        Transaction remascTransaction = blockTransactions.get(1);
        assertThat(remascTransaction, instanceOf(RemascTransaction.class));
    }

    @Test
    public void submitUlordBlockTwoTags() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Mockito.when(ethereumImpl.addNewMinedBlock(Mockito.any())).thenReturn(ImportResult.IMPORTED_BEST);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
        byte[] extraData = ByteBuffer.allocate(4).putInt(1).array();
        minerServer.setExtraData(extraData);
        minerServer.start();
        MinerWork work = minerServer.getWork();
        Block bestBlock = blockchain.getBestBlock();

        extraData = ByteBuffer.allocate(4).putInt(2).array();
        minerServer.setExtraData(extraData);
        minerServer.buildBlockToMine(bestBlock, false);
        MinerWork work2 = minerServer.getWork(); // only the tag is used
        assertNotEquals(work2.getBlockHashForMergedMining(),work.getBlockHashForMergedMining());

        UldBlock ulordMergedMiningBlock = getMergedMiningBlockWithTwoTags(work,work2);

        findNonce(work, ulordMergedMiningBlock);
        SubmitBlockResult result;
        result = ((MinerServerImpl) minerServer).submitUlordBlock(work2.getBlockHashForMergedMining(), ulordMergedMiningBlock,true);


        assertEquals("OK", result.getStatus());
        Assert.assertNotNull(result.getBlockInfo());
        assertEquals("0x1", result.getBlockInfo().getBlockIncludedHeight());
        assertEquals("0x494d504f525445445f42455354", result.getBlockInfo().getBlockImportedResult());

        // Submit again the save PoW for a different header
        result = ((MinerServerImpl) minerServer).submitUlordBlock(work.getBlockHashForMergedMining(), ulordMergedMiningBlock,false);

        assertEquals("ERROR", result.getStatus());

        Mockito.verify(ethereumImpl, Mockito.times(1)).addNewMinedBlock(Mockito.any());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void submitUlordBlock() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Mockito.when(ethereumImpl.addNewMinedBlock(Mockito.any())).thenReturn(ImportResult.IMPORTED_BEST);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
            minerServer.start();
            MinerWork work = minerServer.getWork();

            UldBlock ulordMergedMiningBlock = getMergedMiningBlockWithOnlyCoinbase(work);

            findNonce(work, ulordMergedMiningBlock);

            SubmitBlockResult result = minerServer.submitUlordBlock(work.getBlockHashForMergedMining(), ulordMergedMiningBlock);

            assertEquals("OK", result.getStatus());
            Assert.assertNotNull(result.getBlockInfo());
            assertEquals("0x1", result.getBlockInfo().getBlockIncludedHeight());
            assertEquals("0x494d504f525445445f42455354", result.getBlockInfo().getBlockImportedResult());

            Mockito.verify(ethereumImpl, Mockito.times(1)).addNewMinedBlock(Mockito.any());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void submitUlordBlockPartialMerkleWhenBlockIsEmpty() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Mockito.when(ethereumImpl.addNewMinedBlock(Mockito.any())).thenReturn(ImportResult.IMPORTED_BEST);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
            minerServer.start();
            MinerWork work = minerServer.getWork();

            UldBlock ulordMergedMiningBlock = getMergedMiningBlockWithOnlyCoinbase(work);

            findNonce(work, ulordMergedMiningBlock);

            //noinspection ConstantConditions
            UldTransaction coinbase = ulordMergedMiningBlock.getTransactions().get(0);
            List<String> coinbaseReversedHash = Collections.singletonList(Sha256Hash.wrap(coinbase.getHash().getReversedBytes()).toString());
            SubmitBlockResult result = minerServer.submitUlordBlockPartialMerkle(work.getBlockHashForMergedMining(), ulordMergedMiningBlock, coinbase, coinbaseReversedHash, 1);

            assertEquals("OK", result.getStatus());
            Assert.assertNotNull(result.getBlockInfo());
            assertEquals("0x1", result.getBlockInfo().getBlockIncludedHeight());
            assertEquals("0x494d504f525445445f42455354", result.getBlockInfo().getBlockImportedResult());

            Mockito.verify(ethereumImpl, Mockito.times(1)).addNewMinedBlock(Mockito.any());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void submitUlordBlockPartialMerkleWhenBlockHasTransactions() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Mockito.when(ethereumImpl.addNewMinedBlock(Mockito.any())).thenReturn(ImportResult.IMPORTED_BEST);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
            minerServer.start();
            MinerWork work = minerServer.getWork();

            UldTransaction otherTx = Mockito.mock(UldTransaction.class);
            Sha256Hash otherTxHash = Sha256Hash.wrap("aaaabbbbccccddddaaaabbbbccccddddaaaabbbbccccddddaaaabbbbccccdddd");
            Mockito.when(otherTx.getHash()).thenReturn(otherTxHash);
            Mockito.when(otherTx.getHashAsString()).thenReturn(otherTxHash.toString());

            UldBlock ulordMergedMiningBlock = getMergedMiningBlock(work, Collections.singletonList(otherTx));

            findNonce(work, ulordMergedMiningBlock);

            //noinspection ConstantConditions
            UldTransaction coinbase = ulordMergedMiningBlock.getTransactions().get(0);
            String coinbaseReversedHash = Sha256Hash.wrap(coinbase.getHash().getReversedBytes()).toString();
            String otherTxHashReversed = Sha256Hash.wrap(otherTxHash.getReversedBytes()).toString();
            List<String> merkleHashes = Arrays.asList(coinbaseReversedHash, otherTxHashReversed);
            SubmitBlockResult result = minerServer.submitUlordBlockPartialMerkle(work.getBlockHashForMergedMining(), ulordMergedMiningBlock, coinbase, merkleHashes, 2);

            assertEquals("OK", result.getStatus());
            Assert.assertNotNull(result.getBlockInfo());
            assertEquals("0x1", result.getBlockInfo().getBlockIncludedHeight());
            assertEquals("0x494d504f525445445f42455354", result.getBlockInfo().getBlockImportedResult());

            Mockito.verify(ethereumImpl, Mockito.times(1)).addNewMinedBlock(Mockito.any());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void submitUlordBlockTransactionsWhenBlockIsEmpty() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Mockito.when(ethereumImpl.addNewMinedBlock(Mockito.any())).thenReturn(ImportResult.IMPORTED_BEST);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
            minerServer.start();
            MinerWork work = minerServer.getWork();

            UldBlock ulordMergedMiningBlock = getMergedMiningBlockWithOnlyCoinbase(work);

            findNonce(work, ulordMergedMiningBlock);

            //noinspection ConstantConditions
            UldTransaction coinbase = ulordMergedMiningBlock.getTransactions().get(0);
            SubmitBlockResult result = minerServer.submitUlordBlockTransactions(work.getBlockHashForMergedMining(), ulordMergedMiningBlock, coinbase, Collections.singletonList(coinbase.getHashAsString()));

            assertEquals("OK", result.getStatus());
            Assert.assertNotNull(result.getBlockInfo());
            assertEquals("0x1", result.getBlockInfo().getBlockIncludedHeight());
            assertEquals("0x494d504f525445445f42455354", result.getBlockInfo().getBlockImportedResult());

            Mockito.verify(ethereumImpl, Mockito.times(1)).addNewMinedBlock(Mockito.any());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void submitUlordBlockTransactionsWhenBlockHasTransactions() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);
        Mockito.when(ethereumImpl.addNewMinedBlock(Mockito.any())).thenReturn(ImportResult.IMPORTED_BEST);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
            minerServer.start();
            MinerWork work = minerServer.getWork();

            UldTransaction otherTx = Mockito.mock(UldTransaction.class);
            Sha256Hash otherTxHash = Sha256Hash.wrap("aaaabbbbccccddddaaaabbbbccccddddaaaabbbbccccddddaaaabbbbccccdddd");
            Mockito.when(otherTx.getHash()).thenReturn(otherTxHash);
            Mockito.when(otherTx.getHashAsString()).thenReturn(otherTxHash.toString());

            UldBlock ulordMergedMiningBlock = getMergedMiningBlock(work, Collections.singletonList(otherTx));

            findNonce(work, ulordMergedMiningBlock);

            //noinspection ConstantConditions
            UldTransaction coinbase = ulordMergedMiningBlock.getTransactions().get(0);
            List<String> txs = Arrays.asList(coinbase.getHashAsString(), otherTxHash.toString());
            SubmitBlockResult result = minerServer.submitUlordBlockTransactions(work.getBlockHashForMergedMining(), ulordMergedMiningBlock, coinbase, txs);

            assertEquals("OK", result.getStatus());
            Assert.assertNotNull(result.getBlockInfo());
            assertEquals("0x1", result.getBlockInfo().getBlockIncludedHeight());
            assertEquals("0x494d504f525445445f42455354", result.getBlockInfo().getBlockImportedResult());

            Mockito.verify(ethereumImpl, Mockito.times(1)).addNewMinedBlock(Mockito.any());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void workWithNoTransactionsZeroFees() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                this.blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );

        minerServer.start();
        try {
        MinerWork work = minerServer.getWork();

        assertEquals("0", work.getFeesPaidToMiner());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void initialWorkTurnsNotifyFlagOn() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                this.blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
        minerServer.start();

        MinerWork work = minerServer.getWork();

        assertEquals(true, work.getNotify());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void secondWorkWithNoChangesTurnsNotifyFlagOff() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                this.blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );

        minerServer.start();
        try {
        MinerWork work = minerServer.getWork();

        assertEquals(true, work.getNotify());

        work = minerServer.getWork();

        assertEquals(false, work.getNotify());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void secondBuildBlockToMineTurnsNotifyFlagOff() {
        EthereumImpl ethereumImpl = Mockito.mock(EthereumImpl.class);

        BlockUnclesValidationRule unclesValidationRule = Mockito.mock(BlockUnclesValidationRule.class);
        Mockito.when(unclesValidationRule.isValid(Mockito.any())).thenReturn(true);
        MinerServer minerServer = new MinerServerImpl(
                config,
                ethereumImpl,
                this.blockchain,
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                new BlockToMineBuilder(
                        ConfigUtils.getDefaultMiningConfig(),
                        repository,
                        blockStore,
                        transactionPool,
                        DIFFICULTY_CALCULATOR,
                        new GasLimitCalculator(config),
                        unclesValidationRule,
                        config,
                        null
                ),
                ConfigUtils.getDefaultMiningConfig()
        );
        try {
        minerServer.start();

        MinerWork work = minerServer.getWork();

        String hashForMergedMining = work.getBlockHashForMergedMining();

        minerServer.buildBlockToMine(blockchain.getBestBlock(), false);

        work = minerServer.getWork();
        assertEquals(hashForMergedMining, work.getBlockHashForMergedMining());
        assertEquals(false, work.getNotify());
        } finally {
            minerServer.stop();
        }
    }

    @Test
    public void getCurrentTimeInMilliseconds() {
        long current = System.currentTimeMillis() / 1000;

        MinerServer server = getMinerServerWithMocks();

        long result = server.getCurrentTimeInSeconds();

        Assert.assertTrue(result >= current);
        Assert.assertTrue(result <= current + 1);
    }

    @Test
    public void increaseTime() {
        long current = System.currentTimeMillis() / 1000;

        MinerServer server = getMinerServerWithMocks();

        assertEquals(10, server.increaseTime(10));

        long result = server.getCurrentTimeInSeconds();

        Assert.assertTrue(result >= current + 10);
        Assert.assertTrue(result <= current + 11);
    }

    @Test
    public void increaseTimeUsingNegativeNumberHasNoEffect() {
        long current = System.currentTimeMillis() / 1000;

        MinerServer server = getMinerServerWithMocks();

        assertEquals(0, server.increaseTime(-10));

        long result = server.getCurrentTimeInSeconds();

        Assert.assertTrue(result >= current);
    }

    @Test
    public void increaseTimeTwice() {
        long current = System.currentTimeMillis() / 1000;

        MinerServer server = getMinerServerWithMocks();

        assertEquals(5, server.increaseTime(5));
        assertEquals(10, server.increaseTime(5));

        long result = server.getCurrentTimeInSeconds();

        Assert.assertTrue(result >= current + 10);
        Assert.assertTrue(result <= current + 11);
    }

    private UldBlock getMergedMiningBlockWithOnlyCoinbase(MinerWork work) {
        return getMergedMiningBlock(work, Collections.emptyList());
    }

    private UldBlock getMergedMiningBlock(MinerWork work, List<UldTransaction> txs) {
        NetworkParameters ulordNetworkParameters = RegTestParams.get();
        UldTransaction ulordMergedMiningCoinbaseTransaction = MinerUtils.getUlordMergedMiningCoinbaseTransaction(ulordNetworkParameters, work);

        List<UldTransaction> blockTxs = new ArrayList<>();
        blockTxs.add(ulordMergedMiningCoinbaseTransaction);
        blockTxs.addAll(txs);

        return MinerUtils.getUlordMergedMiningBlock(ulordNetworkParameters, blockTxs);
    }

    private UldBlock getMergedMiningBlockWithTwoTags(MinerWork work, MinerWork work2) {
        NetworkParameters ulordNetworkParameters = RegTestParams.get();
        UldTransaction ulordMergedMiningCoinbaseTransaction =
                MinerUtils.getUlordMergedMiningCoinbaseTransactionWithTwoTags(ulordNetworkParameters, work, work2);
        return MinerUtils.getUlordMergedMiningBlock(ulordNetworkParameters, ulordMergedMiningCoinbaseTransaction);
    }

    private void findNonce(MinerWork work, UldBlock ulordMergedMiningBlock) {
        BigInteger target = new BigInteger(TypeConverter.stringHexToByteArray(work.getTarget()));

        while (true) {
            try {
                // Is our proof of work valid yet?
                BigInteger blockHashBI = ulordMergedMiningBlock.getHash().toBigInteger();
                if (blockHashBI.compareTo(target) <= 0) {
                    break;
                }
                // No, so increment the nonce and try again.
                ulordMergedMiningBlock.setNonce(ulordMergedMiningBlock.getNonce().add(BigInteger.ONE));
            } catch (VerificationException e) {
                throw new RuntimeException(e); // Cannot happen.
            }
        }
    }

    private MinerServerImpl getMinerServerWithMocks() {
        return new MinerServerImpl(
                config,
                Mockito.mock(Ethereum.class),
                Mockito.mock(Blockchain.class),
                null,
                DIFFICULTY_CALCULATOR,
                new ProofOfWorkRule(config).setFallbackMiningEnabled(false),
                getBuilderWithMocks(),
                ConfigUtils.getDefaultMiningConfig()
        );
    }

    private BlockToMineBuilder getBuilderWithMocks() {
        return new BlockToMineBuilder(
                ConfigUtils.getDefaultMiningConfig(),
                Mockito.mock(Repository.class),
                Mockito.mock(BlockStore.class),
                Mockito.mock(TransactionPool.class),
                DIFFICULTY_CALCULATOR,
                new GasLimitCalculator(config),
                Mockito.mock(BlockValidationRule.class),
                config,
                Mockito.mock(ReceiptStore.class)
        );
    }
}
