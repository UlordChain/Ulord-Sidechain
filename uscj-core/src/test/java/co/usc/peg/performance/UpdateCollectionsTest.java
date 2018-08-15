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

import co.usc.ulordj.core.*;
import co.usc.ulordj.script.Script;
import co.usc.config.BridgeRegTestConstants;
import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.ReleaseRequestQueue;
import co.usc.peg.ReleaseTransactionSet;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;

@Ignore
public class UpdateCollectionsTest extends BridgePerformanceTestCase {
    @Test
    public void updateCollections() throws IOException {
        ExecutionStats stats = new ExecutionStats("updateCollections");

        updateCollections_nothing(stats, 1000);
        updateCollections_buildReleaseTxs(stats, 100);
        updateCollections_confirmTxs(stats, 300);

        BridgePerformanceTest.addStats(stats);
    }

    private void updateCollections_nothing(ExecutionStats stats, int numCases) throws IOException {
        final NetworkParameters parameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        BridgeStorageProviderInitializer storageInitializer = (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {};
        final byte[] updateCollectionsEncoded = Bridge.UPDATE_COLLECTIONS.encode();
        ABIEncoder abiEncoder = (int executionIndex) -> updateCollectionsEncoded;

        executeAndAverage(
                "updateCollections-nothing",
                numCases,
                abiEncoder,
                storageInitializer,
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(10), stats
        );
    }

    private void updateCollections_buildReleaseTxs(ExecutionStats stats, int numCases) throws IOException {
        final int minUTXOs = 1;
        final int maxUTXOs = 1000;
        final int minMilliUld = 1;
        final int maxMilliUld = 1000;
        final int minReleaseRequests = 1;
        final int maxReleaseRequests = 100;
        final int minMilliReleaseUld = 10;
        final int maxMilliReleaseUld = 2000;

        final NetworkParameters parameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        BridgeStorageProviderInitializer storageInitializer = (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            Random rnd = new Random();
            List<UTXO> utxos;
            ReleaseRequestQueue queue;

            try {
                utxos = provider.getNewFederationUldUTXOs();
            } catch (Exception e) {
                throw new RuntimeException("Unable to gather active federation uld utxos");
            }

            try {
                queue = provider.getReleaseRequestQueue();
            } catch (Exception e) {
                throw new RuntimeException("Unable to gather release request queue");
            }

            // Generate some utxos
            int numUTXOs = Helper.randomInRange(minUTXOs, maxUTXOs);

            Script federationScript = BridgeRegTestConstants.getInstance().getGenesisFederation().getP2SHScript();

            for (int i = 0; i < numUTXOs; i++) {
                Sha256Hash hash = Sha256Hash.wrap(HashUtil.sha256(BigInteger.valueOf(rnd.nextLong()).toByteArray()));
                Coin value = Coin.MILLICOIN.multiply(Helper.randomInRange(minMilliUld, maxMilliUld));
                utxos.add(new UTXO(hash, 0, value, 1, false, federationScript));
            }

            // Generate some release requests to process
            for (int i = 0; i < Helper.randomInRange(minReleaseRequests, maxReleaseRequests); i++) {
                Coin value = Coin.MILLICOIN.multiply(Helper.randomInRange(minMilliReleaseUld, maxMilliReleaseUld));
                queue.add(new UldECKey().toAddress(parameters), value);
            }
        };

        final byte[] updateCollectionsEncoded = Bridge.UPDATE_COLLECTIONS.encode();
        ABIEncoder abiEncoder = (int executionIndex) -> updateCollectionsEncoded;

        executeAndAverage(
                "updateCollections-releaseRequests",
                numCases,
                abiEncoder,
                storageInitializer,
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(10),
                stats
        );
    }

    private void updateCollections_confirmTxs(ExecutionStats stats, int numCases) throws IOException {
        final int minTxsWaitingForSigs = 0;
        final int maxTxsWaitingForSigs = 10;
        final int minReleaseTxs = 1;
        final int maxReleaseTxs = 100;
        final int minBlockNumber = 10;
        final int maxBlockNumber = 100;
        final int minHeight = 50;
        final int maxHeight = 150;
        final int minCentOutput = 1;
        final int maxCentOutput = 100;

        final NetworkParameters parameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        BridgeStorageProviderInitializer storageInitializer = (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            Random rnd = new Random();
            SortedMap<Keccak256, UldTransaction> txsWaitingForSignatures;
            ReleaseTransactionSet txSet;

            try {
                txsWaitingForSignatures = provider.getUscTxsWaitingForSignatures();
            } catch (Exception e) {
                throw new RuntimeException("Unable to gather txs waiting for signatures");
            }

            try {
                txSet = provider.getReleaseTransactionSet();
            } catch (Exception e) {
                throw new RuntimeException("Unable to gather release tx set");
            }

            // Generate some txs waiting for signatures
            Script genesisFederationScript = bridgeConstants.getGenesisFederation().getP2SHScript();
            for (int i = 0; i < Helper.randomInRange(minTxsWaitingForSigs, maxTxsWaitingForSigs); i++) {
                Keccak256 uscHash = new Keccak256(HashUtil.keccak256(BigInteger.valueOf(rnd.nextLong()).toByteArray()));
                UldTransaction uldTx = new UldTransaction(networkParameters);
                Sha256Hash inputHash = Sha256Hash.wrap(HashUtil.sha256(BigInteger.valueOf(rnd.nextLong()).toByteArray()));
                uldTx.addInput(inputHash, 0, genesisFederationScript);
                uldTx.addOutput(Helper.randomCoin(Coin.CENT, minCentOutput, maxCentOutput), new UldECKey());
                txsWaitingForSignatures.put(uscHash, uldTx);
            }

            // Generate some txs waiting for confirmations
            for (int i = 0; i < Helper.randomInRange(minReleaseTxs, maxReleaseTxs); i++) {
                UldTransaction uldTx = new UldTransaction(networkParameters);
                Sha256Hash inputHash = Sha256Hash.wrap(HashUtil.sha256(BigInteger.valueOf(rnd.nextLong()).toByteArray()));
                uldTx.addInput(inputHash, 0, genesisFederationScript);
                uldTx.addOutput(Helper.randomCoin(Coin.CENT, minCentOutput, maxCentOutput), new UldECKey());
                long blockNumber = Helper.randomInRange(minBlockNumber, maxBlockNumber);
                txSet.add(uldTx, blockNumber);
            }
        };

        final byte[] updateCollectionsEncoded = Bridge.UPDATE_COLLECTIONS.encode();
        ABIEncoder abiEncoder = (int executionIndex) -> updateCollectionsEncoded;
        HeightProvider heightProvider = (int executionIndex) -> Helper.randomInRange(minHeight, maxHeight);

        executeAndAverage(
                "updateCollections-releaseTxs",
                numCases,
                abiEncoder,
                storageInitializer,
                Helper.getZeroValueRandomSenderTxBuilder(),
                heightProvider,
                stats
        );
    }
}
