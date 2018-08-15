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

package co.usc.peg.performance;

import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.UldBlockChain;
import co.usc.ulordj.core.Context;
import co.usc.ulordj.store.BlockStoreException;
import co.usc.ulordj.store.UldBlockStore;
import co.usc.config.TestSystemProperties;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.RepositoryBlockStore;
import co.usc.config.TestSystemProperties;
import org.ethereum.core.Repository;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Ignore
public class ReceiveHeadersTest extends BridgePerformanceTestCase {
    private UldBlock blockToTry;

    @Test
    public void receiveHeaders() throws IOException {
        final int minUldBlocks = 1000;
        final int maxUldBlocks = 2000;

        BridgeStorageProviderInitializer storageInitializer = (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            UldBlockStore uldBlockStore = new RepositoryBlockStore(new TestSystemProperties(), repository, PrecompiledContracts.BRIDGE_ADDR);
            Context uldContext = new Context(networkParameters);
            UldBlockChain uldBlockChain;
            try {
                uldBlockChain = new UldBlockChain(uldContext, uldBlockStore);
            } catch (BlockStoreException e) {
                throw new RuntimeException("Error initializing uld blockchain for tests");
            }

            int blocksToGenerate = Helper.randomInRange(minUldBlocks, maxUldBlocks);
            UldBlock lastBlock = Helper.generateAndAddBlocks(uldBlockChain, blocksToGenerate);
            blockToTry = Helper.generateUldBlock(lastBlock);
        };

        ABIEncoder abiEncoder = (int executionIndex) -> {
            List<UldBlock> headersToSendToBridge = new ArrayList<>();

            // Send just one header (that's the only case we're interested in measuring atm
            headersToSendToBridge.add(blockToTry);

            Object[] headersEncoded = headersToSendToBridge.stream().map(h -> h.ulordSerialize()).toArray();

            return Bridge.RECEIVE_HEADERS.encode(new Object[]{headersEncoded});
        };

        ExecutionStats stats = new ExecutionStats("receiveHeaders");
        executeAndAverage("receiveHeaders", 200, abiEncoder, storageInitializer, Helper.getZeroValueRandomSenderTxBuilder(), Helper.getRandomHeightProvider(10), stats);

        BridgePerformanceTest.addStats(stats);
    }


}
