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

import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.Sha256Hash;
import co.usc.peg.utils.PartialMerkleTreeFormatUtils;
import co.usc.ulordj.core.Utils;
import org.bouncycastle.util.encoders.Hex;
import org.ethereum.util.ByteUtil;

import java.util.List;
import java.util.stream.Stream;

/**
 * Builds USCIP 92 Merkle proofs
 */
public class UscIP92MerkleProofBuilder implements MerkleProofBuilder {

    @Override
    public byte[] buildFromMerkleHashes(
            UldBlock blockWithHeaderOnly,
            List<String> merkleHashesString,
            int blockTxnCount) {
        Stream<byte[]> hashesStream = merkleHashesString.stream()
                .map(mh -> Utils.reverseBytes(Hex.decode(mh)));
        return mergeHashes(hashesStream);
    }

    @Override
    public byte[] buildFromTxHashes(UldBlock blockWithHeaderOnly, List<String> txHashesString) {
        return buildFromFullPmt(
                new GenesisMerkleProofBuilder().buildFromTxHashes(blockWithHeaderOnly, txHashesString)
        );
    }

    @Override
    public byte[] buildFromBlock(UldBlock ulordMergedMiningBlock) {
        return buildFromFullPmt(
                new GenesisMerkleProofBuilder().buildFromBlock(ulordMergedMiningBlock)
        );
    }

    /**
     * This takes a full PMT and slices and rearranges the needed pieces for the new serialization format.
     * It would make sense to re-implement this as a standalone algorithm, but reusing the PMT is enough for now.
     */
    private byte[] buildFromFullPmt(byte[] pmtSerialized) {
        Stream<byte[]> hashesStream = PartialMerkleTreeFormatUtils.streamIntermediateHashes(pmtSerialized)
                .map(Sha256Hash::getBytes);
        return mergeHashes(hashesStream);
    }

    private byte[] mergeHashes(Stream<byte[]> hashesStream) {
        byte[][] hashes = hashesStream
                .skip(1) // skip the coinbase
                .toArray(byte[][]::new);
        return ByteUtil.merge(hashes);
    }
}