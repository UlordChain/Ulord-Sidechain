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

import co.usc.crypto.Keccak256;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.core.UldTransaction;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.util.SortedMap;

/**
 * Created by mario on 20/04/17.
 */
public class StateForFederator {
    private SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures;

    public StateForFederator(SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures) {
        this.uscTxsWaitingForSignatures = uscTxsWaitingForSignatures;
    }

    public StateForFederator(byte[] rlpData, NetworkParameters parameters) {
        RLPList rlpList = (RLPList) RLP.decode2(rlpData).get(0);
        byte[] encodedWaitingForSign = rlpList.get(0).getRLPData();

        this.uscTxsWaitingForSignatures = BridgeSerializationUtils.deserializeMap(encodedWaitingForSign, parameters, false);
    }

    public SortedMap<Keccak256, UldTransaction> getUscTxsWaitingForSignatures() {
        return uscTxsWaitingForSignatures;
    }

    public byte[] getEncoded() {
        byte[] encodedWaitingForSign = BridgeSerializationUtils.serializeMap(this.uscTxsWaitingForSignatures);
        return RLP.encodeList(encodedWaitingForSign);
    }
}
