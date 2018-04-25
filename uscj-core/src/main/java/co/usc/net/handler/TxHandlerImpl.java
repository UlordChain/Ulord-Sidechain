/*
 * This file is part of RskJ
 * Copyright (C) 2017 USC Labs Ltd.
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

package co.usc.net.handler;

import co.usc.config.UscSystemProperties;
import org.ethereum.core.*;
import org.ethereum.listener.CompositeEthereumListener;

import java.util.List;

/**
 * TxHandler validates the incoming transactions
 * It does not check the nonce sequence by account,
 * TransactionPool controls that sequence, with two
 * list of transactions: pendig (in sequence), queued (out of sequence)
 */
public class TxHandlerImpl implements TxHandler {
    private final UscSystemProperties config;
    private Repository repository;
    private Blockchain blockchain;

    public TxHandlerImpl(UscSystemProperties config, CompositeEthereumListener compositeEthereumListener, Repository repository, Blockchain blockchain) {
        this.config = config;
        this.blockchain = blockchain;
        this.repository = repository;
    }

    @Override
    public List<Transaction> retrieveValidTxs(List<Transaction> txs) {
        return new TxValidator(config, repository, blockchain).filterTxs(txs);
    }
}
