/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
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
    private final int UldBlockchainBestChainHeight;
    private final Map<Sha256Hash, Long> btcTxHashesAlreadyProcessed;
    private final List<UTXO> activeFederationBtcUTXOs;
    private final SortedMap<Keccak256, UldTransaction> rskTxsWaitingForSignatures;
    private final ReleaseRequestQueue releaseRequestQueue;
    private final ReleaseTransactionSet releaseTransactionSet;

    private BridgeState(int UldBlockchainBestChainHeight, Map<Sha256Hash, Long> btcTxHashesAlreadyProcessed, List<UTXO> activeFederationBtcUTXOs,
                        SortedMap<Keccak256, UldTransaction> rskTxsWaitingForSignatures, ReleaseRequestQueue releaseRequestQueue, ReleaseTransactionSet releaseTransactionSet) {
        this.UldBlockchainBestChainHeight = UldBlockchainBestChainHeight;
        this.btcTxHashesAlreadyProcessed = btcTxHashesAlreadyProcessed;
        this.activeFederationBtcUTXOs = activeFederationBtcUTXOs;
        this.rskTxsWaitingForSignatures = rskTxsWaitingForSignatures;
        this.releaseRequestQueue = releaseRequestQueue;
        this.releaseTransactionSet = releaseTransactionSet;
    }

    public BridgeState(int UldBlockchainBestChainHeight, BridgeStorageProvider provider) throws IOException {
        this(UldBlockchainBestChainHeight,
                provider.getBtcTxHashesAlreadyProcessed(),
                provider.getNewFederationBtcUTXOs(),
                provider.getRskTxsWaitingForSignatures(),
                provider.getReleaseRequestQueue(),
                provider.getReleaseTransactionSet());
    }

    public int getUldBlockchainBestChainHeight() {
        return this.UldBlockchainBestChainHeight;
    }

    public Map<Sha256Hash, Long> getBtcTxHashesAlreadyProcessed() {
        return btcTxHashesAlreadyProcessed;
    }

    public List<UTXO> getActiveFederationBtcUTXOs() {
        return activeFederationBtcUTXOs;
    }

    public SortedMap<Keccak256, UldTransaction> getRskTxsWaitingForSignatures() {
        return rskTxsWaitingForSignatures;
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
                "UldBlockchainBestChainHeight=" + UldBlockchainBestChainHeight + "\n" +
                ", btcTxHashesAlreadyProcessed=" + btcTxHashesAlreadyProcessed + "\n" +
                ", activeFederationBtcUTXOs=" + activeFederationBtcUTXOs + "\n" +
                ", rskTxsWaitingForSignatures=" + rskTxsWaitingForSignatures + "\n" +
                ", releaseRequestQueue=" + releaseRequestQueue + "\n" +
                ", releaseTransactionSet=" + releaseTransactionSet + "\n" +
                '}';
    }

    public List<String> formatedAlreadyProcessedHashes() {
        List<String> hashes = new ArrayList<>();
        if(this.btcTxHashesAlreadyProcessed != null) {
            this.btcTxHashesAlreadyProcessed.keySet().forEach(s -> hashes.add(s.toString()));
        }
        return hashes;
    }

    public Map<String, Object> stateToMap() {
        Map<String, Object> result = new HashedMap<>();
        result.put("btcTxHashesAlreadyProcessed", this.formatedAlreadyProcessedHashes());
        result.put("rskTxsWaitingForSignatures", this.toStringList(rskTxsWaitingForSignatures.keySet()));
        result.put("UldBlockchainBestChainHeight", this.UldBlockchainBestChainHeight);
        return result;
    }

    public byte[] getEncoded() throws IOException {
        byte[] rlpUldBlockchainBestChainHeight = RLP.encodeBigInteger(BigInteger.valueOf(this.UldBlockchainBestChainHeight));
        byte[] rlpBtcTxHashesAlreadyProcessed = RLP.encodeElement(BridgeSerializationUtils.serializeMapOfHashesToLong(btcTxHashesAlreadyProcessed));
        byte[] rlpActiveFederationBtcUTXOs = RLP.encodeElement(BridgeSerializationUtils.serializeUTXOList(activeFederationBtcUTXOs));
        byte[] rlpRskTxsWaitingForSignatures = RLP.encodeElement(BridgeSerializationUtils.serializeMap(rskTxsWaitingForSignatures));
        byte[] rlpReleaseRequestQueue = RLP.encodeElement(BridgeSerializationUtils.serializeReleaseRequestQueue(releaseRequestQueue));
        byte[] rlpReleaseTransactionSet = RLP.encodeElement(BridgeSerializationUtils.serializeReleaseTransactionSet(releaseTransactionSet));

        return RLP.encodeList(rlpUldBlockchainBestChainHeight, rlpBtcTxHashesAlreadyProcessed, rlpActiveFederationBtcUTXOs, rlpRskTxsWaitingForSignatures, rlpReleaseRequestQueue, rlpReleaseTransactionSet);
    }

    public static BridgeState create(BridgeConstants bridgeConstants, byte[] data) throws IOException {
        RLPList rlpList = (RLPList)RLP.decode2(data).get(0);

        byte[] UldBlockchainBestChainHeightBytes = rlpList.get(0).getRLPData();
        int UldBlockchainBestChainHeight = UldBlockchainBestChainHeightBytes == null ? 0 : (new BigInteger(1, UldBlockchainBestChainHeightBytes)).intValue();
        byte[] btcTxHashesAlreadyProcessedBytes = rlpList.get(1).getRLPData();
        Map<Sha256Hash, Long> btcTxHashesAlreadyProcessed = BridgeSerializationUtils.deserializeMapOfHashesToLong(btcTxHashesAlreadyProcessedBytes);
        byte[] btcUTXOsBytes = rlpList.get(2).getRLPData();
        List<UTXO> btcUTXOs = BridgeSerializationUtils.deserializeUTXOList(btcUTXOsBytes);
        byte[] rskTxsWaitingForSignaturesBytes = rlpList.get(3).getRLPData();
        SortedMap<Keccak256, UldTransaction> rskTxsWaitingForSignatures = BridgeSerializationUtils.deserializeMap(rskTxsWaitingForSignaturesBytes, bridgeConstants.getBtcParams(), false);
        byte[] releaseRequestQueueBytes = rlpList.get(4).getRLPData();
        ReleaseRequestQueue releaseRequestQueue = BridgeSerializationUtils.deserializeReleaseRequestQueue(releaseRequestQueueBytes, bridgeConstants.getBtcParams());
        byte[] releaseTransactionSetBytes = rlpList.get(5).getRLPData();
        ReleaseTransactionSet releaseTransactionSet = BridgeSerializationUtils.deserializeReleaseTransactionSet(releaseTransactionSetBytes, bridgeConstants.getBtcParams());

        return new BridgeState(
                UldBlockchainBestChainHeight,
                btcTxHashesAlreadyProcessed,
                btcUTXOs,
                rskTxsWaitingForSignatures,
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
