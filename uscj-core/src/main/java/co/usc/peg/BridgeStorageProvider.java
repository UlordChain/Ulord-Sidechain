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

import co.usc.ulordj.core.*;
import co.usc.config.BridgeConstants;
import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import co.usc.peg.whitelist.LockWhitelist;
import co.usc.peg.whitelist.LockWhitelistEntry;
import co.usc.peg.whitelist.OneOffWhiteListEntry;
import co.usc.peg.whitelist.UnlimitedWhiteListEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.core.Repository;
import org.ethereum.vm.DataWord;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Provides an object oriented facade of the bridge contract memory.
 * @see co.usc.remasc.RemascStorageProvider
 * @author ajlopez
 * @author Oscar Guindzberg
 */
public class BridgeStorageProvider {
    private static final DataWord NEW_FEDERATION_ULD_UTXOS_KEY = DataWord.fromString("newFederationUldUTXOs");
    private static final DataWord OLD_FEDERATION_ULD_UTXOS_KEY = DataWord.fromString("oldFederationUldUTXOs");
    private static final DataWord ULD_TX_HASHES_ALREADY_PROCESSED_KEY = DataWord.fromString("uldTxHashesAP");
    private static final DataWord RELEASE_REQUEST_QUEUE = DataWord.fromString("releaseRequestQueue");
    private static final DataWord RELEASE_TX_SET = DataWord.fromString("releaseTransactionSet");
    private static final DataWord USC_TXS_WAITING_FOR_SIGNATURES_KEY = DataWord.fromString("uscTxsWaitingFS");
    private static final DataWord NEW_FEDERATION_KEY = DataWord.fromString("newFederation");
    private static final DataWord OLD_FEDERATION_KEY = DataWord.fromString("oldFederation");
    private static final DataWord PENDING_FEDERATION_KEY = DataWord.fromString("pendingFederation");
    private static final DataWord FEDERATION_ELECTION_KEY = DataWord.fromString("federationElection");
    private static final DataWord LOCK_ONE_OFF_WHITELIST_KEY = DataWord.fromString("lockWhitelist");
    private static final DataWord LOCK_UNLIMITED_WHITELIST_KEY = DataWord.fromString("unlimitedLockWhitelist");
    private static final DataWord FEE_PER_KB_KEY = DataWord.fromString("feePerKb");
    private static final DataWord FEE_PER_KB_ELECTION_KEY = DataWord.fromString("feePerKbElection");

    private final Repository repository;
    private final UscAddress contractAddress;
    private final NetworkParameters networkParameters;
    private final Context uldContext;
    private final BridgeStorageConfiguration bridgeStorageConfiguration;

    private Map<Sha256Hash, Long> uldTxHashesAlreadyProcessed;

    // USC release txs follow these steps: First, they are waiting for coin selection (releaseRequestQueue),
    // then they are waiting for enough confirmations on the USC network (releaseTransactionSet),
    // then they are waiting for federators' signatures (uscTxsWaitingForSignatures),
    // then they are logged into the block that has them as completely signed for uld release
    // and are removed from uscTxsWaitingForSignatures.
    // key = usc tx hash, value = uld tx
    private ReleaseRequestQueue releaseRequestQueue;
    private ReleaseTransactionSet releaseTransactionSet;
    private SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures;

    private List<UTXO> newFederationUldUTXOs;
    private List<UTXO> oldFederationUldUTXOs;

    private Federation newFederation;
    private Federation oldFederation;
    private boolean shouldSaveOldFederation = false;
    private PendingFederation pendingFederation;
    private boolean shouldSavePendingFederation = false;

    private ABICallElection federationElection;

    private LockWhitelist lockWhitelist;

    private Coin feePerKb;
    private ABICallElection feePerKbElection;

    public BridgeStorageProvider(Repository repository, UscAddress contractAddress, BridgeConstants bridgeConstants, BridgeStorageConfiguration bridgeStorageConfiguration) {
        this.repository = repository;
        this.contractAddress = contractAddress;
        this.networkParameters = bridgeConstants.getUldParams();
        this.uldContext = new Context(networkParameters);
        this.bridgeStorageConfiguration = bridgeStorageConfiguration;
    }

    public List<UTXO> getNewFederationUldUTXOs() throws IOException {
        if (newFederationUldUTXOs != null) {
            return newFederationUldUTXOs;
        }

        newFederationUldUTXOs = getFromRepository(NEW_FEDERATION_ULD_UTXOS_KEY, BridgeSerializationUtils::deserializeUTXOList);
        return newFederationUldUTXOs;
    }

    public void saveNewFederationUldUTXOs() throws IOException {
        if (newFederationUldUTXOs == null) {
            return;
        }

        saveToRepository(NEW_FEDERATION_ULD_UTXOS_KEY, newFederationUldUTXOs, BridgeSerializationUtils::serializeUTXOList);
    }

    public List<UTXO> getOldFederationUldUTXOs() throws IOException {
        if (oldFederationUldUTXOs != null) {
            return oldFederationUldUTXOs;
        }

        oldFederationUldUTXOs = getFromRepository(OLD_FEDERATION_ULD_UTXOS_KEY, BridgeSerializationUtils::deserializeUTXOList);
        return oldFederationUldUTXOs;
    }

    public void saveOldFederationUldUTXOs() throws IOException {
        if (oldFederationUldUTXOs == null) {
            return;
        }

        saveToRepository(OLD_FEDERATION_ULD_UTXOS_KEY, oldFederationUldUTXOs, BridgeSerializationUtils::serializeUTXOList);
    }

    public Map<Sha256Hash, Long> getUldTxHashesAlreadyProcessed() throws IOException {
        if (uldTxHashesAlreadyProcessed != null) {
            return uldTxHashesAlreadyProcessed;
        }

        uldTxHashesAlreadyProcessed = getFromRepository(ULD_TX_HASHES_ALREADY_PROCESSED_KEY, BridgeSerializationUtils::deserializeMapOfHashesToLong);
        return uldTxHashesAlreadyProcessed;
    }

    public void saveUldTxHashesAlreadyProcessed() {
        if (uldTxHashesAlreadyProcessed == null) {
            return;
        }

        safeSaveToRepository(ULD_TX_HASHES_ALREADY_PROCESSED_KEY, uldTxHashesAlreadyProcessed, BridgeSerializationUtils::serializeMapOfHashesToLong);
    }

    public ReleaseRequestQueue getReleaseRequestQueue() throws IOException {
        if (releaseRequestQueue != null) {
            return releaseRequestQueue;
        }

        releaseRequestQueue = getFromRepository(
                RELEASE_REQUEST_QUEUE,
                data -> BridgeSerializationUtils.deserializeReleaseRequestQueue(data, networkParameters)
        );

        return releaseRequestQueue;
    }

    public void saveReleaseRequestQueue() {
        if (releaseRequestQueue == null) {
            return;
        }

        safeSaveToRepository(RELEASE_REQUEST_QUEUE, releaseRequestQueue, BridgeSerializationUtils::serializeReleaseRequestQueue);
    }

    public ReleaseTransactionSet getReleaseTransactionSet() throws IOException {
        if (releaseTransactionSet != null) {
            return releaseTransactionSet;
        }

        releaseTransactionSet = getFromRepository(
                RELEASE_TX_SET,
                data -> BridgeSerializationUtils.deserializeReleaseTransactionSet(data, networkParameters)
        );

        return releaseTransactionSet;
    }

    public void saveReleaseTransactionSet() {
        if (releaseTransactionSet == null) {
            return;
        }

        safeSaveToRepository(RELEASE_TX_SET, releaseTransactionSet, BridgeSerializationUtils::serializeReleaseTransactionSet);
    }

    public SortedMap<Keccak256, UldTransaction> getUscTxsWaitingForSignatures() throws IOException {
        if (uscTxsWaitingForSignatures != null) {
            return uscTxsWaitingForSignatures;
        }

        uscTxsWaitingForSignatures = getFromRepository(
                USC_TXS_WAITING_FOR_SIGNATURES_KEY,
                data -> BridgeSerializationUtils.deserializeMap(data, networkParameters, false)
        );
        return uscTxsWaitingForSignatures;
    }

    public void saveUscTxsWaitingForSignatures() {
        if (uscTxsWaitingForSignatures == null) {
            return;
        }

        safeSaveToRepository(USC_TXS_WAITING_FOR_SIGNATURES_KEY, uscTxsWaitingForSignatures, BridgeSerializationUtils::serializeMap);
    }

    public Federation getNewFederation() {
        if (newFederation != null) {
            return newFederation;
        }

        newFederation = safeGetFromRepository(NEW_FEDERATION_KEY,
                data ->
                        data == null
                        ? null
                        : BridgeSerializationUtils.deserializeFederation(data, uldContext)
        );
        return newFederation;
    }

    public void setNewFederation(Federation federation) {
        newFederation = federation;
    }

    /**
     * Save the new federation
     * Only saved if a federation was set with BridgeStorageProvider::setNewFederation
     */
    public void saveNewFederation() {
        if (newFederation == null) {
            return;
        }

        safeSaveToRepository(NEW_FEDERATION_KEY, newFederation, BridgeSerializationUtils::serializeFederation);
    }

    public Federation getOldFederation() {
        if (oldFederation != null || shouldSaveOldFederation) {
            return oldFederation;
        }

        oldFederation = safeGetFromRepository(OLD_FEDERATION_KEY,
                data -> data == null
                        ? null
                        : BridgeSerializationUtils.deserializeFederation(data, uldContext)
        );
        return oldFederation;
    }

    public void setOldFederation(Federation federation) {
        shouldSaveOldFederation = true;
        oldFederation = federation;
    }

    /**
     * Save the old federation
     */
    public void saveOldFederation() {
        if (shouldSaveOldFederation) {
            safeSaveToRepository(OLD_FEDERATION_KEY, oldFederation, BridgeSerializationUtils::serializeFederation);
        }
    }

    public PendingFederation getPendingFederation() {
        if (pendingFederation != null || shouldSavePendingFederation) {
            return pendingFederation;
        }

        pendingFederation = safeGetFromRepository(PENDING_FEDERATION_KEY,
                data -> data == null
                        ? null :
                        BridgeSerializationUtils.deserializePendingFederation(data)
        );
        return pendingFederation;
    }

    public void setPendingFederation(PendingFederation federation) {
        shouldSavePendingFederation = true;
        pendingFederation = federation;
    }

    /**
     * Save the pending federation
     */
    public void savePendingFederation() {
        if (shouldSavePendingFederation) {
            safeSaveToRepository(PENDING_FEDERATION_KEY, pendingFederation, BridgeSerializationUtils::serializePendingFederation);
        }
    }

    /**
     * Save the federation election
     */
    public void saveFederationElection() {
        if (federationElection == null) {
            return;
        }

        safeSaveToRepository(FEDERATION_ELECTION_KEY, federationElection, BridgeSerializationUtils::serializeElection);
    }

    public ABICallElection getFederationElection(AddressBasedAuthorizer authorizer) {
        if (federationElection != null) {
            return federationElection;
        }

        federationElection = safeGetFromRepository(FEDERATION_ELECTION_KEY, data -> (data == null)? new ABICallElection(authorizer) : BridgeSerializationUtils.deserializeElection(data, authorizer));
        return federationElection;
    }

    /**
     * Save the lock whitelist
     */
    public void saveLockWhitelist() {
        if (lockWhitelist == null) {
            return;
        }

        List<OneOffWhiteListEntry> oneOffEntries = lockWhitelist.getAll(OneOffWhiteListEntry.class);
        safeSaveToRepository(LOCK_ONE_OFF_WHITELIST_KEY, Pair.of(oneOffEntries, lockWhitelist.getDisableBlockHeight()), BridgeSerializationUtils::serializeOneOffLockWhitelist);

        if (this.bridgeStorageConfiguration.getUnlimitedWhitelistEnabled()) {
            List<UnlimitedWhiteListEntry> unlimitedEntries = lockWhitelist.getAll(UnlimitedWhiteListEntry.class);
            safeSaveToRepository(LOCK_UNLIMITED_WHITELIST_KEY, unlimitedEntries, BridgeSerializationUtils::serializeUnlimitedLockWhitelist);
        }
    }

    public LockWhitelist getLockWhitelist() {
        if (lockWhitelist != null) {
            return lockWhitelist;
        }

        Pair<HashMap<Address, OneOffWhiteListEntry>, Integer> oneOffWhitelistAndDisableBlockHeightData =
                safeGetFromRepository(LOCK_ONE_OFF_WHITELIST_KEY,
                        data -> BridgeSerializationUtils.deserializeOneOffLockWhitelistAndDisableBlockHeight(data, uldContext.getParams()));
        if (oneOffWhitelistAndDisableBlockHeightData == null) {
            lockWhitelist = new LockWhitelist(new HashMap<>());
            return lockWhitelist;
        }

        Map<Address, LockWhitelistEntry> whitelistedAddresses = new HashMap<>();

        whitelistedAddresses.putAll(oneOffWhitelistAndDisableBlockHeightData.getLeft());

        if (this.bridgeStorageConfiguration.getUnlimitedWhitelistEnabled()) {
            whitelistedAddresses.putAll(safeGetFromRepository(LOCK_UNLIMITED_WHITELIST_KEY,
                    data -> BridgeSerializationUtils.deserializeUnlimitedLockWhitelistEntries(data, uldContext.getParams())));
        }

        lockWhitelist = new LockWhitelist(whitelistedAddresses, oneOffWhitelistAndDisableBlockHeightData.getRight());

        return lockWhitelist;
    }

    public Coin getFeePerKb() {
        if (feePerKb != null) {
            return feePerKb;
        }

        feePerKb = safeGetFromRepository(FEE_PER_KB_KEY, BridgeSerializationUtils::deserializeCoin);
        return feePerKb;
    }

    public void setFeePerKb(Coin feePerKb) {
        this.feePerKb = feePerKb;
    }

    public void saveFeePerKb() {
        if (feePerKb == null) {
            return;
        }

        safeSaveToRepository(FEE_PER_KB_KEY, feePerKb, BridgeSerializationUtils::serializeCoin);
    }

    /**
     * Save the fee per kb election
     */
    public void saveFeePerKbElection() {
        if (feePerKbElection == null) {
            return;
        }

        safeSaveToRepository(FEE_PER_KB_ELECTION_KEY, feePerKbElection, BridgeSerializationUtils::serializeElection);
    }


    public ABICallElection getFeePerKbElection(AddressBasedAuthorizer authorizer) {
        if (feePerKbElection != null) {
            return feePerKbElection;
        }

        feePerKbElection = safeGetFromRepository(FEE_PER_KB_ELECTION_KEY, data -> BridgeSerializationUtils.deserializeElection(data, authorizer));
        return feePerKbElection;
    }

    public void save() throws IOException {
        saveUldTxHashesAlreadyProcessed();

        saveReleaseRequestQueue();
        saveReleaseTransactionSet();
        saveUscTxsWaitingForSignatures();

        saveNewFederation();
        saveNewFederationUldUTXOs();

        saveOldFederation();
        saveOldFederationUldUTXOs();

        savePendingFederation();

        saveFederationElection();

        saveLockWhitelist();

        saveFeePerKb();
        saveFeePerKbElection();
    }

    private <T> T safeGetFromRepository(DataWord keyAddress, RepositoryDeserializer<T> deserializer) {
        try {
            return getFromRepository(keyAddress, deserializer);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to get from repository: " + keyAddress, ioe);
        }
    }

    private <T> T getFromRepository(DataWord keyAddress, RepositoryDeserializer<T> deserializer) throws IOException {
        byte[] data = repository.getStorageBytes(contractAddress, keyAddress);
        return deserializer.deserialize(data);
    }

    private <T> void safeSaveToRepository(DataWord addressKey, T object, RepositorySerializer<T> serializer) {
        try {
            saveToRepository(addressKey, object, serializer);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to save to repository: " + addressKey, ioe);
        }
    }

    private <T> void saveToRepository(DataWord addressKey, T object, RepositorySerializer<T> serializer) throws IOException {
        byte[] data = null;
        if (object != null) {
            data = serializer.serialize(object);
        }
        repository.addStorageBytes(contractAddress, addressKey, data);
    }

    private interface RepositoryDeserializer<T> {
        T deserialize(byte[] data) throws IOException;
    }

    private interface RepositorySerializer<T> {
        byte[] serialize(T object) throws IOException;
    }
}
