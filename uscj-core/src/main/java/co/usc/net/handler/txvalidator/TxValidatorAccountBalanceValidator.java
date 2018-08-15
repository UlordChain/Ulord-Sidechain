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

import co.usc.core.Coin;
import org.ethereum.core.AccountState;
import org.ethereum.core.Transaction;

import javax.annotation.Nullable;
import java.math.BigInteger;

/**
 * Checks if an account can pay the transaction execution cost
 */
public class TxValidatorAccountBalanceValidator implements TxValidatorStep {

    @Override
    public boolean validate(Transaction tx, @Nullable AccountState state, BigInteger gasLimit, Coin minimumGasPrice, long bestBlockNumber, boolean isFreeTx) {
        if (isFreeTx) {
            return true;
        }

        if (state == null) {
            return false;
        }

        BigInteger txGasLimit = tx.getGasLimitAsInteger();
        Coin maximumPrice = tx.getGasPrice().multiply(txGasLimit);
        return state.getBalance().compareTo(maximumPrice) >= 0;
    }
}
