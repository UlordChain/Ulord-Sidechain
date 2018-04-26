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

package co.usc.core;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.core.bc.BlockChainStatus;
import co.usc.test.World;
import co.usc.test.builders.AccountBuilder;
import co.usc.test.builders.TransactionBuilder;
import co.usc.blockchain.utils.BlockGenerator;
import co.usc.test.World;
import co.usc.test.builders.AccountBuilder;
import co.usc.test.builders.TransactionBuilder;
import org.ethereum.core.*;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ajlopez on 15/04/2017.
 */
public class SnapshotManagerTest {
    @Test
    public void createWithNoSnapshot() {
        SnapshotManager manager = new SnapshotManager();

        Assert.assertNotNull(manager.getSnapshots());
        Assert.assertTrue(manager.getSnapshots().isEmpty());
    }

    @Test
    public void takeSnapshotOnGenesis() {
        Blockchain blockchain = createBlockchain();

        SnapshotManager manager = new SnapshotManager();

        int result = manager.takeSnapshot(blockchain);

        Assert.assertEquals(1, result);

        Assert.assertNotNull(manager.getSnapshots());
        Assert.assertFalse(manager.getSnapshots().isEmpty());
        Assert.assertEquals(1, manager.getSnapshots().size());
        Assert.assertEquals(0, manager.getSnapshots().get(0).longValue());
    }

    @Test
    public void takeSnapshotOnManyBlocks() {
        Blockchain blockchain = createBlockchain();

        addBlocks(blockchain, 10);

        SnapshotManager manager = new SnapshotManager();

        int result = manager.takeSnapshot(blockchain);

        Assert.assertEquals(1, result);

        Assert.assertNotNull(manager.getSnapshots());
        Assert.assertFalse(manager.getSnapshots().isEmpty());
        Assert.assertEquals(1, manager.getSnapshots().size());
        Assert.assertEquals(10, manager.getSnapshots().get(0).longValue());
    }

    @Test
    public void takeTwoSnapshots() {
        Blockchain blockchain = createBlockchain();

        addBlocks(blockchain, 10);

        SnapshotManager manager = new SnapshotManager();

        int result1 = manager.takeSnapshot(blockchain);

        Assert.assertEquals(1, result1);

        addBlocks(blockchain, 10);

        int result2 = manager.takeSnapshot(blockchain);

        Assert.assertEquals(2, result2);

        Assert.assertNotNull(manager.getSnapshots());
        Assert.assertFalse(manager.getSnapshots().isEmpty());
        Assert.assertEquals(2, manager.getSnapshots().size());
        Assert.assertEquals(10, manager.getSnapshots().get(0).longValue());
        Assert.assertEquals(20, manager.getSnapshots().get(1).longValue());
    }

    @Test
    public void revertToNegativeSnapshot() {
        SnapshotManager manager = new SnapshotManager();

        Assert.assertFalse(manager.revertToSnapshot(null, -1));
    }

    @Test
    public void revertToNonExistentSnapshot() {
        SnapshotManager manager = new SnapshotManager();

        Assert.assertFalse(manager.revertToSnapshot(null, 0));
        Assert.assertFalse(manager.revertToSnapshot(null, 1));
        Assert.assertFalse(manager.revertToSnapshot(null, 10));
    }

    @Test
    public void revertToSnapshot() {
        Blockchain blockchain = createBlockchain();
        addBlocks(blockchain, 10);

        BlockChainStatus status = blockchain.getStatus();

        SnapshotManager manager = new SnapshotManager();

        int snapshotId = manager.takeSnapshot(blockchain);

        addBlocks(blockchain, 20);

        Assert.assertEquals(30, blockchain.getStatus().getBestBlockNumber());

        Assert.assertTrue(manager.revertToSnapshot(blockchain, snapshotId));

        BlockChainStatus newStatus = blockchain.getStatus();

        Assert.assertEquals(status.getBestBlockNumber(), newStatus.getBestBlockNumber());
        Assert.assertEquals(status.getTotalDifficulty(), newStatus.getTotalDifficulty());
        Assert.assertEquals(status.getBestBlock().getHash(), newStatus.getBestBlock().getHash());

        for (int k = 11; k <= 30; k++)
            Assert.assertTrue(blockchain.getBlocksByNumber(k).isEmpty());
    }

    @Test
    public void revertToSnapshotClearingTransactionPool() {
        Blockchain blockchain = createBlockchain();
        addBlocks(blockchain, 10);

        BlockChainStatus status = blockchain.getStatus();

        SnapshotManager manager = new SnapshotManager();

        int snapshotId = manager.takeSnapshot(blockchain);

        addBlocks(blockchain, 20);

        manager.takeSnapshot(blockchain);

        Assert.assertEquals(2, manager.getSnapshots().size());

        TransactionPool transactionPool = blockchain.getTransactionPool();

        Assert.assertNotNull(transactionPool);

        List<Transaction> txs = new ArrayList<>();
        txs.add(createSampleTransaction());
        txs.add(createSampleTransaction());
        transactionPool.addTransactions(txs);
        Assert.assertFalse(transactionPool.getPendingTransactions().isEmpty());
        Assert.assertFalse(transactionPool.getPendingTransactions().isEmpty());

        Assert.assertEquals(30, blockchain.getStatus().getBestBlockNumber());

        Assert.assertTrue(manager.revertToSnapshot(blockchain, snapshotId));

        BlockChainStatus newStatus = blockchain.getStatus();

        Assert.assertEquals(status.getBestBlockNumber(), newStatus.getBestBlockNumber());
        Assert.assertEquals(status.getTotalDifficulty(), newStatus.getTotalDifficulty());
        Assert.assertEquals(status.getBestBlock().getHash(), newStatus.getBestBlock().getHash());

        Assert.assertTrue(blockchain.getTransactionPool().getPendingTransactions().isEmpty());

        Assert.assertEquals(1, manager.getSnapshots().size());

        for (int k = 11; k <= 30; k++)
            Assert.assertTrue(blockchain.getBlocksByNumber(k).isEmpty());
    }

    @Test
    public void resetSnapshotClearingTransactionPool() {
        Blockchain blockchain = createBlockchain();
        Block genesis = blockchain.getBestBlock();
        BlockDifficulty genesisDifficulty = blockchain.getStatus().getTotalDifficulty();

        addBlocks(blockchain, 10);

        BlockChainStatus status = blockchain.getStatus();

        Assert.assertEquals(10, status.getBestBlockNumber());

        TransactionPool transactionPool = blockchain.getTransactionPool();

        Assert.assertNotNull(transactionPool);
        List<Transaction> txs = new ArrayList<>();
        txs.add(createSampleTransaction());
        txs.add(createSampleTransaction());
        transactionPool.addTransactions(txs);
        Assert.assertFalse(transactionPool.getPendingTransactions().isEmpty());
        Assert.assertFalse(transactionPool.getPendingTransactions().isEmpty());

        SnapshotManager manager = new SnapshotManager();

        manager.takeSnapshot(blockchain);
        Assert.assertFalse(manager.getSnapshots().isEmpty());
        Assert.assertTrue(manager.resetSnapshots(blockchain));
        Assert.assertTrue(manager.getSnapshots().isEmpty());

        Assert.assertTrue(manager.resetSnapshots(blockchain));

        BlockChainStatus newStatus = blockchain.getStatus();

        Assert.assertEquals(0, newStatus.getBestBlockNumber());
        Assert.assertEquals(genesisDifficulty, newStatus.getTotalDifficulty());
        Assert.assertEquals(genesis.getHash(), newStatus.getBestBlock().getHash());

        Assert.assertTrue(blockchain.getTransactionPool().getPendingTransactions().isEmpty());

        Assert.assertTrue(manager.getSnapshots().isEmpty());

        for (int k = 1; k <= 10; k++)
            Assert.assertTrue(blockchain.getBlocksByNumber(k).isEmpty());
    }

    private static Blockchain createBlockchain() {
        return new World().getBlockChain();
    }

    private static void addBlocks(Blockchain blockchain, int size) {
        List<Block> blocks = new BlockGenerator().getBlockChain(blockchain.getBestBlock(), size);

        for (Block block : blocks)
            blockchain.tryToConnect(block);
    }

    private static Transaction createSampleTransaction() {
        Account sender = new AccountBuilder().name("sender").build();
        Account receiver = new AccountBuilder().name("receiver").build();

        Transaction tx = new TransactionBuilder()
                .sender(sender)
                .receiver(receiver)
                .value(BigInteger.TEN)
                .build();

        return tx;
    }
}
