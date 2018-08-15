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

import org.ethereum.core.AccountState;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TxValidatorAccountStateValidatorTest {

    @Test
    public void validAccountState() {
        AccountState state = Mockito.mock(AccountState.class);
        Mockito.when(state.isDeleted()).thenReturn(false);

        TxValidatorAccountStateValidator tvasv = new TxValidatorAccountStateValidator();
        Assert.assertTrue(tvasv.validate(null, state, null, null, Long.MAX_VALUE, false));
    }

    @Test
    public void invalidAccountState() {
        AccountState state = Mockito.mock(AccountState.class);
        Mockito.when(state.isDeleted()).thenReturn(true);

        TxValidatorAccountStateValidator tvasv = new TxValidatorAccountStateValidator();
        Assert.assertFalse(tvasv.validate(null, state, null, null, Long.MAX_VALUE, false));
    }
}
