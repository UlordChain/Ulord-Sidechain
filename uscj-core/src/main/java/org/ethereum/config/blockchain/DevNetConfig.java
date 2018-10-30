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

package org.ethereum.config.blockchain;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeDevNetConstants;
import org.ethereum.config.blockchain.testnet.TestNetAfterBridgeSyncConfig;
//import org.ethereum.config.blockchain.testnet.TestNetAfterBridgeSyncConfig;

/**
 * Created by Oscar Guindzberg on 25.10.2016.
 */
public class DevNetConfig extends TestNetAfterBridgeSyncConfig {

    public static class DevNetConstants extends TestNetConstants {
        private static final byte CHAIN_ID = 52;
        @Override
        public BridgeConstants getBridgeConstants() {
            return BridgeDevNetConstants.getInstance();
        }

        @Override
        public byte getChainId() {
            return DevNetConstants.CHAIN_ID;
        }
    }

    public DevNetConfig() {
        super(new DevNetConstants());
    }

    @Override
    public boolean areBridgeTxsFree() {
        return true;
    }

    @Override
    public boolean isUscIP89() {
        return true;
    }


}
