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

package co.usc.mine;

import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.config.UscMiningConstants;
import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.core.bc.PendingState;
import co.usc.crypto.Keccak256;
import co.usc.remasc.RemascTransaction;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.core.TransactionPool;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.rpc.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.Arrays;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

public class MinerUtils {

    private static final Logger logger = LoggerFactory.getLogger("minerserver");

    public static co.usc.ulordj.core.UldTransaction getUlordMergedMiningCoinbaseTransaction(co.usc.ulordj.core.NetworkParameters params, MinerWork work) {
        return getUlordMergedMiningCoinbaseTransaction(params, TypeConverter.stringHexToByteArray(work.getBlockHashForMergedMining()));
    }

    public static co.usc.ulordj.core.UldTransaction getUlordMergedMiningCoinbaseTransaction(co.usc.ulordj.core.NetworkParameters params, byte[] blockHashForMergedMining) {
        co.usc.ulordj.core.UldTransaction coinbaseTransaction = new co.usc.ulordj.core.UldTransaction(params);
        //Add a random number of random bytes before the USC tag
        SecureRandom random = new SecureRandom();
        byte[] prefix = new byte[random.nextInt(1000)];
        random.nextBytes(prefix);
        byte[] bytes = Arrays.concatenate(prefix, UscMiningConstants.USC_TAG, blockHashForMergedMining);
        // Add the Tag to the scriptSig of first input
        co.usc.ulordj.core.TransactionInput ti = new co.usc.ulordj.core.TransactionInput(params, coinbaseTransaction, bytes);
        coinbaseTransaction.addInput(ti);
        ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
        co.usc.ulordj.core.UldECKey key = new co.usc.ulordj.core.UldECKey();
        try {
            co.usc.ulordj.script.Script.writeBytes(scriptPubKeyBytes, key.getPubKey());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        scriptPubKeyBytes.write(co.usc.ulordj.script.ScriptOpCodes.OP_CHECKSIG);
        coinbaseTransaction.addOutput(new co.usc.ulordj.core.TransactionOutput(params, coinbaseTransaction, co.usc.ulordj.core.Coin.valueOf(112, 96), scriptPubKeyBytes.toByteArray()));
        return coinbaseTransaction;
    }

    public static co.usc.ulordj.core.UldTransaction getUlordMergedMiningCoinbaseTransactionWithTwoTags(
            co.usc.ulordj.core.NetworkParameters params,
            MinerWork work,
            MinerWork work2) {
        return getUlordMergedMiningCoinbaseTransactionWithTwoTags(
                params,
                TypeConverter.stringHexToByteArray(work.getBlockHashForMergedMining()),
                TypeConverter.stringHexToByteArray(work2.getBlockHashForMergedMining()));
    }

    public static co.usc.ulordj.core.UldTransaction getUlordMergedMiningCoinbaseTransactionWithTwoTags(
            co.usc.ulordj.core.NetworkParameters params,
            byte[] blockHashForMergedMining1,
            byte[] blockHashForMergedMining2) {
        co.usc.ulordj.core.UldTransaction coinbaseTransaction = new co.usc.ulordj.core.UldTransaction(params);
        //Add a random number of random bytes before the USC tag
        SecureRandom random = new SecureRandom();
        byte[] prefix = new byte[random.nextInt(1000)];
        random.nextBytes(prefix);

        byte[] bytes0 = Arrays.concatenate(UscMiningConstants.USC_TAG, blockHashForMergedMining1);
        // addsecond tag
        byte[] bytes1 = Arrays.concatenate(bytes0, UscMiningConstants.USC_TAG, blockHashForMergedMining2);

        co.usc.ulordj.core.TransactionInput ti = new co.usc.ulordj.core.TransactionInput(params, coinbaseTransaction, prefix);
        coinbaseTransaction.addInput(ti);
        ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
        co.usc.ulordj.core.UldECKey key = new co.usc.ulordj.core.UldECKey();
        try {
            co.usc.ulordj.script.Script.writeBytes(scriptPubKeyBytes, key.getPubKey());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        scriptPubKeyBytes.write(co.usc.ulordj.script.ScriptOpCodes.OP_CHECKSIG);
        coinbaseTransaction.addOutput(new co.usc.ulordj.core.TransactionOutput(params, coinbaseTransaction, co.usc.ulordj.core.Coin.valueOf(50, 0), scriptPubKeyBytes.toByteArray()));
        // add opreturn output with two tags
        ByteArrayOutputStream output2Bytes = new ByteArrayOutputStream();
        output2Bytes.write(co.usc.ulordj.script.ScriptOpCodes.OP_RETURN);

        try {
            co.usc.ulordj.script.Script.writeBytes(output2Bytes, bytes1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        coinbaseTransaction.addOutput(
                new co.usc.ulordj.core.TransactionOutput(params, coinbaseTransaction, co.usc.ulordj.core.Coin.valueOf(1), output2Bytes.toByteArray()));

        return coinbaseTransaction;
    }

    public static co.usc.ulordj.core.UldBlock getUlordMergedMiningBlock(co.usc.ulordj.core.NetworkParameters params, UldTransaction transaction) {
        return getUlordMergedMiningBlock(params, Collections.singletonList(transaction));
    }

    public static co.usc.ulordj.core.UldBlock getUlordMergedMiningBlock(co.usc.ulordj.core.NetworkParameters params, List<UldTransaction> transactions) {
        co.usc.ulordj.core.Sha256Hash prevBlockHash = co.usc.ulordj.core.Sha256Hash.ZERO_HASH;
        long time = System.currentTimeMillis() / 1000;
        long difficultyTarget = co.usc.ulordj.core.Utils.encodeCompactBits(params.getMaxTarget());
        return new co.usc.ulordj.core.UldBlock(params, params.getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT), prevBlockHash, null, time, difficultyTarget, BigInteger.ZERO, transactions);
    }

    /**
     * Takes in a proofBuilderFunction (e.g. buildFromTxHashes)
     * and executes it on the builder corresponding to this block number.
     */
    public static byte[] buildMerkleProof(
            BlockchainNetConfig blockchainConfig,
            Function<MerkleProofBuilder, byte[]> proofBuilderFunction,
            long blockNumber) {
        if (blockchainConfig.getConfigForBlock(blockNumber).isUscIP92()) {
            return proofBuilderFunction.apply(new UscIP92MerkleProofBuilder());
        } else {
            return proofBuilderFunction.apply(new GenesisMerkleProofBuilder());
        }
    }

    public List<org.ethereum.core.Transaction> getAllTransactions(TransactionPool transactionPool) {

        List<Transaction> txs = transactionPool.getPendingTransactions();

        return PendingState.sortByPriceTakingIntoAccountSenderAndNonce(txs);
    }

    public List<org.ethereum.core.Transaction> filterTransactions(List<Transaction> txsToRemove, List<Transaction> txs, Map<UscAddress, BigInteger> accountNonces, Repository originalRepo, Coin minGasPrice) {
        List<org.ethereum.core.Transaction> txsResult = new ArrayList<>();
        for (org.ethereum.core.Transaction tx : txs) {
            try {
                Keccak256 hash = tx.getHash();
                Coin txValue = tx.getValue();
                BigInteger txNonce = new BigInteger(1, tx.getNonce());
                UscAddress txSender = tx.getSender();
                logger.debug("Examining tx={} sender: {} value: {} nonce: {}", hash, txSender, txValue, txNonce);

                BigInteger expectedNonce;

                if (accountNonces.containsKey(txSender)) {
                    expectedNonce = accountNonces.get(txSender).add(BigInteger.ONE);
                } else {
                    expectedNonce = originalRepo.getNonce(txSender);
                }

                if (!(tx instanceof RemascTransaction) && tx.getGasPrice().compareTo(minGasPrice) < 0) {
                    logger.warn("Rejected tx={} because of low gas account {}, removing tx from pending state.", hash, txSender);

                    txsToRemove.add(tx);
                    continue;
                }

                if (!expectedNonce.equals(txNonce)) {
                    logger.warn("Invalid nonce, expected {}, found {}, tx={}", expectedNonce, txNonce, hash);
                    continue;
                }

                accountNonces.put(txSender, txNonce);

                logger.debug("Accepted tx={} sender: {} value: {} nonce: {}", hash, txSender, txValue, txNonce);
            } catch (Exception e) {
                // Txs that can't be selected by any reason should be removed from pending state
                logger.warn(String.format("Error when processing tx=%s", tx.getHash()), e);
                if (txsToRemove != null) {
                    txsToRemove.add(tx);
                } else {
                    logger.error("Can't remove invalid txs from pending state.");
                }
                continue;
            }

            txsResult.add(tx);
        }

        logger.debug("Ending getTransactions {}", txsResult.size());

        return txsResult;
    }
}
