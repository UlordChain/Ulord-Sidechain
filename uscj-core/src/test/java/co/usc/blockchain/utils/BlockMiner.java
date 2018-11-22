package co.usc.blockchain.utils;

import co.usc.config.TestSystemProperties;
import co.usc.crypto.Keccak256;
import co.usc.mine.MinerUtils;
import co.usc.util.DifficultyUtils;
import org.ethereum.core.Block;

import java.math.BigInteger;

import static co.usc.mine.MinerServerImpl.compressCoinbase;

/**
 * Created by ajlopez on 13/09/2017.
 */
public class BlockMiner {
    private static BigInteger nextNonceToUse = BigInteger.ZERO;

    private final TestSystemProperties config;

    public BlockMiner(TestSystemProperties config) {
        this.config = config;
    }

    public Block mineBlock(Block block) {
        Keccak256 blockMergedMiningHash = new Keccak256(block.getHashForMergedMining());

        co.usc.ulordj.core.NetworkParameters ulordNetworkParameters = co.usc.ulordj.params.RegTestParams.get();
        co.usc.ulordj.core.UldTransaction ulordMergedMiningCoinbaseTransaction = MinerUtils.getUlordMergedMiningCoinbaseTransaction(ulordNetworkParameters, blockMergedMiningHash.getBytes());
        co.usc.ulordj.core.UldBlock ulordMergedMiningBlock = MinerUtils.getUlordMergedMiningBlock(ulordNetworkParameters, ulordMergedMiningCoinbaseTransaction);

        BigInteger targetBI = DifficultyUtils.difficultyToTarget(block.getDifficulty());

        findNonce(ulordMergedMiningBlock, targetBI);

        // We need to clone to allow modifications
        Block newBlock = new Block(block.getEncoded()).cloneBlock();

        newBlock.setUlordMergedMiningHeader(ulordMergedMiningBlock.cloneAsHeader().ulordSerialize());

        ulordMergedMiningCoinbaseTransaction = ulordMergedMiningBlock.getTransactions().get(0);
        byte[] merkleProof = MinerUtils.buildMerkleProof(
                config.getBlockchainConfig(),
                pb -> pb.buildFromBlock(ulordMergedMiningBlock),
                newBlock.getNumber()
        );

        newBlock.setUlordMergedMiningCoinbaseTransaction(compressCoinbase(ulordMergedMiningCoinbaseTransaction.ulordSerialize()));
        newBlock.setUlordMergedMiningMerkleProof(merkleProof);

        return newBlock;
    }

    /**
     * findNonce will try to find a valid nonce for ulordMergedMiningBlock, that satisfies the given target difficulty.
     *
     * @param ulordMergedMiningBlock ulordBlock to find nonce for. This block's nonce will be modified.
     * @param target                   target difficulty. Block's hash should be lower than this number.
     */
    public void findNonce(co.usc.ulordj.core.UldBlock ulordMergedMiningBlock, BigInteger target) {
        ulordMergedMiningBlock.setNonce(nextNonceToUse);
        nextNonceToUse = nextNonceToUse.add(BigInteger.ONE);
        while (true) {
            // Is our proof of work valid yet?
            BigInteger blockHashBI = ulordMergedMiningBlock.getHash().toBigInteger();

            if (blockHashBI.compareTo(target) <= 0)
                return;

            // No, so increment the nonce and try again.
            ulordMergedMiningBlock.setNonce(nextNonceToUse);
            nextNonceToUse = nextNonceToUse.add(BigInteger.ONE);
        }
   }
}
