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
package co.usc;

import co.usc.blocks.BlockPlayer;
import co.usc.blocks.FileBlockPlayer;
import co.usc.blocks.FileBlockRecorder;
import co.usc.config.UscSystemProperties;
import co.usc.core.Usc;
import co.usc.core.UscImpl;
import co.usc.db.PruneConfiguration;
import co.usc.db.PruneService;
import co.usc.mine.MinerClient;
import co.usc.mine.MinerServer;
import co.usc.mine.TxBuilder;
import co.usc.mine.TxBuilderEx;
import co.usc.net.*;
import co.usc.net.discovery.UDPServer;
import co.usc.rpc.netty.Web3HttpServer;
import co.usc.rpc.netty.Web3WebSocketServer;
import org.ethereum.core.*;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.server.PeerServer;
import org.ethereum.rpc.Web3;
import org.ethereum.sync.SyncPool;
import org.ethereum.util.BuildInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

@Component
public class FullNodeRunner implements NodeRunner {
    private static Logger logger = LoggerFactory.getLogger("fullnoderunner");

    private final Usc usc;
    private final UDPServer udpServer;
    private final MinerServer minerServer;
    private final MinerClient minerClient;
    private final UscSystemProperties uscSystemProperties;
    private final Web3HttpServer web3HttpServer;
    private final Web3WebSocketServer web3WebSocketServer;
    private final Repository repository;
    private final Blockchain blockchain;
    private final ChannelManager channelManager;
    private final SyncPool syncPool;
    private final MessageHandler messageHandler;

    private final Web3 web3Service;
    private final BlockProcessor nodeBlockProcessor;
    private final TransactionPool transactionPool;
    private final PeerServer peerServer;
    private final SyncPool.PeerClientFactory peerClientFactory;
    private final TransactionGateway transactionGateway;

    private final PruneService pruneService;

    @Autowired
    public FullNodeRunner(
            Usc usc,
            UDPServer udpServer,
            MinerServer minerServer,
            MinerClient minerClient,
            UscSystemProperties uscSystemProperties,
            Web3 web3Service,
            Web3HttpServer web3HttpServer,
            Web3WebSocketServer web3WebSocketServer,
            Repository repository,
            Blockchain blockchain,
            ChannelManager channelManager,
            SyncPool syncPool,
            MessageHandler messageHandler,
            BlockProcessor nodeBlockProcessor,
            TransactionPool transactionPool,
            PeerServer peerServer,
            SyncPool.PeerClientFactory peerClientFactory,
            TransactionGateway transactionGateway) {
        this.usc = usc;
        this.udpServer = udpServer;
        this.minerServer = minerServer;
        this.minerClient = minerClient;
        this.uscSystemProperties = uscSystemProperties;
        this.web3HttpServer = web3HttpServer;
        this.web3Service = web3Service;
        this.web3WebSocketServer = web3WebSocketServer;
        this.repository = repository;
        this.blockchain = blockchain;
        this.channelManager = channelManager;
        this.syncPool = syncPool;
        this.messageHandler = messageHandler;
        this.nodeBlockProcessor = nodeBlockProcessor;
        this.transactionPool = transactionPool;
        this.peerServer = peerServer;
        this.peerClientFactory = peerClientFactory;
        this.transactionGateway = transactionGateway;

        PruneConfiguration pruneConfiguration = uscSystemProperties.getPruneConfiguration();
        this.pruneService = new PruneService(pruneConfiguration, uscSystemProperties, blockchain, PrecompiledContracts.REMASC_ADDR);
    }

    @Override
    public void run() throws Exception {
        logger.info("Starting USC");

        logger.info(
                "Running {},  core version: {}-{}",
                uscSystemProperties.genesisInfo(),
                uscSystemProperties.projectVersion(),
                uscSystemProperties.projectVersionModifier()
        );
        BuildInfo.printInfo();

        transactionGateway.start();
        // this should be the genesis block at this point
        transactionPool.start(blockchain.getBestBlock());
        channelManager.start();
        messageHandler.start();
        peerServer.start();

        if (logger.isInfoEnabled()) {
            String versions = EthVersion.supported().stream().map(EthVersion::name).collect(Collectors.joining(", "));
            logger.info("Capability eth version: [{}]", versions);
        }
        if (uscSystemProperties.isBlocksEnabled()) {
            setupRecorder(uscSystemProperties.blocksRecorder());
            setupPlayer(usc, channelManager, blockchain, uscSystemProperties.blocksPlayer());
        }

        if (!"".equals(uscSystemProperties.blocksLoader())) {
            uscSystemProperties.setSyncEnabled(Boolean.FALSE);
            uscSystemProperties.setDiscoveryEnabled(Boolean.FALSE);
        }

        Metrics.registerNodeID(uscSystemProperties.nodeId());

        if (uscSystemProperties.simulateTxs()) {
            enableSimulateTxs();
        }

        if (uscSystemProperties.simulateTxsEx()) {
            enableSimulateTxsEx();
        }

        startWeb3(uscSystemProperties);

        if (uscSystemProperties.isPeerDiscoveryEnabled()) {
            udpServer.start();
        }

        if (uscSystemProperties.isSyncEnabled()) {
            syncPool.updateLowerUsefulDifficulty();
            syncPool.start(peerClientFactory);
            if (uscSystemProperties.waitForSync()) {
                waitUscSyncDone();
            }
        }

        if (uscSystemProperties.isMinerServerEnabled()) {
            minerServer.start();

            if (uscSystemProperties.isMinerClientEnabled()) {
                minerClient.mine();
            }
        }

        if (uscSystemProperties.isPruneEnabled()) {
            pruneService.start();
        }

        logger.info("done");
    }

    private void startWeb3(UscSystemProperties uscSystemProperties) throws InterruptedException {
        boolean rpcHttpEnabled = uscSystemProperties.isRpcHttpEnabled();
        boolean rpcWebSocketEnabled = uscSystemProperties.isRpcWebSocketEnabled();

        if (rpcHttpEnabled || rpcWebSocketEnabled) {
            web3Service.start();
        }

        if (rpcHttpEnabled) {
            logger.info("RPC HTTP enabled");
            web3HttpServer.start();
        } else {
            logger.info("RPC HTTP disabled");
        }

        if (rpcWebSocketEnabled) {
            logger.info("RPC WebSocket enabled");
            web3WebSocketServer.start();
        } else {
            logger.info("RPC WebSocket disabled");
        }
    }

    private void enableSimulateTxs() {
        new TxBuilder(uscSystemProperties, usc, nodeBlockProcessor, repository).simulateTxs();
    }

    private void enableSimulateTxsEx() {
        new TxBuilderEx(uscSystemProperties, usc, repository, nodeBlockProcessor, transactionPool).simulateTxs();
    }

    private void waitUscSyncDone() throws InterruptedException {
        while (usc.isBlockchainEmpty() || usc.hasBetterBlockToSync() || usc.isPlayingBlocks()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                logger.trace("Wait sync done was interrupted", e1);
                throw e1;
            }
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down USC node");

        if (uscSystemProperties.isPruneEnabled()) {
            pruneService.stop();
        }

        syncPool.stop();

        boolean rpcHttpEnabled = uscSystemProperties.isRpcHttpEnabled();
        boolean rpcWebSocketEnabled = uscSystemProperties.isRpcWebSocketEnabled();
        if (rpcHttpEnabled) {
            web3HttpServer.stop();
        }
        if (rpcWebSocketEnabled) {
            try {
                web3WebSocketServer.stop();
            } catch (InterruptedException e) {
                logger.error("Couldn't stop the WebSocket server", e);
                Thread.currentThread().interrupt();
            }
        }

        if (rpcHttpEnabled || rpcWebSocketEnabled) {
            web3Service.stop();
        }

        if (uscSystemProperties.isMinerServerEnabled()) {
            minerServer.stop();
            if (uscSystemProperties.isMinerClientEnabled()) {
                minerClient.stop();
            }
        }

        peerServer.stop();
        messageHandler.stop();
        channelManager.stop();
        transactionGateway.stop();

        if (uscSystemProperties.isPeerDiscoveryEnabled()) {
            try {
                udpServer.stop();
            } catch (InterruptedException e) {
                logger.error("Couldn't stop the updServer", e);
                Thread.currentThread().interrupt();
            }
        }

        logger.info("USC node Shut down");
    }

    private void setupRecorder(@Nullable String blocksRecorderFileName) {
        if (blocksRecorderFileName != null) {
            blockchain.setBlockRecorder(new FileBlockRecorder(blocksRecorderFileName));
        }
    }

    private void setupPlayer(Usc usc, ChannelManager cm, Blockchain bc, @Nullable String blocksPlayerFileName) {
        if (blocksPlayerFileName == null) {
            return;
        }

        new Thread(() -> {
            UscImpl uscImpl = (UscImpl) usc;
            try (FileBlockPlayer bplayer = new FileBlockPlayer(uscSystemProperties, blocksPlayerFileName)) {
                uscImpl.setIsPlayingBlocks(true);
                connectBlocks(bplayer, bc, cm);
            } catch (Exception e) {
                logger.error("Error", e);
            } finally {
                uscImpl.setIsPlayingBlocks(false);
            }
        }).start();
    }

    private void connectBlocks(FileBlockPlayer bplayer, Blockchain bc, ChannelManager cm) {
        for (Block block = bplayer.readBlock(); block != null; block = bplayer.readBlock()) {
            ImportResult tryToConnectResult = bc.tryToConnect(block);
            if (BlockProcessResult.importOk(tryToConnectResult)) {
                cm.broadcastBlock(block);
            }
        }
    }
}
