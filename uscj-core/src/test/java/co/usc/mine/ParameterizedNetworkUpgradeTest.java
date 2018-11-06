/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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

package co.usc.mine;

import co.usc.config.TestSystemProperties;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.config.blockchain.HardForkActivationConfig;
import org.ethereum.config.net.RegTestConfig;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * This class helps when running tests that should work both before and after a hard fork.
 * To use it, you need to:
 * 1. Extend it
 * 2. Create a public constructor that receives a TestSystemProperties
 * 3. call super(config)
 *
 * It will execute your test cases once per fork, with adequate configuration object arguments.
 */
@RunWith(Parameterized.class)
public abstract class ParameterizedNetworkUpgradeTest {

    @Parameterized.Parameters(name = "Network version: {0}")
    public static Object[] data() {
        TestSystemProperties tagoreConfig = new TestSystemProperties() {
            @Override
            protected BlockchainNetConfig buildBlockchainConfig() {
                return RegTestConfig.getFromConfig(new HardForkActivationConfig(Integer.MAX_VALUE));
            }

            @Override
            public String toString() {
                return "Tagore";
            }
        };
        TestSystemProperties shakespeareConfig = new TestSystemProperties() {
            @Override
            protected BlockchainNetConfig buildBlockchainConfig() {
                return RegTestConfig.getFromConfig(new HardForkActivationConfig(0));
            }

            @Override
            public String toString() {
                return "Shakespeare";
            }
        };
        return new Object[] { tagoreConfig, shakespeareConfig };
    }

    protected final TestSystemProperties config;

    protected ParameterizedNetworkUpgradeTest(TestSystemProperties config) {
        this.config = config;
    }
}
