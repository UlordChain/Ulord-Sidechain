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
import co.usc.ulordj.core.NetworkParameters;
import co.usc.config.TestSystemProperties;
import co.usc.crypto.Keccak256;
import co.usc.config.TestSystemProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by mario on 20/04/17.
 */
public class StateForFederatorTest {

    private static final String SHA3_1 = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String SHA3_2 = "2222222222222222222222222222222222222222222222222222222222222222";
    private static final String SHA3_3 = "3333333333333333333333333333333333333333333333333333333333333333";
    private static final String SHA3_4 = "4444444444444444444444444444444444444444444444444444444444444444";

    private static final NetworkParameters NETWORK_PARAMETERS = new TestSystemProperties().getBlockchainConfig().getCommonConstants().getBridgeConstants().getUldParams();

    @Test
    public void serialize() {
        Keccak256 hash1 = new Keccak256(SHA3_1);
        Keccak256 hash2 = new Keccak256(SHA3_2);
        Keccak256 hash3 = new Keccak256(SHA3_3);
        Keccak256 hash4 = new Keccak256(SHA3_4);

        UldTransaction tx1 = new UldTransaction(NETWORK_PARAMETERS);
        UldTransaction tx2 = new UldTransaction(NETWORK_PARAMETERS);
        UldTransaction tx3 = new UldTransaction(NETWORK_PARAMETERS);
        UldTransaction tx4 = new UldTransaction(NETWORK_PARAMETERS);

        SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures = new TreeMap<>();
        uscTxsWaitingForSignatures.put(hash1, tx1);
        uscTxsWaitingForSignatures.put(hash2, tx2);

        SortedMap<Keccak256, Pair<UldTransaction, Long>> uscTxsWaitingForBroadcasting = new TreeMap<>();
        uscTxsWaitingForBroadcasting.put(hash3, Pair.of(tx3, 3L));
        uscTxsWaitingForBroadcasting.put(hash4, Pair.of(tx4, 4L));

        StateForFederator stateForFederator = new StateForFederator(uscTxsWaitingForSignatures);

        byte[] encoded = stateForFederator.getEncoded();

        Assert.assertTrue(encoded.length > 0);

        StateForFederator reverseResult = new StateForFederator(encoded, NETWORK_PARAMETERS);

        Assert.assertNotNull(reverseResult);
        Assert.assertEquals(2, reverseResult.getUscTxsWaitingForSignatures().size());

        Assert.assertEquals(tx1, reverseResult.getUscTxsWaitingForSignatures().get(hash1));
        Assert.assertEquals(tx2, reverseResult.getUscTxsWaitingForSignatures().get(hash2));

        Assert.assertTrue(checkKeys(reverseResult.getUscTxsWaitingForSignatures().keySet(), hash1, hash2));
    }

    private boolean checkKeys(Set<Keccak256> keccak256s, Keccak256... keys) {
        for(Keccak256 sha3 : keys)
            if(!keccak256s.contains(sha3))
                return false;
        return true;
    }
}
