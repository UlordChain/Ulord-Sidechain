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

package co.usc.core;

import co.usc.config.UscSystemProperties;
import co.usc.net.NodeBlockProcessor;
import org.ethereum.core.Blockchain;
import org.ethereum.core.TransactionPool;
import org.ethereum.facade.EthereumImpl;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.server.PeerServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UscImpl extends EthereumImpl implements Usc {

    private boolean isplaying;
    private final NodeBlockProcessor nodeBlockProcessor;

    @Autowired
    public UscImpl(
            ChannelManager channelManager,
            PeerServer peerServer,
            TransactionPool transactionPool,
            UscSystemProperties config,
            CompositeEthereumListener compositeEthereumListener,
            NodeBlockProcessor nodeBlockProcessor,
            ReversibleTransactionExecutor reversibleTransactionExecutor,
            Blockchain blockchain) {
        super(
                config,
                channelManager,
                transactionPool,
                compositeEthereumListener,
                blockchain
        );
        this.nodeBlockProcessor = nodeBlockProcessor;
    }

    @Override
    public boolean isPlayingBlocks() {
        return isplaying;
    }

    @Override
    public boolean isBlockchainEmpty() {
        return this.nodeBlockProcessor.getBestBlockNumber() == 0;
    }

    public void setIsPlayingBlocks(boolean value) {
        isplaying = value;
    }

    @Override
    public boolean hasBetterBlockToSync() {
        return this.nodeBlockProcessor.hasBetterBlockToSync();
    }
}
