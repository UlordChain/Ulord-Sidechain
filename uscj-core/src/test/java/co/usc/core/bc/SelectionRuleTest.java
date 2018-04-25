package co.usc.core.bc;

import co.usc.blockchain.utils.BlockGenerator;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.util.UscTestFactory;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectionRuleTest {

    @Test
    public void smallerBlockHashTest() {
        byte[] lowerHash = new byte[]{0};
        byte[] biggerHash = new byte[]{1};

        assertTrue(SelectionRule.isThisBlockHashSmaller(lowerHash, biggerHash));
        assertFalse(SelectionRule.isThisBlockHashSmaller(biggerHash, lowerHash));
    }

    @Test
    public void addBlockTest() {
        Blockchain blockchain = createBlockchain();


        BlockGenerator blockGenerator = new BlockGenerator();
        Block lowDifficultyBlock = blockGenerator.createChildBlock(blockchain.getBestBlock(), 0, 1);
        Block highDifficultyBlock = blockGenerator.createChildBlock(lowDifficultyBlock, 0, 5);
        Block highDifficultyBlockWithMoreFees = blockGenerator.createChildBlock(lowDifficultyBlock, 10L, new ArrayList<>(), highDifficultyBlock.getDifficulty().getBytes());

        //diff test
        assertFalse(SelectionRule.shouldWeAddThisBlock(lowDifficultyBlock.getDifficulty(),
                highDifficultyBlock.getDifficulty(), lowDifficultyBlock, highDifficultyBlock));
        assertTrue(SelectionRule.shouldWeAddThisBlock(highDifficultyBlock.getDifficulty(),
                lowDifficultyBlock.getDifficulty(), highDifficultyBlock, lowDifficultyBlock));
        // At same difficulty, more fees
        assertTrue(SelectionRule.shouldWeAddThisBlock(highDifficultyBlockWithMoreFees.getDifficulty(),
                highDifficultyBlock.getDifficulty(), highDifficultyBlockWithMoreFees, highDifficultyBlock));
        //Low hash is proved in smallerBlockHashTest
    }

    private static BlockChainImpl createBlockchain() {
        UscTestFactory factory = new UscTestFactory();
        return factory.getBlockchain();
    }
}
