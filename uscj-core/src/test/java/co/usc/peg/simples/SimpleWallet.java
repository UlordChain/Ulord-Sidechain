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

package co.usc.peg.simples;

import co.usc.ulordj.core.Context;
import co.usc.ulordj.wallet.SendRequest;
import co.usc.ulordj.wallet.Wallet;

/**
 * Created by ajlopez on 6/9/2016.
 */
public class SimpleWallet extends Wallet {
    public SimpleWallet(Context context) {
        super(context);
    }

    @Override
    public void completeTx(SendRequest req) {
        return;
    }
}
