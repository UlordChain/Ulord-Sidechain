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

package co.usc.net.handler.txvalidator;

import co.usc.crypto.Keccak256;
import co.usc.remasc.RemascTransaction;
import org.ethereum.core.Transaction;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TxValidatorNotRemascTxValidatorTest {

    @Test
    public void remascTx() {
        TxValidatorNotRemascTxValidator validator = new TxValidatorNotRemascTxValidator();
        Transaction tx1 = Mockito.mock(RemascTransaction.class);
        Mockito.when(tx1.getHash()).thenReturn(Keccak256.ZERO_HASH);
        Assert.assertFalse(validator.validate(tx1, null, null, null, 0, false));
    }

    @Test
    public void commonTx() {
        TxValidatorNotRemascTxValidator validator = new TxValidatorNotRemascTxValidator();
        Assert.assertTrue(validator.validate(Mockito.mock(Transaction.class), null, null, null, 0, false));
    }
}
