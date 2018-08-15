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

package co.usc.peg.utils;

import co.usc.ulordj.core.UldECKey;
import co.usc.ulordj.core.UldTransaction;
import co.usc.config.BridgeConstants;
import co.usc.peg.Bridge;
import co.usc.peg.Federation;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.util.RLP;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.PrecompiledContracts;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for logging events triggered by BridgeContract.
 *
 * @author martin.medina
 */
public class BridgeEventLoggerImpl implements BridgeEventLogger {

    private static final byte[] BRIDGE_CONTRACT_ADDRESS = PrecompiledContracts.BRIDGE_ADDR.getBytes();

    private final BridgeConstants bridgeConstants;

    private List<LogInfo> logs;

    public BridgeEventLoggerImpl(BridgeConstants bridgeConstants, List<LogInfo> logs) {
        this.bridgeConstants = bridgeConstants;
        this.logs = logs;
    }

    public void logUpdateCollections(Transaction uscTx) {
        this.logs.add(
                new LogInfo(BRIDGE_CONTRACT_ADDRESS,
                            Collections.singletonList(Bridge.UPDATE_COLLECTIONS_TOPIC),
                            RLP.encodeElement(uscTx.getSender().getBytes())
                )
        );
    }

    public void logAddSignature(UldECKey federatorPublicKey, UldTransaction uldTx, byte[] uscTxHash) {
        List<DataWord> topics = Collections.singletonList(Bridge.ADD_SIGNATURE_TOPIC);
        byte[] data = RLP.encodeList(RLP.encodeString(uldTx.getHashAsString()),
                                    RLP.encodeElement(federatorPublicKey.getPubKeyHash()),
                                    RLP.encodeElement(uscTxHash));

        this.logs.add(new LogInfo(BRIDGE_CONTRACT_ADDRESS, topics, data));
    }

    public void logReleaseUld(UldTransaction uldTx) {
        List<DataWord> topics = Collections.singletonList(Bridge.RELEASE_ULD_TOPIC);
        byte[] data = RLP.encodeList(RLP.encodeString(uldTx.getHashAsString()), RLP.encodeElement(uldTx.ulordSerialize()));

        this.logs.add(new LogInfo(BRIDGE_CONTRACT_ADDRESS, topics, data));
    }

    public void logCommitFederation(Block executionBlock, Federation oldFederation, Federation newFederation) {
        List<DataWord> topics = Collections.singletonList(Bridge.COMMIT_FEDERATION_TOPIC);

        byte[] oldFedFlatPubKeys = flatKeysAsRlpCollection(oldFederation.getPublicKeys());
        byte[] oldFedData = RLP.encodeList(RLP.encodeElement(oldFederation.getAddress().getHash160()), RLP.encodeList(oldFedFlatPubKeys));

        byte[] newFedFlatPubKeys = flatKeysAsRlpCollection(newFederation.getPublicKeys());
        byte[] newFedData = RLP.encodeList(RLP.encodeElement(newFederation.getAddress().getHash160()), RLP.encodeList(newFedFlatPubKeys));

        long newFedActivationBlockNumber = executionBlock.getNumber() + this.bridgeConstants.getFederationActivationAge();

        byte[] data = RLP.encodeList(oldFedData, newFedData, RLP.encodeString(Long.toString(newFedActivationBlockNumber)));

        this.logs.add(new LogInfo(BRIDGE_CONTRACT_ADDRESS, topics, data));
    }

    private byte[] flatKeysAsRlpCollection(List<UldECKey> keys) {
        List<byte[]> pubKeys = keys.stream()
                                    .map(k -> RLP.encodeElement(k.getPubKey()))
                                    .collect(Collectors.toList());
        int pubKeysLength = pubKeys.stream().mapToInt(key -> key.length).sum();

        byte[] flatPubKeys = new byte[pubKeysLength];
        int copyPos = 0;
        for(byte[] key : pubKeys) {
            System.arraycopy(key, 0, flatPubKeys, copyPos, key.length);
            copyPos += key.length;
        }

        return flatPubKeys;
    }
}
