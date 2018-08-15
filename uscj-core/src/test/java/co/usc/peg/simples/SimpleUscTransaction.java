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

import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.Keccak256Helper;

/**
 * Created by ajlopez on 6/8/2016.
 */
public class SimpleUscTransaction extends Transaction {
    private final Keccak256 hash;

    public SimpleUscTransaction(byte[] hash) {
        super(null);
        this.hash = hash == null ? null : new Keccak256(hash);
        this.sender = new UscAddress(ECKey.fromPrivate(Keccak256Helper.keccak256("cow".getBytes())).getAddress());
    }

    @Override
    public Keccak256 getHash() { return hash; }

    @Override
    public Coin getValue() {
        return Coin.valueOf(10000000);
    }
}
