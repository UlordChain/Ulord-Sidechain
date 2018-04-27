/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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

package co.usc.rpc;

import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.core.Context;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.RegTestParams;
import co.usc.config.UscMiningConstants;
import co.usc.config.UscSystemProperties;
import co.usc.core.NetworkStateExporter;
import co.usc.metrics.HashRateCalculator;
import co.usc.mine.*;
import co.usc.net.BlockProcessor;
import co.usc.rpc.exception.JsonRpcSubmitBlockException;
import co.usc.rpc.modules.eth.EthModule;
import co.usc.rpc.modules.personal.PersonalModule;
import co.usc.rpc.modules.txpool.TxPoolModule;
import co.usc.scoring.PeerScoringManager;
import org.apache.commons.lang3.ArrayUtils;
import org.ethereum.core.*;
import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.ethereum.facade.Ethereum;
import org.ethereum.net.client.ConfigCapabilities;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.server.PeerServer;
import org.ethereum.rpc.TypeConverter;
import org.ethereum.rpc.Web3Impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Handles requests for work and block submission.
 * Full responsibility for processing the request is delegated to MinerServer.
 *
 * @author Adrian Eidelman
 * @author Martin Medina
 */
public class Web3UscImpl extends Web3Impl {
    private static final Logger logger = LoggerFactory.getLogger("web3");
    private final NetworkStateExporter networkStateExporter;
    private final BlockStore blockStore;

    public Web3UscImpl(Ethereum eth,
                       Blockchain blockchain,
                       TransactionPool transactionPool,
                       UscSystemProperties properties,
                       MinerClient minerClient,
                       MinerServer minerServer,
                       PersonalModule personalModule,
                       EthModule ethModule,
                       TxPoolModule txPoolModule,
                       ChannelManager channelManager,
                       Repository repository,
                       PeerScoringManager peerScoringManager,
                       NetworkStateExporter networkStateExporter,
                       BlockStore blockStore,
                       ReceiptStore receiptStore,
                       PeerServer peerServer,
                       BlockProcessor nodeBlockProcessor,
                       HashRateCalculator hashRateCalculator,
                       ConfigCapabilities configCapabilities) {
        super(eth, blockchain, transactionPool, blockStore, receiptStore, properties, minerClient, minerServer, personalModule, ethModule, txPoolModule, channelManager, repository, peerScoringManager, peerServer, nodeBlockProcessor, hashRateCalculator, configCapabilities);
        this.networkStateExporter = networkStateExporter;
        this.blockStore = blockStore;
    }

    public MinerWork mnr_getWork() {
        logger.debug("mnr_getWork()");

        return minerServer.getWork();
    }

    public SubmittedBlockInfo mnr_submitBitcoinBlock(String bitcoinBlockHex) {
        logger.debug("mnr_submitBitcoinBlock(): {}", bitcoinBlockHex.length());

        NetworkParameters params = RegTestParams.get();
        new Context(params);

        UldBlock bitcoinBlock = getUldBlock(bitcoinBlockHex, params);
        UldTransaction coinbase = bitcoinBlock.getTransactions().get(0);

        String blockHashForMergedMining = extractBlockHashForMergedMining(coinbase);

        SubmitBlockResult result = minerServer.submitUlordBlock(blockHashForMergedMining, bitcoinBlock);

        return parseResultAndReturn(result);
    }

    public SubmittedBlockInfo mnr_submitBitcoinBlockPartialMerkle(
            String blockHashHex,
            String blockHeaderHex,
            String coinbaseHex,
            String merkleHashesHex,
            String blockTxnCountHex
    ) {
        logger.debug("mnr_submitBitcoinBlockPartialMerkle(): {}, {}, {}, {}, {}", blockHashHex, blockHeaderHex, coinbaseHex, merkleHashesHex, blockTxnCountHex);

        NetworkParameters params = RegTestParams.get();
        new Context(params);

        UldBlock bitcoinBlockWithHeaderOnly = getUldBlock(blockHeaderHex, params);
        UldTransaction coinbase = new UldTransaction(params, Hex.decode(coinbaseHex));

        String blockHashForMergedMining = extractBlockHashForMergedMining(coinbase);

        List<String> merkleHashes = parseHashes(merkleHashesHex);

        int txnCount = Integer.parseInt(blockTxnCountHex, 16);

        SubmitBlockResult result = minerServer.submitUlordBlockPartialMerkle(blockHashForMergedMining, bitcoinBlockWithHeaderOnly, coinbase, merkleHashes, txnCount);

        return parseResultAndReturn(result);
    }

    public SubmittedBlockInfo mnr_submitBitcoinBlockTransactions(
            String blockHashHex,
            String blockHeaderHex,
            String coinbaseHex,
            String txnHashesHex
    ) {
        logger.debug("mnr_submitBitcoinBlockTransactions(): {}, {}, {}, {}", blockHashHex, blockHeaderHex, coinbaseHex, txnHashesHex);

        NetworkParameters params = RegTestParams.get();
        new Context(params);

        UldBlock bitcoinBlockWithHeaderOnly = getUldBlock(blockHeaderHex, params);
        UldTransaction coinbase = new UldTransaction(params, Hex.decode(coinbaseHex));

        String blockHashForMergedMining = extractBlockHashForMergedMining(coinbase);

        List<String> txnHashes = parseHashes(txnHashesHex);

        SubmitBlockResult result = minerServer.submitUlordBlockTransactions(blockHashForMergedMining, bitcoinBlockWithHeaderOnly, coinbase, txnHashes);

        return parseResultAndReturn(result);
    }

    public void ext_dumpState() {
        Block bestBlcock = blockStore.getBestBlock();
        logger.info("Dumping state for block hash {}, block number {}", bestBlcock.getHash(), bestBlcock.getNumber());
        networkStateExporter.exportStatus(System.getProperty("user.dir") + "/" + "uscdump.json");
    }

    /**
     * Export the blockchain tree as a tgf file to user.dir/uscblockchain.tgf
     *
     * @param numberOfBlocks Number of block heights to include. Eg if best block is block 2300 and numberOfBlocks is 10, the graph will include blocks in heights 2290 to 2300.
     * @param includeUncles  Whether to show uncle links (recommended value is false)
     */
    public void ext_dumpBlockchain(long numberOfBlocks, boolean includeUncles) {
        Block bestBlock = blockStore.getBestBlock();
        logger.info("Dumping blockchain starting on block number {}, to best block number {}", bestBlock.getNumber() - numberOfBlocks, bestBlock.getNumber());
        PrintWriter writer = null;
        try {
            File graphFile = new File(System.getProperty("user.dir") + "/" + "uscblockchain.tgf");
            writer = new PrintWriter(new FileWriter(graphFile));

            List<Block> result = new LinkedList<>();
            long firstBlock = bestBlock.getNumber() - numberOfBlocks;
            if (firstBlock < 0) {
                firstBlock = 0;
            }
            for (long i = firstBlock; i < bestBlock.getNumber(); i++) {
                result.addAll(blockStore.getChainBlocksByNumber(i));
            }
            for (Block block : result) {
                writer.println(toSmallHash(block.getHash().getBytes()) + " " + block.getNumber() + "-" + toSmallHash(block.getHash().getBytes()));
            }
            writer.println("#");
            for (Block block : result) {
                writer.println(toSmallHash(block.getHash().getBytes()) + " " + toSmallHash(block.getParentHash().getBytes()) + " P");
                if (includeUncles) {
                    for (BlockHeader uncleHeader : block.getUncleList()) {
                        writer.println(toSmallHash(block.getHash().getBytes()) + " " + toSmallHash(uncleHeader.getHash().getBytes()) + " U");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Could nos save node graph to file", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private String toSmallHash(byte[] input) {
        return Hex.toHexString(input).substring(56, 64);
    }

    private UldBlock getUldBlock(String blockHeaderHex, NetworkParameters params) {
        byte[] bitcoinBlockByteArray = Hex.decode(blockHeaderHex);
        return params.getDefaultSerializer().makeBlock(bitcoinBlockByteArray);
    }

    private String extractBlockHashForMergedMining(UldTransaction coinbase) {
        byte[] coinbaseAsByteArray = coinbase.ulordSerialize();
        List<Byte> coinbaseAsByteList = Arrays.asList(ArrayUtils.toObject(coinbaseAsByteArray));

        List<Byte> uscTagAsByteList = Arrays.asList(ArrayUtils.toObject(UscMiningConstants.USC_TAG));

        int uscTagPosition = Collections.lastIndexOfSubList(coinbaseAsByteList, uscTagAsByteList);
        byte[] blockHashForMergedMiningArray = new byte[Keccak256Helper.Size.S256.getValue() / 8];
        System.arraycopy(coinbaseAsByteArray, uscTagPosition + UscMiningConstants.USC_TAG.length, blockHashForMergedMiningArray, 0, blockHashForMergedMiningArray.length);
        return TypeConverter.toJsonHex(blockHashForMergedMiningArray);
    }

    private List<String> parseHashes(String txnHashesHex) {
        String[] split = txnHashesHex.split("\\s+");
        return Arrays.asList(split);
    }

    private SubmittedBlockInfo parseResultAndReturn(SubmitBlockResult result) {
        if ("OK".equals(result.getStatus())) {
            return result.getBlockInfo();
        } else {
            throw new JsonRpcSubmitBlockException(result.getMessage());
        }
    }
}
