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
package co.usc.remasc;

import co.usc.config.BridgeConstants;
import co.usc.config.UscSystemProperties;
import co.usc.core.UscAddress;
import co.usc.peg.BridgeStorageConfiguration;
import co.usc.peg.BridgeStorageProvider;
import co.usc.peg.FederationSupport;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.ethereum.vm.PrecompiledContracts;

/**
 * Created by ajlopez on 14/11/2017.
 */
public class RemascFederationProvider {
    private final FederationSupport federationSupport;

    public RemascFederationProvider(
            UscSystemProperties config,
            Repository repository,
            Block processingBlock) {
        BridgeConstants bridgeConstants = config.getBlockchainConfig().getCommonConstants().getBridgeConstants();
        BridgeStorageProvider bridgeStorageProvider = new BridgeStorageProvider(
                repository,
                PrecompiledContracts.BRIDGE_ADDR,
                bridgeConstants,
                BridgeStorageConfiguration.fromBlockchainConfig(config.getBlockchainConfig().getConfigForBlock(processingBlock.getNumber()))
        );
        this.federationSupport = new FederationSupport(
                bridgeStorageProvider,
                bridgeConstants,
                processingBlock
        );
    }

    public int getFederationSize() {
        return this.federationSupport.getFederationSize();
    }

    public UscAddress getFederatorAddress(int n) {
        byte[] publicKey = this.federationSupport.getFederatorPublicKey(n);
        return new UscAddress(ECKey.fromPublicOnly(publicKey).getAddress());
    }
}
