package co.usc.blockchain.utils;

import co.usc.crypto.Keccak256;
import co.usc.mine.MinerUtils;
import co.usc.util.DifficultyUtils;
import org.ethereum.core.Block;

import javax.annotation.Nonnull;
import java.math.BigInteger;

import static co.usc.mine.MinerServerImpl.compressCoinbase;
import static co.usc.mine.MinerServerImpl.getBitcoinMergedMerkleBranch;

/**
 * Created by ajlopez on 13/09/2017.
 */
public class BlockMiner {
    private static BigInteger nextNonceToUse = BigInteger.ZERO;

    public static Block mineBlock(Block block) {
        Keccak256 blockMergedMiningHash = new Keccak256(block.getHashForMergedMining());

        co.usc.ulordj.core.NetworkParameters bitcoinNetworkParameters = co.usc.ulordj.params.RegTestParams.get();
        co.usc.ulordj.core.UldTransaction bitcoinMergedMiningCoinbaseTransaction = MinerUtils.getBitcoinMergedMiningCoinbaseTransaction(bitcoinNetworkParameters, blockMergedMiningHash.getBytes());
        co.usc.ulordj.core.UldBlock bitcoinMergedMiningBlock = MinerUtils.getUlordMergedMiningBlock(bitcoinNetworkParameters, bitcoinMergedMiningCoinbaseTransaction);

        BigInteger targetBI = DifficultyUtils.difficultyToTarget(block.getDifficulty());

        findNonce(bitcoinMergedMiningBlock, targetBI);

        // We need to clone to allow modifications
        Block newBlock = new Block(block.getEncoded()).cloneBlock();

        newBlock.setBitcoinMergedMiningHeader(bitcoinMergedMiningBlock.cloneAsHeader().bitcoinSerialize());

        bitcoinMergedMiningCoinbaseTransaction = bitcoinMergedMiningBlock.getTransactions().get(0);
        co.usc.ulordj.core.PartialMerkleTree bitcoinMergedMiningMerkleBranch = getBitcoinMergedMerkleBranch(bitcoinMergedMiningBlock);

        newBlock.setBitcoinMergedMiningCoinbaseTransaction(compressCoinbase(bitcoinMergedMiningCoinbaseTransaction.bitcoinSerialize()));
        newBlock.setBitcoinMergedMiningMerkleProof(bitcoinMergedMiningMerkleBranch.bitcoinSerialize());

        return newBlock;
    }

    /**
     * findNonce will try to find a valid nonce for bitcoinMergedMiningBlock, that satisfies the given target difficulty.
     *
     * @param bitcoinMergedMiningBlock bitcoinBlock to find nonce for. This block's nonce will be modified.
     * @param target                   target difficulty. Block's hash should be lower than this number.
     */
    public static void findNonce(@Nonnull final co.usc.ulordj.core.UldBlock bitcoinMergedMiningBlock,
                              @Nonnull final BigInteger target) {
        bitcoinMergedMiningBlock.setNonce(nextNonceToUse.add(BigInteger.ONE));

        while (true) {
            // Is our proof of work valid yet?
            BigInteger blockHashBI = bitcoinMergedMiningBlock.getHash().toBigInteger();

            if (blockHashBI.compareTo(target) <= 0)
                return;

            // No, so increment the nonce and try again.
            bitcoinMergedMiningBlock.setNonce(nextNonceToUse.add(BigInteger.ONE));
        }
   }
}
