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

import co.usc.ulordj.core.Coin;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.Federation;
import org.ethereum.core.Repository;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class GetFeePerKbTest extends BridgePerformanceTestCase {
    private Federation federation;

    @Test
    public void getFeePerKb() throws IOException {
        ExecutionStats stats = new ExecutionStats("getFeePerKb");
        ABIEncoder abiEncoder = (int executionIndex) -> Bridge.GET_FEE_PER_KB.encode();
        executeTestCaseSection(abiEncoder, "getFeePerKb", true,50, stats);
        executeTestCaseSection(abiEncoder, "getFeePerKb", false,500, stats);
        BridgePerformanceTest.addStats(stats);
    }

    private void executeTestCaseSection(ABIEncoder abiEncoder, String name, boolean genesis, int times, ExecutionStats stats) {
        executeAndAverage(
                String.format("%s-%s", name, genesis ? "genesis" : "non-genesis"),
                times, abiEncoder,
                buildInitializer(genesis),
                Helper.getZeroValueRandomSenderTxBuilder(),
                Helper.getRandomHeightProvider(10),
                stats
        );
    }

    private BridgeStorageProviderInitializer buildInitializer(boolean genesis) {
        return (BridgeStorageProvider provider, Repository repository, int executionIndex) -> {
            if (!genesis) {
                provider.setFeePerKb(Helper.randomCoin(Coin.MILLICOIN, 1, 100));
            } else {
                federation = bridgeConstants.getGenesisFederation();
            }
        };
    }


}
