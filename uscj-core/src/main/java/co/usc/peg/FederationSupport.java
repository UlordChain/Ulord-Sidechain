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
import co.usc.ulordj.core.UTXO;
import co.usc.config.BridgeConstants;
import org.ethereum.core.Block;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class FederationSupport {

    private enum StorageFederationReference { NONE, NEW, OLD, GENESIS }

    private final BridgeStorageProvider provider;
    private final BridgeConstants bridgeConstants;
    private final Block executionBlock;

    public FederationSupport(BridgeStorageProvider provider, BridgeConstants bridgeConstants, Block executionBlock) {
        this.provider = provider;
        this.bridgeConstants = bridgeConstants;
        this.executionBlock = executionBlock;
    }

    /**
     * Returns the federation's size
     * @return the federation size
     */
    public int getFederationSize() {
        return getActiveFederation().getPublicKeys().size();
    }

    /**
     * Returns the public key of the federation's federator at the given index
     * @param index the federator's index (zero-based)
     * @return the federator's public key
     */
    public byte[] getFederatorPublicKey(int index) {
        List<UldECKey> publicKeys = getActiveFederation().getPublicKeys();

        if (index < 0 || index >= publicKeys.size()) {
            throw new IndexOutOfBoundsException(String.format("Federator index must be between 0 and %d", publicKeys.size() - 1));
        }

        return publicKeys.get(index).getPubKey();
    }

    /**
     * Returns the currently active federation.
     * See getActiveFederationReference() for details.
     *
     * @return the currently active federation.
     */
    public Federation getActiveFederation() {
        switch (getActiveFederationReference()) {
            case NEW:
                return provider.getNewFederation();
            case OLD:
                return provider.getOldFederation();
            case GENESIS:
            default:
                return bridgeConstants.getGenesisFederation();
        }
    }

    /**
     * Returns the currently retiring federation.
     * See getRetiringFederationReference() for details.
     *
     * @return the retiring federation.
     */
    @Nullable
    public Federation getRetiringFederation() {
        switch (getRetiringFederationReference()) {
            case OLD:
                return provider.getOldFederation();
            case NONE:
            default:
                return null;
        }
    }

    public List<UTXO> getActiveFederationUldUTXOs() throws IOException {
        switch (getActiveFederationReference()) {
            case OLD:
                return provider.getOldFederationUldUTXOs();
            case NEW:
            case GENESIS:
            default:
                return provider.getNewFederationUldUTXOs();
        }
    }

    public List<UTXO> getRetiringFederationUldUTXOs() throws IOException {
        switch (getRetiringFederationReference()) {
            case OLD:
                return provider.getOldFederationUldUTXOs();
            case NONE:
            default:
                return Collections.emptyList();
        }
    }

    public boolean amAwaitingFederationActivation() {
        Federation newFederation = provider.getNewFederation();
        Federation oldFederation = provider.getOldFederation();

        return newFederation != null && oldFederation != null && !shouldFederationBeActive(newFederation);
    }

    /**
     * Returns the currently active federation reference.
     * Logic is as follows:
     * When no "new" federation is recorded in the blockchain, then return GENESIS
     * When a "new" federation is present and no "old" federation is present, then return NEW
     * When both "new" and "old" federations are present, then
     * 1) If the "new" federation is at least bridgeConstants::getFederationActivationAge() blocks old,
     * return the NEW
     * 2) Otherwise, return OLD
     *
     * @return a reference to where the currently active federation is stored.
     */
    private StorageFederationReference getActiveFederationReference() {
        Federation newFederation = provider.getNewFederation();

        // No new federation in place, then the active federation
        // is the genesis federation
        if (newFederation == null) {
            return StorageFederationReference.GENESIS;
        }

        Federation oldFederation = provider.getOldFederation();

        // No old federation in place, then the active federation
        // is the new federation
        if (oldFederation == null) {
            return StorageFederationReference.NEW;
        }

        // Both new and old federations in place
        // If the minimum age has gone by for the new federation's
        // activation, then that federation is the currently active.
        // Otherwise, the old federation is still the currently active.
        if (shouldFederationBeActive(newFederation)) {
            return StorageFederationReference.NEW;
        }

        return StorageFederationReference.OLD;
    }

    /**
     * Returns the currently retiring federation reference.
     * Logic is as follows:
     * When no "new" or "old" federation is recorded in the blockchain, then return empty.
     * When both "new" and "old" federations are present, then
     * 1) If the "new" federation is at least bridgeConstants::getFederationActivationAge() blocks old,
     * return OLD
     * 2) Otherwise, return empty
     *
     * @return the retiring federation.
     */
    private StorageFederationReference getRetiringFederationReference() {
        Federation newFederation = provider.getNewFederation();
        Federation oldFederation = provider.getOldFederation();

        if (oldFederation == null || newFederation == null) {
            return StorageFederationReference.NONE;
        }

        // Both new and old federations in place
        // If the minimum age has gone by for the new federation's
        // activation, then the old federation is the currently retiring.
        // Otherwise, there is no retiring federation.
        if (shouldFederationBeActive(newFederation)) {
            return StorageFederationReference.OLD;
        }

        return StorageFederationReference.NONE;
    }

    private boolean shouldFederationBeActive(Federation federation) {
        long federationAge = executionBlock.getNumber() - federation.getCreationBlockNumber();
        return federationAge >= bridgeConstants.getFederationActivationAge();
    }
}
