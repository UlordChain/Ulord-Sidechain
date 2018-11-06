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


import co.usc.blockchain.utils.BlockGenerator;
import co.usc.core.bc.BlockChainImpl;
import co.usc.crypto.Keccak256;
import co.usc.peg.PegTestUtils;
import org.ethereum.TestUtils;
import org.ethereum.core.*;
import org.bouncycastle.util.encoders.Hex;
import co.usc.remasc.RemascTransaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockTest {
    private static final byte[] EMPTY_LIST_HASH = HashUtil.keccak256(RLP.encodeList());

    @Test
    public void testParseRemascTransaction() {
        List<Transaction> txs = new ArrayList<>();

        Transaction txNotToRemasc = new Transaction(
                BigInteger.ZERO.toByteArray(),
                BigInteger.ONE.toByteArray(),
                BigInteger.valueOf(21000).toByteArray(),
                new ECKey().getAddress() ,
                BigInteger.valueOf(1000).toByteArray(),
                null);
        txNotToRemasc.sign(new ECKey().getPrivKeyBytes());
        txs.add(txNotToRemasc);

        Transaction txToRemascThatIsNotTheLatestTx = new Transaction(
                BigInteger.ZERO.toByteArray(),
                BigInteger.ONE.toByteArray(),
                BigInteger.valueOf(21000).toByteArray(),
                PrecompiledContracts.REMASC_ADDR.getBytes(),
                BigInteger.valueOf(1000).toByteArray(),
                null);
        txToRemascThatIsNotTheLatestTx.sign(new ECKey().getPrivKeyBytes());
        txs.add(txToRemascThatIsNotTheLatestTx);

        Transaction remascTx = new RemascTransaction(1);
        txs.add(remascTx);

        Block block =  new Block(
                PegTestUtils.createHash3().getBytes(),          // parent hash
                EMPTY_LIST_HASH,       // uncle hash
                TestUtils.randomAddress().getBytes(),            // coinbase
                new Bloom().getData(),          // logs bloom
                BigInteger.ONE.toByteArray(),    // difficulty
                1,
                BigInteger.valueOf(4000000).toByteArray(), // gasLimit
                3000000, // gasUsed
                100, //timestamp
                new byte[0],                    // extraData
                new byte[0],                    // mixHash
                new byte[]{0},         // provisory nonce
                HashUtil.EMPTY_TRIE_HASH,       // receipts root
                BlockChainImpl.calcTxTrie(txs), // transaction root
                HashUtil.EMPTY_TRIE_HASH,    //EMPTY_TRIE_HASH,   // state root
                txs,                            // transaction list
                null,  // uncle list
                BigInteger.TEN.toByteArray(),
                Coin.ZERO
        );

        Block parsedBlock = new Block(block.getEncoded());
        Assert.assertEquals(ImmutableTransaction.class, parsedBlock.getTransactionsList().get(0).getClass());
        Assert.assertEquals(ImmutableTransaction.class, parsedBlock.getTransactionsList().get(1).getClass());
        Assert.assertEquals(RemascTransaction.class, parsedBlock.getTransactionsList().get(2).getClass());
    }

    @Test
    public void sealedBlockHasSealesBlockHeader() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        Assert.assertTrue(block.getHeader().isSealed());
    }

    @Test
    public void sealedBlockAddUncle() {
        BlockGenerator blockGenerator = new BlockGenerator();
        Block block = blockGenerator.createBlock(10, 0);
        Block uncle = blockGenerator.createBlock(9, 0);

        block.seal();

        try {
            block.addUncle(uncle.getHeader());
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to add uncle", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockSetStateRoot() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.setStateRoot(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to alter state root", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockSetExtraData() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.setExtraData(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to alter extra data", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockSetTransactionList() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.setTransactionsList(Collections.emptyList());
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to alter transaction list", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockSetUlordMergedMiningCoinbaseTransaction() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.setUlordMergedMiningCoinbaseTransaction(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to alter ulord merged mining coinbase transaction", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockSetUlordMergedMiningHeader() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.setUlordMergedMiningHeader(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to alter ulord merged mining header", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockSetUlordMergedMiningMerkleProof() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.setUlordMergedMiningMerkleProof(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockException ex) {
            Assert.assertEquals("Sealed block: trying to alter ulord merged mining Merkle proof", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetStateRoot() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setStateRoot(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter state root", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetReceiptsRoot() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setReceiptsRoot(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter receipts root", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetTransactionsRoot() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setTransactionsRoot(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter transactions root", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetDifficulty() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setDifficulty(RLP.parseBlockDifficulty(new byte[32]));
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter difficulty", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetTimestamp() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setTimestamp(10);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter timestamp", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetNumber() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setNumber(10);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter number", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetGasLimit() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setGasLimit(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter gas limit", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetPaidFees() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setPaidFees(Coin.valueOf(10L));
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter paid fees", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetGasUsed() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setGasUsed(10);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter gas used", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetLogsBloom() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setLogsBloom(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter logs bloom", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetExtraData() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setExtraData(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter extra data", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetUlordMergedMiningHeader() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setUlordMergedMiningHeader(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter ulord merged mining header", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetUlordMergedMiningMerkleProof() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setUlordMergedMiningMerkleProof(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter ulord merged mining merkle proof", ex.getMessage());
        }
    }

    @Test
    public void sealedBlockHeaderSetUlordMergedMiningCoinbaseTransaction() {
        Block block = new BlockGenerator().createBlock(10, 0);

        block.seal();

        try {
            block.getHeader().setUlordMergedMiningCoinbaseTransaction(new byte[32]);
            Assert.fail();
        }
        catch (SealedBlockHeaderException ex) {
            Assert.assertEquals("Sealed block header: trying to alter ulord merged mining coinbase transaction", ex.getMessage());
        }
    }

    @Test
    public void checkTxTrieShouldBeDifferentForDifferentBlock() {
        BlockGenerator blockGenerator = new BlockGenerator();
        Block block1 = blockGenerator.createBlock(10, 1);
        Block block2 = blockGenerator.createBlock(10, 2);
        String trieHash1 = Hex.toHexString(block1.getTxTrieRoot());
        String trieHash2 = Hex.toHexString(block2.getTxTrieRoot());
        Assert.assertNotEquals(trieHash1, trieHash2);
    }

    @Test
    public void checkTxTrieShouldBeEqualForHeaderAndBody() {
        Block block = new BlockGenerator().createBlock(10, 5);
        Keccak256 trieHash = new Keccak256(block.getTxTrieRoot());
        Keccak256 trieListHash = Block.getTxTrie(block.getTransactionsList()).getHash();
        Assert.assertEquals(trieHash, trieListHash);
    }
}
