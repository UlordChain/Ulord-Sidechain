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

package co.usc.peg.performance;

import co.usc.ulordj.core.UldBlockChain;
import co.usc.ulordj.core.Context;
import co.usc.ulordj.store.BlockStoreException;
import co.usc.ulordj.store.UldBlockStore;
import co.usc.config.TestSystemProperties;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.RepositoryBlockStore;
import org.ethereum.core.Repository;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class UldBlockchainTest extends BridgePerformanceTestCase {
    @Test
    public void getUldBlockChainBestChainHeight() throws IOException {
        ABIEncoder abiEncoder = (int executionIndex) -> Bridge.GET_ULD_BLOCKCHAIN_BEST_CHAIN_HEIGHT.encode();
        ExecutionStats stats = new ExecutionStats("getUldBlockChainBestChainHeight");
        executeAndAverage("getUldBlockChainBestChainHeight", 200, abiEncoder, buildInitializer(), Helper.getZeroValueRandomSenderTxBuilder(), Helper.getRandomHeightProvider(10), stats);
        BridgePerformanceTest.addStats(stats);
    }

    @Test
    public void getUldBlockChainBlockLocator() throws IOException {
        ABIEncoder abiEncoder = (int executionIndex) -> Bridge.GET_ULD_BLOCKCHAIN_BLOCK_LOCATOR.encode();
        ExecutionStats stats = new ExecutionStats("getUldBlockChainBlockLocator");
        executeAndAverage("getUldBlockChainBlockLocator", 200, abiEncoder, buildInitializer(), Helper.getZeroValueRandomSenderTxBuilder(), Helper.getRandomHeightProvider(10), stats);
        BridgePerformanceTest.addStats(stats);
    }

    private BridgeStorageProviderInitializer buildInitializer() {
        final int minUldBlocks = 1000;
        final int maxUldBlocks = 2000;

        return (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            UldBlockStore UldBlockStore = new RepositoryBlockStore(new TestSystemProperties(), repository, PrecompiledContracts.BRIDGE_ADDR);
            Context btcContext = new Context(networkParameters);
            UldBlockChain UldBlockChain;
            try {
                UldBlockChain = new UldBlockChain(btcContext, UldBlockStore);
            } catch (BlockStoreException e) {
                throw new RuntimeException("Error initializing btc blockchain for tests");
            }

            int blocksToGenerate = Helper.randomInRange(minUldBlocks, maxUldBlocks);
            Helper.generateAndAddBlocks(UldBlockChain, blocksToGenerate);
        };
    }


}
