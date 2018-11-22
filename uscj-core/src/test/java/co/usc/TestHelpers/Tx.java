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

package co.usc.TestHelpers;

import co.usc.config.UscSystemProperties;
import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import org.ethereum.TestUtils;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;


public class Tx {

    public static Transaction create(
            UscSystemProperties config,
            long value,
            long gaslimit,
            long gasprice,
            long nonce,
            long data,
            long sender) {
        Random r = new Random(sender);
        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(transaction.getValue()).thenReturn(new Coin(BigInteger.valueOf(value)));
        Mockito.when(transaction.getGasLimit()).thenReturn(BigInteger.valueOf(gaslimit).toByteArray());
        Mockito.when(transaction.getGasLimitAsInteger()).thenReturn(BigInteger.valueOf(gaslimit));
        Mockito.when(transaction.getGasPrice()).thenReturn(Coin.valueOf(gasprice));
        Mockito.when(transaction.getNonce()).thenReturn(BigInteger.valueOf(nonce).toByteArray());
        Mockito.when(transaction.getNonceAsInteger()).thenReturn(BigInteger.valueOf(nonce));

        byte[] returnSenderBytes = new byte[20];
        r.nextBytes(returnSenderBytes);
        UscAddress returnSender = new UscAddress(returnSenderBytes);

        byte[] returnReceiveAddressBytes = new byte[20];
        r.nextBytes(returnReceiveAddressBytes);
        UscAddress returnReceiveAddress = new UscAddress(returnReceiveAddressBytes);

        Mockito.when(transaction.getSender()).thenReturn(returnSender);
        Mockito.when(transaction.getHash()).thenReturn(new Keccak256(TestUtils.randomBytes(32)));
        Mockito.when(transaction.acceptTransactionSignature(config.getBlockchainConfig().getCommonConstants().getChainId())).thenReturn(Boolean.TRUE);
        Mockito.when(transaction.getReceiveAddress()).thenReturn(returnReceiveAddress);
        ArrayList<Byte> bytes = new ArrayList();
        long amount = 21000;
        if (data != 0) {
            data /= 2;
            for (int i = 0; i < data / 4; i++) {
                bytes.add((byte)0);
                amount += 4;
            }
            for (int i = 0; i < data / 68; i++) {
                bytes.add((byte)1);
                amount += 68;
            }
        }
        int n = bytes.size();
        byte b[] = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = bytes.get(i);
        }
        Mockito.when(transaction.getData()).thenReturn(b);
        Mockito.when(transaction.transactionCost(any(Block.class), eq(config.getBlockchainConfig()))).thenReturn(amount);

        return transaction;
    }
}
