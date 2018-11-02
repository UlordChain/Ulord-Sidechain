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

package org.ethereum.config;

import co.usc.cli.CliArgs;
import co.usc.config.*;
import co.usc.core.DifficultyCalculator;
import co.usc.core.NetworkStateExporter;
import co.usc.crypto.Keccak256;
import co.usc.metrics.BlockHeaderElement;
import co.usc.metrics.HashRateCalculator;
import co.usc.metrics.HashRateCalculatorMining;
import co.usc.metrics.HashRateCalculatorNonMining;
import co.usc.net.discovery.PeerExplorer;
import co.usc.net.discovery.UDPServer;
import co.usc.net.discovery.table.KademliaOptions;
import co.usc.net.discovery.table.NodeDistanceTable;
import co.usc.util.UscCustomCache;
import co.usc.validators.*;
import org.apache.commons.collections4.CollectionUtils;
import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.ethereum.datasource.KeyValueDataSource;
import org.ethereum.datasource.LevelDbDataSource;
import org.ethereum.db.*;
import org.ethereum.net.rlpx.Node;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.ethereum.db.IndexedBlockStore.BLOCK_INFO_SERIALIZER;

/**
 * @author Roman Mandeleil
 *         Created on: 27/01/2015 01:05
 */
@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {
    private static Logger logger = LoggerFactory.getLogger("general");

    @Bean
    public BlockStore blockStore(UscSystemProperties config) {
        return buildBlockStore(config.databaseDir());
    }

    public BlockStore buildBlockStore(String databaseDir) {
        File blockIndexDirectory = new File(databaseDir + "/blocks/");
        File dbFile = new File(blockIndexDirectory, "index");
        if (!blockIndexDirectory.exists()) {
            boolean mkdirsSuccess = blockIndexDirectory.mkdirs();
            if (!mkdirsSuccess) {
                logger.error("Unable to create blocks directory: {}", blockIndexDirectory);
            }
        }

        DB indexDB = DBMaker.fileDB(dbFile)
                .closeOnJvmShutdown()
                .make();

        Map<Long, List<IndexedBlockStore.BlockInfo>> indexMap = indexDB.hashMapCreate("index")
                .keySerializer(Serializer.LONG)
                .valueSerializer(BLOCK_INFO_SERIALIZER)
                .counterEnable()
                .makeOrGet();

        KeyValueDataSource blocksDB = new LevelDbDataSource("blocks", databaseDir);
        blocksDB.init();

        return new IndexedBlockStore(indexMap, blocksDB, indexDB);
    }

    @Bean
    public ReceiptStore receiptStore(UscSystemProperties config) {
        return buildReceiptStore(config.databaseDir());
    }

    public ReceiptStore buildReceiptStore(String databaseDir) {
        KeyValueDataSource ds = new LevelDbDataSource("receipts", databaseDir);
        ds.init();
        return new ReceiptStoreImpl(ds);
    }

    @Bean
    public HashRateCalculator hashRateCalculator(UscSystemProperties uscSystemProperties, BlockStore blockStore, MiningConfig miningConfig) {
        UscCustomCache<Keccak256, BlockHeaderElement> cache = new UscCustomCache<>(60000L);
        if (!uscSystemProperties.isMinerServerEnabled()) {
            return new HashRateCalculatorNonMining(blockStore, cache);
        }

        return new HashRateCalculatorMining(blockStore, cache, miningConfig.getCoinbaseAddress());
    }

    @Bean
    public MiningConfig miningConfig(UscSystemProperties uscSystemProperties) {
        return new MiningConfig(
                uscSystemProperties.coinbaseAddress(),
                uscSystemProperties.minerMinFeesNotifyInDollars(),
                uscSystemProperties.minerGasUnitInDollars(),
                uscSystemProperties.minerMinGasPrice(),
                uscSystemProperties.getBlockchainConfig().getCommonConstants().getUncleListLimit(),
                uscSystemProperties.getBlockchainConfig().getCommonConstants().getUncleGenerationLimit(),
                new GasLimitConfig(
                        uscSystemProperties.getBlockchainConfig().getCommonConstants().getMinGasLimit(),
                        uscSystemProperties.getTargetGasLimit(),
                        uscSystemProperties.getForceTargetGasLimit()
                )
        );
    }

    @Bean
    public UscSystemProperties uscSystemProperties(CliArgs<NodeCliOptions, NodeCliFlags> cliArgs) {
        return new UscSystemProperties(new ConfigLoader(cliArgs));
    }

    @Bean
    public BlockParentDependantValidationRule blockParentDependantValidationRule(
            Repository repository,
            UscSystemProperties config,
            DifficultyCalculator difficultyCalculator) {
        BlockTxsValidationRule blockTxsValidationRule = new BlockTxsValidationRule(repository);
        BlockTxsFieldsValidationRule blockTxsFieldsValidationRule = new BlockTxsFieldsValidationRule();
        PrevMinGasPriceRule prevMinGasPriceRule = new PrevMinGasPriceRule();
        BlockParentNumberRule parentNumberRule = new BlockParentNumberRule();
        BlockDifficultyRule difficultyRule = new BlockDifficultyRule(difficultyCalculator);
        BlockParentGasLimitRule parentGasLimitRule = new BlockParentGasLimitRule(config.getBlockchainConfig().
                getCommonConstants().getGasLimitBoundDivisor());

        return new BlockParentCompositeRule(blockTxsFieldsValidationRule, blockTxsValidationRule, prevMinGasPriceRule, parentNumberRule, difficultyRule, parentGasLimitRule);
    }

    @Bean(name = "blockValidationRule")
    public BlockValidationRule blockValidationRule(
            BlockStore blockStore,
            UscSystemProperties config,
            DifficultyCalculator difficultyCalculator,
            ProofOfWorkRule proofOfWorkRule) {
        Constants commonConstants = config.getBlockchainConfig().getCommonConstants();
        int uncleListLimit = commonConstants.getUncleListLimit();
        int uncleGenLimit = commonConstants.getUncleGenerationLimit();
        int validPeriod = commonConstants.getNewBlockMaxSecondsInTheFuture();
        BlockTimeStampValidationRule blockTimeStampValidationRule = new BlockTimeStampValidationRule(validPeriod);

        BlockParentGasLimitRule parentGasLimitRule = new BlockParentGasLimitRule(commonConstants.getGasLimitBoundDivisor());
        BlockParentCompositeRule unclesBlockParentHeaderValidator = new BlockParentCompositeRule(new PrevMinGasPriceRule(), new BlockParentNumberRule(), blockTimeStampValidationRule, new BlockDifficultyRule(difficultyCalculator), parentGasLimitRule);

        BlockCompositeRule unclesBlockHeaderValidator = new BlockCompositeRule(proofOfWorkRule, blockTimeStampValidationRule, new ValidGasUsedRule());

        BlockUnclesValidationRule blockUnclesValidationRule = new BlockUnclesValidationRule(config, blockStore, uncleListLimit, uncleGenLimit, unclesBlockHeaderValidator, unclesBlockParentHeaderValidator);

        int minGasLimit = commonConstants.getMinGasLimit();
        int maxExtraDataSize = commonConstants.getMaximumExtraDataSize();

        return new BlockCompositeRule(new TxsMinGasPriceRule(), blockUnclesValidationRule, new BlockRootValidationRule(), new RemascValidationRule(), blockTimeStampValidationRule, new GasLimitRule(minGasLimit), new ExtraDataRule(maxExtraDataSize));
    }

    @Bean
    public NetworkStateExporter networkStateExporter(Repository repository) {
        return new NetworkStateExporter(repository);
    }


    @Bean(name = "minerServerBlockValidation")
    public BlockValidationRule minerServerBlockValidationRule(
            BlockStore blockStore,
            UscSystemProperties config,
            DifficultyCalculator difficultyCalculator,
            ProofOfWorkRule proofOfWorkRule) {
        Constants commonConstants = config.getBlockchainConfig().getCommonConstants();
        int uncleListLimit = commonConstants.getUncleListLimit();
        int uncleGenLimit = commonConstants.getUncleGenerationLimit();

        BlockParentGasLimitRule parentGasLimitRule = new BlockParentGasLimitRule(commonConstants.getGasLimitBoundDivisor());
        BlockParentCompositeRule unclesBlockParentHeaderValidator = new BlockParentCompositeRule(new PrevMinGasPriceRule(), new BlockParentNumberRule(), new BlockDifficultyRule(difficultyCalculator), parentGasLimitRule);

        int validPeriod = commonConstants.getNewBlockMaxSecondsInTheFuture();
        BlockTimeStampValidationRule blockTimeStampValidationRule = new BlockTimeStampValidationRule(validPeriod);
        BlockCompositeRule unclesBlockHeaderValidator = new BlockCompositeRule(proofOfWorkRule, blockTimeStampValidationRule, new ValidGasUsedRule());

        return new BlockUnclesValidationRule(config, blockStore, uncleListLimit, uncleGenLimit, unclesBlockHeaderValidator, unclesBlockParentHeaderValidator);
    }

    @Bean
    public PeerExplorer peerExplorer(UscSystemProperties uscConfig) {
        ECKey key = uscConfig.getMyKey();
        Integer networkId = uscConfig.networkId();
        Node localNode = new Node(key.getNodeId(), uscConfig.getPublicIp(), uscConfig.getPeerPort());
        NodeDistanceTable distanceTable = new NodeDistanceTable(KademliaOptions.BINS, KademliaOptions.BUCKET_SIZE, localNode);
        long msgTimeOut = uscConfig.peerDiscoveryMessageTimeOut();
        long refreshPeriod = uscConfig.peerDiscoveryRefreshPeriod();
        long cleanPeriod = uscConfig.peerDiscoveryCleanPeriod();
        List<String> initialBootNodes = uscConfig.peerDiscoveryIPList();
        List<Node> activePeers = uscConfig.peerActive();
        if(CollectionUtils.isNotEmpty(activePeers)) {
            for(Node n : activePeers) {
                InetSocketAddress address = n.getAddress();
                initialBootNodes.add(address.getHostName() + ":" + address.getPort());
            }
        }
        return new PeerExplorer(initialBootNodes, localNode, distanceTable, key, msgTimeOut, refreshPeriod, cleanPeriod, networkId);
    }

    @Bean
    public UDPServer udpServer(PeerExplorer peerExplorer, UscSystemProperties uscConfig) {
        return new UDPServer(uscConfig.getBindAddress().getHostAddress(), uscConfig.getPeerPort(), peerExplorer);
    }
}
