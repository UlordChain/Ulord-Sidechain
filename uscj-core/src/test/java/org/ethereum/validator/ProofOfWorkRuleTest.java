/*
 * This file is part of RskJ
 * Copyright (C) 2017 USC Labs Ltd.
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

package org.ethereum.validator;

import co.usc.blockchain.utils.BlockGenerator;
import co.usc.blockchain.utils.BlockMiner;
import co.usc.config.UscMiningConstants;
import co.usc.config.TestSystemProperties;
import co.usc.crypto.Keccak256;
import co.usc.mine.MinerUtils;
import co.usc.util.DifficultyUtils;
import co.usc.validators.ProofOfWorkRule;
import org.ethereum.core.Block;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import static co.usc.mine.MinerServerImpl.getBitcoinMergedMerkleBranch;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
public class ProofOfWorkRuleTest {

    private ProofOfWorkRule rule = new ProofOfWorkRule(new TestSystemProperties()).setFallbackMiningEnabled(false);

    @Test
    public void test_1() {
        // mined block
        Block b = BlockMiner.mineBlock(new BlockGenerator().getBlock(1));
        assertTrue(rule.isValid(b));
    }

    @Ignore
    @Test // invalid block
    public void test_2() {
        // mined block
        Block b = BlockMiner.mineBlock(new BlockGenerator().getBlock(1));
        byte[] mergeMiningHeader = b.getBitcoinMergedMiningHeader();
        // TODO improve, the mutated block header could be still valid
        mergeMiningHeader[0]++;
        b.setBitcoinMergedMiningHeader(mergeMiningHeader);
        assertFalse(rule.isValid(b));
    }

    // This test must be moved to the appropiate place
    @Test
    public void test_RLPEncoding() {
        // mined block
        Block b = BlockMiner.mineBlock(new BlockGenerator().getBlock(1));
        byte[] lastField = b.getBitcoinMergedMiningCoinbaseTransaction(); // last field
        b.flushRLP();// force re-encode
        byte[] encoded = b.getEncoded();
        Block b2 = new Block(encoded);
        byte[] lastField2 = b2.getBitcoinMergedMiningCoinbaseTransaction(); // last field
        b2.flushRLP();// force re-encode
        byte[] encoded2 = b2.getEncoded();
        Assert.assertTrue(Arrays.equals(encoded,encoded2));
        Assert.assertTrue(Arrays.equals(lastField,lastField2));
    }

    @Ignore
    @Test // stress test
    public void test_3() {
        int iterCnt = 1_000_000;

        // mined block
        Block b = BlockMiner.mineBlock(new BlockGenerator().getBlock(1));

        long start = System.currentTimeMillis();
        for (int i = 0; i < iterCnt; i++)
            rule.isValid(b);

        long total = System.currentTimeMillis() - start;

        System.out.println(String.format("Time: total = %d ms, per block = %.2f ms", total, (double) total / iterCnt));
    }

    @Test
    public void test_noRSKTagInCoinbaseTransaction() {
        BlockGenerator blockGenerator = new BlockGenerator();

        // mined block
        Block b = mineBlockWithCoinbaseTransactionWithCompressedCoinbaseTransactionPrefix(blockGenerator.getBlock(1), new byte[100]);

        Assert.assertFalse(rule.isValid(b));
    }

    @Test
    public void test_RSKTagInCoinbaseTransactionTooFar() {
        /* This test is about a rsk block, with a compressed coinbase that leaves more than 64 bytes before the start of the USC tag. */
        BlockGenerator blockGenerator = new BlockGenerator();
        byte[] prefix = new byte[1000];
        byte[] bytes = org.spongycastle.util.Arrays.concatenate(prefix, UscMiningConstants.USC_TAG);

        // mined block
        Block b = mineBlockWithCoinbaseTransactionWithCompressedCoinbaseTransactionPrefix(blockGenerator.getBlock(1), bytes);

        Assert.assertFalse(rule.isValid(b));
    }

    private static Block mineBlockWithCoinbaseTransactionWithCompressedCoinbaseTransactionPrefix(Block block, byte[] compressed) {
        Keccak256 blockMergedMiningHash = new Keccak256(block.getHashForMergedMining());

        co.usc.ulordj.core.NetworkParameters bitcoinNetworkParameters = co.usc.ulordj.params.RegTestParams.get();
        co.usc.ulordj.core.UldTransaction bitcoinMergedMiningCoinbaseTransaction = MinerUtils.getBitcoinMergedMiningCoinbaseTransaction(bitcoinNetworkParameters, blockMergedMiningHash.getBytes());
        co.usc.ulordj.core.UldBlock bitcoinMergedMiningBlock = MinerUtils.getBitcoinMergedMiningBlock(bitcoinNetworkParameters, bitcoinMergedMiningCoinbaseTransaction);

        BigInteger targetBI = DifficultyUtils.difficultyToTarget(block.getDifficulty());

        BlockMiner.findNonce(bitcoinMergedMiningBlock, targetBI);

        // We need to clone to allow modifications
        Block newBlock = new Block(block.getEncoded()).cloneBlock();

        newBlock.setBitcoinMergedMiningHeader(bitcoinMergedMiningBlock.cloneAsHeader().bitcoinSerialize());

        co.usc.ulordj.core.PartialMerkleTree bitcoinMergedMiningMerkleBranch = getBitcoinMergedMerkleBranch(bitcoinMergedMiningBlock);

        newBlock.setBitcoinMergedMiningCoinbaseTransaction(org.spongycastle.util.Arrays.concatenate(compressed, blockMergedMiningHash.getBytes()));
        newBlock.setBitcoinMergedMiningMerkleProof(bitcoinMergedMiningMerkleBranch.bitcoinSerialize());

        return newBlock;
    }

    private static co.usc.ulordj.core.UldTransaction getBitcoinMergedMiningCoinbaseTransactionWithoutRSKTag(co.usc.ulordj.core.NetworkParameters params, byte[] blockHashForMergedMining) {
        co.usc.ulordj.core.UldTransaction coinbaseTransaction = new co.usc.ulordj.core.UldTransaction(params);
        //Add a random number of random bytes before the USC tag
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[1000];
        random.nextBytes(bytes);
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
        coinbaseTransaction.addOutput(new co.usc.ulordj.core.TransactionOutput(params, coinbaseTransaction, co.usc.ulordj.core.Coin.valueOf(50, 0), scriptPubKeyBytes.toByteArray()));
        return coinbaseTransaction;
    }
}
