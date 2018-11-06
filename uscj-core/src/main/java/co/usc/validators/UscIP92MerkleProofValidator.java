/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
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
package co.usc.validators;

import co.usc.ulordj.core.PartialMerkleTree;
import co.usc.ulordj.core.Sha256Hash;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Validates USCIP 92 Merkle proofs
 */
public class UscIP92MerkleProofValidator implements MerkleProofValidator {

    private final byte[] pmtSerialized;

    public UscIP92MerkleProofValidator(byte[] pmtSerialized) {
        if ((pmtSerialized.length % Sha256Hash.LENGTH) != 0) {
            throw new IllegalArgumentException("Partial merkle tree does not have the expected format");
        }

        this.pmtSerialized = pmtSerialized;
    }

    @Override
    public boolean isValid(Sha256Hash expectedRoot, Sha256Hash coinbaseHash) {
        Sha256Hash root = streamHashes().reduce(coinbaseHash, UscIP92MerkleProofValidator::combineLeftRight);
        return root.equals(expectedRoot);
    }

    private Stream<Sha256Hash> streamHashes() {
        return IntStream.range(0, pmtSerialized.length / Sha256Hash.LENGTH)
                .mapToObj(this::getHash);
    }

    private Sha256Hash getHash(int index) {
        int start = index * Sha256Hash.LENGTH;
        int end = (index + 1) * Sha256Hash.LENGTH;
        byte[] hash = Arrays.copyOfRange(pmtSerialized, start, end);
        return Sha256Hash.wrap(hash);
    }

    /**
     * See {@link PartialMerkleTree#combineLeftRight(byte[], byte[])}
     */
    private static Sha256Hash combineLeftRight(Sha256Hash left, Sha256Hash right) {
        return Sha256Hash.wrapReversed(
                Sha256Hash.hashTwice(
                        left.getReversedBytes(), 0, 32,
                        right.getReversedBytes(), 0, 32
                )
        );
    }
}
