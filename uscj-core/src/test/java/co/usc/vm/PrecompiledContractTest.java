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

package co.usc.vm;

import co.usc.config.TestSystemProperties;
import co.usc.peg.Bridge;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.PrecompiledContracts.PrecompiledContract;
import org.junit.Assert;
import org.junit.Test;

public class PrecompiledContractTest {

    private final TestSystemProperties config = new TestSystemProperties();
    private final PrecompiledContracts precompiledContracts = new PrecompiledContracts(config);

    @Test
    public void getBridgeContract() {
        DataWord bridgeAddress = new DataWord(PrecompiledContracts.BRIDGE_ADDR.getBytes());
        PrecompiledContract bridge = precompiledContracts.getContractForAddress(null, bridgeAddress);

        Assert.assertNotNull(bridge);
        Assert.assertEquals(Bridge.class, bridge.getClass());
    }

    @Test
    public void getBridgeContractTwice() {
        DataWord bridgeAddress = new DataWord(PrecompiledContracts.BRIDGE_ADDR.getBytes());
        PrecompiledContract bridge1 = precompiledContracts.getContractForAddress(null, bridgeAddress);
        PrecompiledContract bridge2 = precompiledContracts.getContractForAddress(null, bridgeAddress);

        Assert.assertNotNull(bridge1);
        Assert.assertNotNull(bridge2);
        Assert.assertNotSame(bridge1, bridge2);
    }
}
