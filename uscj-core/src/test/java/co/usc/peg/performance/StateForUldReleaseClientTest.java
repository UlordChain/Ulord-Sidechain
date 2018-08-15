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
import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.Federation;
import co.usc.peg.PegTestUtils;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@Ignore
public class StateForUldReleaseClientTest extends BridgePerformanceTestCase {
    @Test
    public void getStateForUldReleaseClient() {
        ExecutionStats stats = new ExecutionStats("getStateForUldReleaseClient");

        executeAndAverage(
                "getStateForUldReleaseClient",
                200,
                (int executionIndex) -> Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT.encode(),
                getInitializer(),
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(10),
                stats
        );

        BridgePerformanceTest.addStats(stats);
    }

    private BridgeStorageProviderInitializer getInitializer() {
        final int minNumTxs = 1;
        final int maxNumTxs = 100;

        final int minNumInputs = 1;
        final int maxNumInputs = 10;

        return (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            Map<Keccak256, UldTransaction> txsWaitingForSignatures;
            try {
                txsWaitingForSignatures = provider.getUscTxsWaitingForSignatures();
            } catch (IOException e) {
                throw new RuntimeException("Exception while trying to gather txs waiting for signatures for storage initialization");
            }

            int numTxs = Helper.randomInRange(minNumTxs, maxNumTxs);
            for (int i = 0; i < numTxs; i++) {
                UldTransaction releaseTx = new UldTransaction(networkParameters);

                Federation federation = bridgeConstants.getGenesisFederation();

                // Receiver and amounts
                Address toAddress = new UldECKey().toAddress(networkParameters);
                Coin releaseAmount = Coin.CENT.multiply(Helper.randomInRange(10, 100));

                releaseTx.addOutput(releaseAmount, toAddress);

                // Input generation
                int numInputs = Helper.randomInRange(minNumInputs, maxNumInputs);
                for (int j = 0; j < numInputs; j++) {
                    Coin inputAmount = releaseAmount.divide(numInputs);
                    UldTransaction inputTx = new UldTransaction(networkParameters);
                    inputTx.addOutput(inputAmount, federation.getAddress());
                    releaseTx
                            .addInput(inputTx.getOutput(0))
                            .setScriptSig(PegTestUtils.createBaseInputScriptThatSpendsFromTheFederation(federation));
                }


                Keccak256 uscTxHash = new Keccak256(HashUtil.keccak256(BigInteger.valueOf(new Random().nextLong()).toByteArray()));
                txsWaitingForSignatures.put(uscTxHash, releaseTx);
            }
        };
    }
}
