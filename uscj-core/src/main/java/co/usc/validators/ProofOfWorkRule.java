/*
 * Copyright (C) 2017 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
 * Copyright (C) 2016-2018  Ulord developers
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

package co.usc.validators;

import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.PartialMerkleTree;
import co.usc.ulordj.core.Sha256Hash;
import co.usc.config.BridgeConstants;
import co.usc.config.UscMiningConstants;
import co.usc.config.UscSystemProperties;
import co.usc.util.DifficultyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.ethereum.config.Constants;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Checks proof value against its boundary for the block header.
 */
@Component
public class ProofOfWorkRule implements BlockHeaderValidationRule, BlockValidationRule {

    private static final Logger logger = LoggerFactory.getLogger("blockvalidator");

    private final BridgeConstants bridgeConstants;
    private final Constants constants;
    private boolean fallbackMiningEnabled = true;

    @Autowired
    public ProofOfWorkRule(UscSystemProperties config) {
        this.bridgeConstants = config.getBlockchainConfig().getCommonConstants().getBridgeConstants();
        this.constants = config.getBlockchainConfig().getCommonConstants();
    }

    public ProofOfWorkRule setFallbackMiningEnabled(boolean e) {
        fallbackMiningEnabled = e;
        return this;
    }

    @Override
    public boolean isValid(Block block) {
        return isValid(block.getHeader());
    }

    public static boolean isFallbackMiningPossible(Constants constants, BlockHeader header) {

        if (header.getNumber() >= constants.getEndOfFallbackMiningBlockNumber()) {
            return false;
        }

        if (header.getDifficulty().compareTo(constants.getFallbackMiningDifficulty()) > 0) {
            return false;
        }

        // If more than 10 minutes have elapsed, and difficulty is lower than 4 peta/s (config)
        // then private mining is still possible, but only after 10 minutes of inactivity or
        // previous block was privately mined.
        // This difficulty reset will be computed in DifficultyRule
        return true;
    }

    public boolean isFallbackMiningPossibleAndBlockSigned(BlockHeader header) {

        if (header.getUlordMergedMiningCoinbaseTransaction() != null) {
            return false;
        }

        if (header.getUlordMergedMiningMerkleProof() != null) {
            return false;
        }

        if (!fallbackMiningEnabled) {
            return false;
        }

        return isFallbackMiningPossible(constants, header);

    }

    @Override
    public boolean isValid(BlockHeader header) {
        // TODO: refactor this an move it to another class. Change the Global ProofOfWorkRule to AuthenticationRule.
        // TODO: Make ProofOfWorkRule one of the classes that inherits from AuthenticationRule.

        if (isFallbackMiningPossibleAndBlockSigned(header)) {
            boolean isValidFallbackSignature = validFallbackBlockSignature(constants, header, header.getUlordMergedMiningHeader());
            if (!isValidFallbackSignature) {
                logger.warn("Fallback signature failed. Header {}", header.getShortHash());
            }
            return isValidFallbackSignature;
        }

        co.usc.ulordj.core.NetworkParameters ulordNetworkParameters = bridgeConstants.getUldParams();
        byte[] ulordMergedMiningCoinbaseTransactionCompressed = header.getUlordMergedMiningCoinbaseTransaction();

        if (ulordMergedMiningCoinbaseTransactionCompressed==null) {
			logger.warn("Compressed coinbase transaction does not exist. Header {}", header.getShortHash());
            return false;
        }

        if (header.getUlordMergedMiningHeader()==null) {
			logger.warn("Ulord merged mining header does not exist. Header {}", header.getShortHash());
            return false;
        }

        if (header.getUlordMergedMiningMerkleProof()==null) {
			logger.warn("Partial merkle tree does not have the expected size. Header {}", header.getShortHash());
            return false;
        }

        UldBlock ulordMergedMiningBlock = ulordNetworkParameters.getDefaultSerializer().makeBlock(header.getUlordMergedMiningHeader());
        PartialMerkleTree UlordMergedMiningMerkleBranch  = new PartialMerkleTree(ulordNetworkParameters, header.getUlordMergedMiningMerkleProof(), 0);

        BigInteger target = DifficultyUtils.difficultyToTarget(header.getDifficulty());
        BigInteger ulordMergedMiningBlockHashBI = ulordMergedMiningBlock.getHash().toBigInteger();

        //logger.info("ulordMergedMiningBlockHashBI: " + ulordMergedMiningBlockHashBI.toString(16));
        //logger.info("PoW Target:                   " + target.toString(16));
        if (ulordMergedMiningBlockHashBI.compareTo(target) > 0) {
            logger.warn("Hash {} is higher than target {}", ulordMergedMiningBlockHashBI.toString(16), target.toString(16));
            return false;
        }

        byte[] ulordMergedMiningCoinbaseTransactionMidstate = new byte[UscMiningConstants.MIDSTATE_SIZE];
        System.arraycopy(ulordMergedMiningCoinbaseTransactionCompressed, 0, ulordMergedMiningCoinbaseTransactionMidstate, 8, UscMiningConstants.MIDSTATE_SIZE_TRIMMED);

        byte[] ulordMergedMiningCoinbaseTransactionTail = new byte[ulordMergedMiningCoinbaseTransactionCompressed.length - UscMiningConstants.MIDSTATE_SIZE_TRIMMED];
        System.arraycopy(ulordMergedMiningCoinbaseTransactionCompressed, UscMiningConstants.MIDSTATE_SIZE_TRIMMED,
                ulordMergedMiningCoinbaseTransactionTail, 0, ulordMergedMiningCoinbaseTransactionTail.length);

        byte[] expectedCoinbaseMessageBytes = org.spongycastle.util.Arrays.concatenate(UscMiningConstants.USC_TAG, header.getHashForMergedMining());


        List<Byte> ulordMergedMiningCoinbaseTransactionTailAsList = Arrays.asList(ArrayUtils.toObject(ulordMergedMiningCoinbaseTransactionTail));
        List<Byte> expectedCoinbaseMessageBytesAsList = Arrays.asList(ArrayUtils.toObject(expectedCoinbaseMessageBytes));

        int uscTagPosition = Collections.lastIndexOfSubList(ulordMergedMiningCoinbaseTransactionTailAsList, expectedCoinbaseMessageBytesAsList);
        if (uscTagPosition == -1) {
            logger.warn("ulord coinbase transaction tail message does not contain expected USCBLOCK:UscBlockHeaderHash. Expected: {} . Actual: {} .", Arrays.toString(expectedCoinbaseMessageBytes), Arrays.toString(ulordMergedMiningCoinbaseTransactionTail));
            return false;
        }

        /*
        * We check that the there is no other block before the usc tag, to avoid a possible malleability attack:
        * If we have a mid state with 10 blocks, and the usc tag, we can also have
        * another mid state with 9 blocks, 64bytes + the usc tag, giving us two blocks with different hashes but the same spv proof.
        * */
        if (uscTagPosition >= 64) {
            logger.warn("ulord coinbase transaction tag position is bigger than expected 64. Actual: {}.", Integer.toString(uscTagPosition));
            return false;
        }

        List<Byte> uscTagAsList = Arrays.asList(ArrayUtils.toObject(UscMiningConstants.USC_TAG));
        int lastTag = Collections.lastIndexOfSubList(ulordMergedMiningCoinbaseTransactionTailAsList, uscTagAsList);
        if (uscTagPosition !=lastTag) {
            logger.warn("The valid USC tag is not the last USC tag. Tail: {}.", Arrays.toString(ulordMergedMiningCoinbaseTransactionTail));
            return false;
        }

        int remainingByteCount = ulordMergedMiningCoinbaseTransactionTail.length -
                uscTagPosition -
                UscMiningConstants.USC_TAG.length -
                UscMiningConstants.BLOCK_HEADER_HASH_SIZE;

        if (remainingByteCount > UscMiningConstants.MAX_BYTES_AFTER_MERGED_MINING_HASH) {
            logger.warn("More than " + Integer.toString(UscMiningConstants.MAX_BYTES_AFTER_MERGED_MINING_HASH) + "bytes after USC tag");
            return false;
        }

        SHA256Digest digest = new SHA256Digest(ulordMergedMiningCoinbaseTransactionMidstate);
        digest.update(ulordMergedMiningCoinbaseTransactionTail,0,ulordMergedMiningCoinbaseTransactionTail.length);
        byte[] ulordMergedMiningCoinbaseTransactionOneRoundOfHash = new byte[32];
        digest.doFinal(ulordMergedMiningCoinbaseTransactionOneRoundOfHash, 0);
        Sha256Hash ulordMergedMiningCoinbaseTransactionHash = Sha256Hash.wrapReversed(Sha256Hash.hash(ulordMergedMiningCoinbaseTransactionOneRoundOfHash));

        List<Sha256Hash> txHashesInTheMerkleBranch = new ArrayList<>();
        Sha256Hash merkleRoot = UlordMergedMiningMerkleBranch.getTxnHashAndMerkleRoot(txHashesInTheMerkleBranch);
        if (!merkleRoot.equals(ulordMergedMiningBlock.getMerkleRoot())) {
            logger.warn("ulord merkle root of ulord block does not match the merkle root of merkle branch");
            return false;
        }
        if (!txHashesInTheMerkleBranch.contains(ulordMergedMiningCoinbaseTransactionHash)) {
            logger.warn("ulord coinbase transaction {} not included in merkle branch", ulordMergedMiningCoinbaseTransactionHash);
            return false;
        }

        return true;
    }

    public static boolean validFallbackBlockSignature(Constants constants, BlockHeader header, byte[] signatureBytesRLP) {

        if (header.getUlordMergedMiningCoinbaseTransaction() != null) {
            return false;
        }

        if (header.getUlordMergedMiningMerkleProof() != null) {
            return false;
        }

        byte[] fallbackMiningPubKeyBytes;
        boolean isEvenBlockNumber = (header.getNumber() % 2 == 0);
        if (isEvenBlockNumber) {
            fallbackMiningPubKeyBytes = constants.getFallbackMiningPubKey0();
        } else {
            fallbackMiningPubKeyBytes = constants.getFallbackMiningPubKey1();
        }

        ECKey fallbackMiningPubKey = ECKey.fromPublicOnly(fallbackMiningPubKeyBytes);
        List<RLPElement> signatureRLP = (RLPList) RLP.decode2(signatureBytesRLP).get(0);
        if (signatureRLP.size() != 3) {
            return false;
        }

        byte[] v = signatureRLP.get(0).getRLPData();
        byte[] r = signatureRLP.get(1).getRLPData();
        byte[] s = signatureRLP.get(2).getRLPData();

        ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v[0]);

        return fallbackMiningPubKey.verify(header.getHashForMergedMining(), signature);
    }
}
