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

package co.usc.validators;

import co.usc.core.Coin;
import co.usc.crypto.Keccak256;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mario on 26/12/16.
 */
public class TxsMinGasPriceValidatorTest {

    private static final Coin BLOCK_MGP = Coin.valueOf(10);

    @Test
    public void blockWithNullTxList() {
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getTransactionsList()).thenReturn(null);
        Mockito.when(block.getMinimumGasPrice()).thenReturn(BLOCK_MGP);
        TxsMinGasPriceRule tmgpv = new TxsMinGasPriceRule();

        Assert.assertTrue(tmgpv.isValid(block));
    }

    @Test
    public void blockWithEmptyTxList() {
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getTransactionsList()).thenReturn(new ArrayList<>());
        Mockito.when(block.getMinimumGasPrice()).thenReturn(BLOCK_MGP);
        TxsMinGasPriceRule tmgpv = new TxsMinGasPriceRule();

        Assert.assertTrue(tmgpv.isValid(block));
    }

    @Test
    public void blockWithNullMGP() {
        Block block = Mockito.mock(Block.class);
        TxsMinGasPriceRule tmgpv = new TxsMinGasPriceRule();

        Assert.assertFalse(tmgpv.isValid(block));
    }

    @Test
    public void blockWithAllValidTx() {
        Block block = Mockito.mock(Block.class);
        List<Transaction> txs = buildTxList(10, 0, BLOCK_MGP);
        Mockito.when(block.getTransactionsList()).thenReturn(txs);
        Mockito.when(block.getMinimumGasPrice()).thenReturn(BLOCK_MGP);

        TxsMinGasPriceRule tmgpv = new TxsMinGasPriceRule();

        Assert.assertTrue(tmgpv.isValid(block));
    }

    @Test
    public void blockWithAllInvalidTx() {
        Block block = Mockito.mock(Block.class);
        List<Transaction> txs = buildTxList(0, 10, BLOCK_MGP);
        Mockito.when(block.getTransactionsList()).thenReturn(txs);
        Mockito.when(block.getMinimumGasPrice()).thenReturn(BLOCK_MGP);

        TxsMinGasPriceRule tmgpv = new TxsMinGasPriceRule();

        Assert.assertFalse(tmgpv.isValid(block));
    }

    @Test
    public void blockWithSomeInvalidTx() {
        Block block = Mockito.mock(Block.class);
        List<Transaction> txs = buildTxList(10, 10, BLOCK_MGP);
        Mockito.when(block.getTransactionsList()).thenReturn(txs);
        Mockito.when(block.getMinimumGasPrice()).thenReturn(BLOCK_MGP);

        TxsMinGasPriceRule tmgpv = new TxsMinGasPriceRule();

        Assert.assertFalse(tmgpv.isValid(block));
    }

    private List<Transaction> buildTxList(int validTxNbr, int invalidTxNbr, Coin blockGasPrice) {
        List<Transaction> ret = new ArrayList<>();

        for(int i = 0; i < validTxNbr; i++) {
            Transaction tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getGasPrice()).thenReturn(blockGasPrice.add(Coin.valueOf(1)));
            ret.add(tx);
        }

        for(int i = 0; i < invalidTxNbr; i++) {
            Transaction tx = Mockito.mock(Transaction.class);
            Mockito.when(tx.getGasPrice()).thenReturn(blockGasPrice.subtract(Coin.valueOf(1)));
            Mockito.when(tx.getHash()).thenReturn(Keccak256.ZERO_HASH);
            ret.add(tx);
        }
        return ret;
    }
}
