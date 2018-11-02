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

import co.usc.peg.whitelist.OneOffWhiteListEntry;
import co.usc.ulordj.core.*;
import co.usc.ulordj.script.Script;
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
import java.util.Arrays;
import java.util.List;

@Ignore
public class RegisterUldTransactionTest extends BridgePerformanceTestCase {
    private UldBlock blockWithTx;
    private int blockWithTxHeight;
    private UldTransaction txToLock;
    private PartialMerkleTree pmtOfLockTx;

    @Test
    public void registerUldTransaction() {
        ExecutionStats stats = new ExecutionStats("registerUldTransaction");
        registerUldTransaction_lockSuccess(100, stats);
        registerUldTransaction_alreadyProcessed(100, stats);
        registerUldTransaction_notEnoughConfirmations(100, stats);
        BridgePerformanceTest.addStats(stats);
    }

    private void registerUldTransaction_lockSuccess(int times, ExecutionStats stats) {
        BridgeStorageProviderInitializer storageInitializer = generateInitializerForLock(
                1000,
                2000,
                20,
                false
        );

        executeAndAverage("registerUldTransaction-lockSuccess", times, getABIEncoder(), storageInitializer, Helper.getZeroValueRandomSenderTxBuilder(), Helper.getRandomHeightProvider(10), stats);

    }

    private void registerUldTransaction_alreadyProcessed(int times, ExecutionStats stats) {
        BridgeStorageProviderInitializer storageInitializer = generateInitializerForLock(
                1000,
                2000,
                20,
                true
        );

        executeAndAverage("registerUldTransaction-alreadyProcessed", times, getABIEncoder(), storageInitializer, Helper.getZeroValueRandomSenderTxBuilder(), Helper.getRandomHeightProvider(10), stats);
    }

    private void registerUldTransaction_notEnoughConfirmations(int times, ExecutionStats stats) {
        BridgeStorageProviderInitializer storageInitializer = generateInitializerForLock(
                1000,
                2000,
                1,
                false
        );

        executeAndAverage("registerUldTransaction-notEnoughConfirmations", times, getABIEncoder(), storageInitializer, Helper.getZeroValueRandomSenderTxBuilder(), Helper.getRandomHeightProvider(10), stats);
    }

    private ABIEncoder getABIEncoder() {
        return (int executionIndex) ->
                Bridge.REGISTER_ULD_TRANSACTION.encode(new Object[]{
                        txToLock.ulordSerialize(),
                        blockWithTxHeight,
                        pmtOfLockTx.ulordSerialize()
                });
    }

    private BridgeStorageProviderInitializer generateInitializerForLock(int minUldBlocks, int maxUldBlocks, int numberOfLockConfirmations, boolean markAsAlreadyProcessed) {
        return (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
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

            // Sender and amounts
            UldECKey from = new UldECKey();
            Address fromAddress = from.toAddress(networkParameters);
            Coin fromAmount = Coin.CENT.multiply(Helper.randomInRange(10, 100));
            Coin lockAmount = fromAmount.divide(Helper.randomInRange(2, 10));
            Coin changeAmount = fromAmount.subtract(lockAmount).subtract(Coin.MILLICOIN); // 1 millicoin fee simulation

            // Whitelisting sender
            provider.getLockWhitelist().put(fromAddress, new OneOffWhiteListEntry(fromAddress, lockAmount));

            // Input tx
            UldTransaction inputTx = new UldTransaction(networkParameters);
            inputTx.addOutput(fromAmount, fromAddress);

            // Lock tx that uses the input tx
            txToLock = new UldTransaction(networkParameters);
            txToLock.addInput(inputTx.getOutput(0));
            txToLock.addOutput(lockAmount, bridgeConstants.getGenesisFederation().getAddress());
            txToLock.addOutput(changeAmount, fromAddress);

            // Signing the input of the lock tx
            Sha256Hash hashForSig = txToLock.hashForSignature(0, inputTx.getOutput(0).getScriptPubKey(), UldTransaction.SigHash.ALL, false);
            Script scriptSig = new Script(Script.createInputScript(from.sign(hashForSig).encodeToDER(), from.getPubKey()));
            txToLock.getInput(0).setScriptSig(scriptSig);

            pmtOfLockTx = PartialMerkleTree.buildFromLeaves(networkParameters, new byte[]{(byte) 0xff}, Arrays.asList(txToLock.getHash()));
            List<Sha256Hash> hashes = new ArrayList<>();
            Sha256Hash merkleRoot = pmtOfLockTx.getTxnHashAndMerkleRoot(hashes);

            blockWithTx = Helper.generateUldBlock(lastBlock, Arrays.asList(txToLock), merkleRoot);
            uldBlockChain.add(blockWithTx);
            blockWithTxHeight = uldBlockChain.getBestChainHeight();

            Helper.generateAndAddBlocks(uldBlockChain, numberOfLockConfirmations);

            // Marking as already processed
            if (markAsAlreadyProcessed) {
                try {
                    provider.getUldTxHashesAlreadyProcessed().put(txToLock.getHash(), (long) blockWithTxHeight - 10);
                } catch (IOException e) {
                    throw new RuntimeException("Exception while trying to mark tx as already processed for test");
                }
            }
        };
    }


}
