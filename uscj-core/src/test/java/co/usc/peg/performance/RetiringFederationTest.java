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
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.Federation;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Repository;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Ignore
public class RetiringFederationTest extends BridgePerformanceTestCase {
    private Federation retiringFederation;

    @Test
    public void getRetiringFederationAddress() throws IOException {
        executeTestCase(Bridge.GET_RETIRING_FEDERATION_ADDRESS);
    }

    @Test
    public void getRetiringFederationSize() throws IOException {
        executeTestCase(Bridge.GET_RETIRING_FEDERATION_SIZE);
    }

    @Test
    public void getRetiringFederationThreshold() throws IOException {
        executeTestCase(Bridge.GET_RETIRING_FEDERATION_THRESHOLD);
    }

    @Test
    public void getRetiringFederationCreationTime() throws IOException {
        executeTestCase(Bridge.GET_RETIRING_FEDERATION_CREATION_TIME);
    }

    @Test
    public void getRetiringFederationCreationBlockNumber() throws IOException {
        executeTestCase(Bridge.GET_RETIRING_FEDERATION_CREATION_BLOCK_NUMBER);
    }

    @Test
    public void getRetiringFederatorPublicKey() throws IOException {
        ExecutionStats stats = new ExecutionStats("getRetiringFederatorPublicKey");
        ABIEncoder abiEncoder;
        abiEncoder = (int executionIndex) -> Bridge.GET_RETIRING_FEDERATOR_PUBLIC_KEY.encode(new Object[]{Helper.randomInRange(0, retiringFederation.getPublicKeys().size()-1)});
        executeTestCaseSection(abiEncoder, "getRetiringFederatorPublicKey", true,50, stats);
        abiEncoder = (int executionIndex) -> Bridge.GET_RETIRING_FEDERATOR_PUBLIC_KEY.encode(new Object[]{Helper.randomInRange(0, 10)});
        executeTestCaseSection(abiEncoder, "getRetiringFederatorPublicKey", false,500, stats);
        BridgePerformanceTest.addStats(stats);
    }

    private void executeTestCase(CallTransaction.Function fn) {
        ExecutionStats stats = new ExecutionStats(fn.name);
        executeTestCaseSection(fn,true,50, stats);
        executeTestCaseSection(fn,false,500, stats);
        BridgePerformanceTest.addStats(stats);
    }

    private void executeTestCaseSection(CallTransaction.Function fn, boolean genesis, int times, ExecutionStats stats) {
        executeTestCaseSection((int executionIndex) -> fn.encode(), fn.name, genesis, times, stats);
    }

    private void executeTestCaseSection(ABIEncoder abiEncoder, String name, boolean present, int times, ExecutionStats stats) {
        executeAndAverage(
                String.format("%s-%s", name, present ? "present" : "not-present"),
                times, abiEncoder,
                buildInitializer(present),
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(11, 15),
                stats
        );
    }

    private BridgeStorageProviderInitializer buildInitializer(boolean present) {
        final int minFederators = 10;
        final int maxFederators = 16;

        return (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            if (present) {
                int numFederators = Helper.randomInRange(minFederators, maxFederators);
                List<UldECKey> federatorKeys = new ArrayList<>();
                for (int i = 0; i < numFederators; i++) {
                    federatorKeys.add(new UldECKey());
                }
                retiringFederation = new Federation(
                        federatorKeys,
                        Instant.ofEpochMilli(new Random().nextLong()),
                        Helper.randomInRange(1, 10),
                        networkParameters
                );
                provider.setNewFederation(bridgeConstants.getGenesisFederation());
                provider.setOldFederation(retiringFederation);
            } else {
                retiringFederation = null;
            }
        };
    }


}
