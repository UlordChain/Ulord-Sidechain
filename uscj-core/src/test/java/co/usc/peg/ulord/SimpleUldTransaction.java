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

package co.usc.peg.ulord;

import co.usc.ulordj.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ajlopez on 6/1/2016.
 */
public class SimpleUldTransaction extends UldTransaction {
    private NetworkParameters networkParameters;
    private Sha256Hash hash;
    private Map<Sha256Hash, Integer> appears = new HashMap<>();

    public SimpleUldTransaction(NetworkParameters networkParameters, Sha256Hash hash) {
        super(networkParameters);
        this.hash = hash;
        this.networkParameters = networkParameters;
    }

    @Override
    public Sha256Hash getHash() {
        return this.hash;
    }

    @Override
    public Map<Sha256Hash, Integer> getAppearsInHashes() {
        return appears;
    }

    public void setAppearsInHashes(Map<Sha256Hash, Integer> appears) {
        this.appears = appears;
    }

    @Override
    public void verify() { }

    @Override
    public Coin getValueSentToMe(TransactionBag wallet) { return Coin.COIN; }

    @Override
    public TransactionInput getInput(long index) {
        return new TransactionInput(this.networkParameters, null, new byte[0]);
    }
}
