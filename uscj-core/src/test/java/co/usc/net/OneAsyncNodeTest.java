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

package co.usc.net;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.config.TestSystemProperties;
import co.usc.core.DifficultyCalculator;
import co.usc.net.messages.BlockMessage;
import co.usc.net.simples.SimpleAsyncNode;
import co.usc.net.sync.SyncConfiguration;
import co.usc.test.World;
import co.usc.validators.DummyBlockValidationRule;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.rpc.Simples.SimpleChannelManager;
import org.ethereum.util.UscMockFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ajlopez on 5/14/2016.
 */
public class OneAsyncNodeTest {
    private static SimpleAsyncNode createNode() {
        final World world = new World();
        final BlockStore store = new BlockStore();
        final Blockchain blockchain = world.getBlockChain();

        TestSystemProperties config = new TestSystemProperties();
        BlockNodeInformation nodeInformation = new BlockNodeInformation();
        SyncConfiguration syncConfiguration = SyncConfiguration.IMMEDIATE_FOR_TESTING;
        BlockSyncService blockSyncService = new BlockSyncService(config, store, blockchain, nodeInformation, syncConfiguration);
        NodeBlockProcessor processor = new NodeBlockProcessor(store, blockchain, nodeInformation, blockSyncService, syncConfiguration);
        SimpleChannelManager channelManager = new SimpleChannelManager();
        SyncProcessor syncProcessor = new SyncProcessor(blockchain, blockSyncService, UscMockFactory.getPeerScoringManager(), channelManager, syncConfiguration, new DummyBlockValidationRule(), new DifficultyCalculator(config));
        NodeMessageHandler handler = new NodeMessageHandler(config, processor, syncProcessor, channelManager, null, UscMockFactory.getPeerScoringManager(), new DummyBlockValidationRule());

        return new SimpleAsyncNode(handler, syncProcessor, channelManager);
    }

    private static Block getGenesis() {
        final World world = new World();

        return world.getBlockChain().getBestBlock();
    }

    @Test
    public void buildBlockchain() throws InterruptedException {
        SimpleAsyncNode node = createNode();

        List<Block> blocks = new BlockGenerator().getBlockChain(getGenesis(), 10);

        for (Block block : blocks)
            node.receiveMessageFrom(null, new BlockMessage(block));

        node.waitExactlyNTasksWithTimeout(10);
        node.joinWithTimeout();

        Assert.assertEquals(blocks.size(), node.getBestBlock().getNumber());
        Assert.assertEquals(blocks.get(blocks.size() - 1).getHash(), node.getBestBlock().getHash());
    }

    @Test
    public void buildBlockchainInReverse() throws InterruptedException {
        SimpleAsyncNode node = createNode();

        List<Block> blocks = new BlockGenerator().getBlockChain(getGenesis(), 10);

        List<Block> reverse = new ArrayList<>();

        for (Block block : blocks)
            reverse.add(0, block);

        for (Block block : reverse)
            node.receiveMessageFrom(null, new BlockMessage(block));

        node.waitExactlyNTasksWithTimeout(10);
        node.joinWithTimeout();

        Assert.assertEquals(blocks.size(), node.getBestBlock().getNumber());
        Assert.assertEquals(blocks.get(blocks.size() - 1).getHash(), node.getBestBlock().getHash());
    }
}
