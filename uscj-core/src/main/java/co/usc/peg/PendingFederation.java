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

package co.usc.peg;

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.crypto.Keccak256;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.ByteArrayWrapper;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable representation of an USC Pending Federation.
 * A pending federation is one that is being actively
 * voted by the current federation to potentially become
 * the new active federation.
 *
 * @author Ariel Mendelzon
 */
public final class PendingFederation {
    private static final int MIN_FEDERATORS_REQUIRED = 2;

    private List<UldECKey> publicKeys;

    public PendingFederation(List<UldECKey> publicKeys) {
        // Sorting public keys ensures same order of federators for same public keys
        // Immutability provides protection unless unwanted modification, thus making the Pending Federation instance
        // effectively immutable
        this.publicKeys = Collections.unmodifiableList(publicKeys.stream().sorted(UldECKey.PUBKEY_COMPARATOR).collect(Collectors.toList()));
    }

    public List<UldECKey> getPublicKeys() {
        return publicKeys;
    }

    public boolean isComplete() {
        return this.publicKeys.size() >= MIN_FEDERATORS_REQUIRED;
    }

    /**
     * Creates a new PendingFederation with the additional specified public key
     * @param key the new public key
     * @return a new PendingFederation with the added public key
     */
    public PendingFederation addPublicKey(UldECKey key) {
        List<UldECKey> newKeys = new ArrayList<>(publicKeys);
        newKeys.add(key);
        return new PendingFederation(newKeys);
    }

    /**
     * Builds a Federation from this PendingFederation
     * @param creationTime the creation time for the new Federation
     * @param uldParams the ulord parameters for the new Federation
     * @return a Federation
     */
    public Federation buildFederation(Instant creationTime, long blockNumber, NetworkParameters uldParams) {
        if (!this.isComplete()) {
            throw new IllegalStateException("PendingFederation is incomplete");
        }

        return new Federation(
                publicKeys,
                creationTime,
                blockNumber,
                uldParams
        );
    }

    @Override
    public String toString() {
        return String.format("%d signatures pending federation (%s)", publicKeys.size(), isComplete() ? "complete" : "incomplete");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        PendingFederation otherFederation = (PendingFederation) other;
        ByteArrayWrapper[] thisPublicKeys = this.getPublicKeys().stream()
                .sorted(UldECKey.PUBKEY_COMPARATOR)
                .map(k -> new ByteArrayWrapper(k.getPubKey()))
                .toArray(ByteArrayWrapper[]::new);
        ByteArrayWrapper[] otherPublicKeys = otherFederation.getPublicKeys().stream()
                .sorted(UldECKey.PUBKEY_COMPARATOR)
                .map(k -> new ByteArrayWrapper(k.getPubKey()))
                .toArray(ByteArrayWrapper[]::new);

        return this.getPublicKeys().size() == otherFederation.getPublicKeys().size() &&
                Arrays.equals(thisPublicKeys, otherPublicKeys);
    }

    public Keccak256 getHash() {
        byte[] encoded = BridgeSerializationUtils.serializePendingFederation(this);
        return new Keccak256(HashUtil.keccak256(encoded));
    }

    @Override
    public int hashCode() {
        // Can use java.util.Objects.hash since List<UldECKey> has a
        // well-defined hashCode()
        return Objects.hash(getPublicKeys());
    }
}
