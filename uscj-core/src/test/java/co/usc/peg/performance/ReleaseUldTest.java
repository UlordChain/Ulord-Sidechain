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

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.ReleaseRequestQueue;
import org.ethereum.core.Denomination;
import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

@Ignore
public class ReleaseUldTest extends BridgePerformanceTestCase {
    @Test
    public void releaseUld() throws IOException {
        int minCentsUld = 5;
        int maxCentsUld = 100;

        final NetworkParameters parameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        BridgeStorageProviderInitializer storageInitializer = (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            ReleaseRequestQueue queue;

            try {
                queue = provider.getReleaseRequestQueue();
            } catch (Exception e) {
                throw new RuntimeException("Unable to gather release request queue");
            }

            for (int i = 0; i < Helper.randomInRange(10, 100); i++) {
                Coin value = Coin.CENT.multiply(Helper.randomInRange(minCentsUld, maxCentsUld));
                queue.add(new UldECKey().toAddress(parameters), value);
            }
        };

        final byte[] releaseUldEncoded = Bridge.RELEASE_ULD.encode();
        ABIEncoder abiEncoder = (int executionIndex) -> releaseUldEncoded;

        TxBuilder txBuilder = (int executionIndex) -> {
            long satoshis = Coin.CENT.multiply(Helper.randomInRange(minCentsUld, maxCentsUld)).getValue();
            BigInteger weis = Denomination.satoshisToWeis(BigInteger.valueOf(satoshis));
            ECKey sender = new ECKey();

            return Helper.buildSendValueTx(sender, weis);
        };

        ExecutionStats stats = new ExecutionStats("releaseUld");
        executeAndAverage("releaseUld", 1000, abiEncoder, storageInitializer, txBuilder, Helper.getRandomHeightProvider(10), stats);

        BridgePerformanceTest.addStats(stats);
    }
}
