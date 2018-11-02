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

import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.core.Sha256Hash;
import co.usc.ulordj.core.UTXO;
import co.usc.config.BridgeConstants;
import co.usc.crypto.Keccak256;
import org.apache.commons.collections4.map.HashedMap;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * DTO to send the contract state.
 * Not production code, just used for debugging.
 *
 * Created by mario on 27/09/2016.
 */
public class BridgeState {
    private final int uldBlockchainBestChainHeight;
    private final Map<Sha256Hash, Long> uldTxHashesAlreadyProcessed;
    private final List<UTXO> activeFederationUldUTXOs;
    private final SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures;
    private final ReleaseRequestQueue releaseRequestQueue;
    private final ReleaseTransactionSet releaseTransactionSet;

    private BridgeState(int uldBlockchainBestChainHeight, Map<Sha256Hash, Long> uldTxHashesAlreadyProcessed, List<UTXO> activeFederationUldUTXOs,
                        SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures, ReleaseRequestQueue releaseRequestQueue, ReleaseTransactionSet releaseTransactionSet) {
        this.uldBlockchainBestChainHeight = uldBlockchainBestChainHeight;
        this.uldTxHashesAlreadyProcessed = uldTxHashesAlreadyProcessed;
        this.activeFederationUldUTXOs = activeFederationUldUTXOs;
        this.uscTxsWaitingForSignatures = uscTxsWaitingForSignatures;
        this.releaseRequestQueue = releaseRequestQueue;
        this.releaseTransactionSet = releaseTransactionSet;
    }

    public BridgeState(int uldBlockchainBestChainHeight, BridgeStorageProvider provider) throws IOException {
        this(uldBlockchainBestChainHeight,
                provider.getUldTxHashesAlreadyProcessed(),
                provider.getNewFederationUldUTXOs(),
                provider.getUscTxsWaitingForSignatures(),
                provider.getReleaseRequestQueue(),
                provider.getReleaseTransactionSet());
    }

    public int getUldBlockchainBestChainHeight() {
        return this.uldBlockchainBestChainHeight;
    }

    public Map<Sha256Hash, Long> getUldTxHashesAlreadyProcessed() {
        return uldTxHashesAlreadyProcessed;
    }

    public List<UTXO> getActiveFederationUldUTXOs() {
        return activeFederationUldUTXOs;
    }

    public SortedMap<Keccak256, UldTransaction> getUscTxsWaitingForSignatures() {
        return uscTxsWaitingForSignatures;
    }

    public ReleaseRequestQueue getReleaseRequestQueue() {
        return releaseRequestQueue;
    }

    public ReleaseTransactionSet getReleaseTransactionSet() {
        return releaseTransactionSet;
    }

    @Override
    public String toString() {
        return "StateForDebugging{" + "\n" +
                "uldBlockchainBestChainHeight=" + uldBlockchainBestChainHeight + "\n" +
                ", uldTxHashesAlreadyProcessed=" + uldTxHashesAlreadyProcessed + "\n" +
                ", activeFederationUldUTXOs=" + activeFederationUldUTXOs + "\n" +
                ", uscTxsWaitingForSignatures=" + uscTxsWaitingForSignatures + "\n" +
                ", releaseRequestQueue=" + releaseRequestQueue + "\n" +
                ", releaseTransactionSet=" + releaseTransactionSet + "\n" +
                '}';
    }

    public List<String> formatedAlreadyProcessedHashes() {
        List<String> hashes = new ArrayList<>();
        if(this.uldTxHashesAlreadyProcessed != null) {
            this.uldTxHashesAlreadyProcessed.keySet().forEach(s -> hashes.add(s.toString()));
        }
        return hashes;
    }

    public Map<String, Object> stateToMap() {
        Map<String, Object> result = new HashedMap<>();
        result.put("uldTxHashesAlreadyProcessed", this.formatedAlreadyProcessedHashes());
        result.put("uscTxsWaitingForSignatures", this.toStringList(uscTxsWaitingForSignatures.keySet()));
        result.put("uldBlockchainBestChainHeight", this.uldBlockchainBestChainHeight);
        return result;
    }

    public byte[] getEncoded() throws IOException {
        byte[] rlpUldBlockchainBestChainHeight = RLP.encodeBigInteger(BigInteger.valueOf(this.uldBlockchainBestChainHeight));
        byte[] rlpUldTxHashesAlreadyProcessed = RLP.encodeElement(BridgeSerializationUtils.serializeMapOfHashesToLong(uldTxHashesAlreadyProcessed));
        byte[] rlpActiveFederationUldUTXOs = RLP.encodeElement(BridgeSerializationUtils.serializeUTXOList(activeFederationUldUTXOs));
        byte[] rlpUscTxsWaitingForSignatures = RLP.encodeElement(BridgeSerializationUtils.serializeMap(uscTxsWaitingForSignatures));
        byte[] rlpReleaseRequestQueue = RLP.encodeElement(BridgeSerializationUtils.serializeReleaseRequestQueue(releaseRequestQueue));
        byte[] rlpReleaseTransactionSet = RLP.encodeElement(BridgeSerializationUtils.serializeReleaseTransactionSet(releaseTransactionSet));

        return RLP.encodeList(rlpUldBlockchainBestChainHeight, rlpUldTxHashesAlreadyProcessed, rlpActiveFederationUldUTXOs, rlpUscTxsWaitingForSignatures, rlpReleaseRequestQueue, rlpReleaseTransactionSet);
    }

    public static BridgeState create(BridgeConstants bridgeConstants, byte[] data) throws IOException {
        RLPList rlpList = (RLPList)RLP.decode2(data).get(0);

        byte[] uldBlockchainBestChainHeightBytes = rlpList.get(0).getRLPData();
        int uldBlockchainBestChainHeight = uldBlockchainBestChainHeightBytes == null ? 0 : (new BigInteger(1, uldBlockchainBestChainHeightBytes)).intValue();
        byte[] uldTxHashesAlreadyProcessedBytes = rlpList.get(1).getRLPData();
        Map<Sha256Hash, Long> uldTxHashesAlreadyProcessed = BridgeSerializationUtils.deserializeMapOfHashesToLong(uldTxHashesAlreadyProcessedBytes);
        byte[] uldUTXOsBytes = rlpList.get(2).getRLPData();
        List<UTXO> uldUTXOs = BridgeSerializationUtils.deserializeUTXOList(uldUTXOsBytes);
        byte[] uscTxsWaitingForSignaturesBytes = rlpList.get(3).getRLPData();
        SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures = BridgeSerializationUtils.deserializeMap(uscTxsWaitingForSignaturesBytes, bridgeConstants.getUldParams(), false);
        byte[] releaseRequestQueueBytes = rlpList.get(4).getRLPData();
        ReleaseRequestQueue releaseRequestQueue = BridgeSerializationUtils.deserializeReleaseRequestQueue(releaseRequestQueueBytes, bridgeConstants.getUldParams());
        byte[] releaseTransactionSetBytes = rlpList.get(5).getRLPData();
        ReleaseTransactionSet releaseTransactionSet = BridgeSerializationUtils.deserializeReleaseTransactionSet(releaseTransactionSetBytes, bridgeConstants.getUldParams());

        return new BridgeState(
                uldBlockchainBestChainHeight,
                uldTxHashesAlreadyProcessed,
                uldUTXOs,
                uscTxsWaitingForSignatures,
                releaseRequestQueue,
                releaseTransactionSet
        );
    }

    private List<String> toStringList(Set<Keccak256> keys) {
        List<String> hashes = new ArrayList<>();
        if(keys != null) {
            keys.forEach(s -> hashes.add(s.toHexString()));
        }

        return hashes;
    }
}
