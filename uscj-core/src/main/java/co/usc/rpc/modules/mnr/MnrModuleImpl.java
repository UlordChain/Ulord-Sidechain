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

package co.usc.rpc.modules.mnr;

import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.core.Context;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.RegTestParams;
import co.usc.config.UscMiningConstants;
import co.usc.mine.*;
import co.usc.rpc.exception.JsonRpcSubmitBlockException;
import com.google.common.collect.EvictingQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.rpc.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

@Component
public class MnrModuleImpl implements MnrModule, Runnable {

    private static Queue<String> submittedQueue = EvictingQueue.create(10);

    private Thread processUlordBlockPartialMerkleThread;

    private static final Logger logger = LoggerFactory.getLogger("web3");

    private final MinerServer minerServer;

    @Autowired
    public MnrModuleImpl(MinerServer minerServer) {
        this.minerServer = minerServer;
    }

    @Override
    public MinerWork getWork() {
        logger.debug("getWork()");
        return minerServer.getWork();
    }

    @Override
    public SubmittedBlockInfo submitUlordBlock(String ulordBlockHex) {
        logger.debug("submitUlordBlock(): {}", ulordBlockHex.length());

        NetworkParameters params = RegTestParams.get();
        new Context(params);

        UldBlock ulordBlock = getUldBlock(ulordBlockHex, params);
        UldTransaction coinbase = ulordBlock.getTransactions().get(0);

        String blockHashForMergedMining = extractBlockHashForMergedMining(coinbase);

        SubmitBlockResult result = minerServer.submitUlordBlock(blockHashForMergedMining, ulordBlock);

        return parseResultAndReturn(result);
    }

    @Override
    public SubmittedBlockInfo submitUlordBlockTransactions(String blockHashHex, String blockHeaderHex, String coinbaseHex, String txnHashesHex) {
        logger.debug("submitUlordBlockTransactions(): {}, {}, {}, {}", blockHashHex, blockHeaderHex, coinbaseHex, txnHashesHex);

        NetworkParameters params = RegTestParams.get();
        new Context(params);

        UldBlock ulordBlockWithHeaderOnly = getUldBlock(blockHeaderHex, params);
        UldTransaction coinbase = new UldTransaction(params, Hex.decode(coinbaseHex));

        String blockHashForMergedMining = extractBlockHashForMergedMining(coinbase);

        List<String> txnHashes = parseHashes(txnHashesHex);

        SubmitBlockResult result = minerServer.submitUlordBlockTransactions(blockHashForMergedMining, ulordBlockWithHeaderOnly, coinbase, txnHashes);

        return parseResultAndReturn(result);
    }

    @Override
    public SubmittedBlockInfo submitUlordBlockPartialMerkle(String blockHashHex, String blockHeaderHex, String coinbaseHex, String merkleHashesHex, String blockTxnCountHex) {
        synchronized (this) {
            submittedQueue.add(blockHashHex + ":" + blockHeaderHex + ":" + coinbaseHex + ":" + merkleHashesHex + ":" + blockTxnCountHex);
        }

        logger.debug("Queue Size: " + submittedQueue.size());
        if(processUlordBlockPartialMerkleThread == null) {
            processUlordBlockPartialMerkleThread = new Thread(this);
            processUlordBlockPartialMerkleThread.setName("processUlordBlockPartialMerkle");
            processUlordBlockPartialMerkleThread.start();
        }

        return parseResultAndReturn(new SubmitBlockResult("OK", "OK"));
    }

    private UldBlock getUldBlock(String blockHeaderHex, NetworkParameters params) {
        byte[] ulordBlockByteArray = Hex.decode(blockHeaderHex);
        return params.getDefaultSerializer().makeBlock(ulordBlockByteArray);
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

    private void processUlordBlockPartialMerkle() {

        while(true) {
            try {
                if (submittedQueue.isEmpty()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        logger.warn("processUlordBlockPartialMerkle thread interrupted: " + ex);
                    }
                    continue;
                }

                String data = "";
                synchronized (this) {
                    data = submittedQueue.poll();
                }

                if (data == null) continue;
                String[] dataObjects = data.split(":");

                String blockHashHex = dataObjects[0];
                String blockHeaderHex = dataObjects[1];
                String coinbaseHex = dataObjects[2];
                String merkleHashesHex = dataObjects[3];
                String blockTxnCountHex = dataObjects[4];

                logger.debug("submitUlordBlockPartialMerkle(): {}, {}, {}, {}, {}", blockHashHex, blockHeaderHex, coinbaseHex, merkleHashesHex, blockTxnCountHex);

                NetworkParameters params = RegTestParams.get();
                new Context(params);

                UldBlock ulordBlockWithHeaderOnly = getUldBlock(blockHeaderHex, params);
                UldTransaction coinbase = new UldTransaction(params, Hex.decode(coinbaseHex));

                String blockHashForMergedMining = extractBlockHashForMergedMining(coinbase);

                List<String> merkleHashes = parseHashes(merkleHashesHex);

                int txnCount = Integer.parseInt(blockTxnCountHex, 16);

                SubmitBlockResult result = minerServer.submitUlordBlockPartialMerkle(blockHashForMergedMining, ulordBlockWithHeaderOnly, coinbase, merkleHashes, txnCount);

                logger.debug("result" + result.getMessage() + " " + result.getStatus() + " " + result.getBlockInfo().getBlockIncludedHeight());
            } catch (Exception e) {
                logger.warn("Exception in processUlordBlockPartialMerkle:" + e);
            }

        }
    }

    @Override
    public void run() {
        processUlordBlockPartialMerkle();
    }
}
