/*
 * This file is part of USC
 * Copyright (C) 2016 - 2018 USC developer team.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
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

package org.ethereum.net.eth.handler;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.eth.message.EthMessage;
import org.ethereum.sync.SyncStatistics;

import java.util.List;

import static org.ethereum.net.eth.EthVersion.*;

/**
 * It's quite annoying to always check {@code if (eth != null)} before accessing it. <br>
 *
 * This adapter helps to avoid such checks. It provides meaningful answers to Eth client
 * assuming that Eth hasn't been initialized yet. <br>
 *
 * Check {@link org.ethereum.net.server.Channel} for example.
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public class EthAdapter implements Eth {

    private final SyncStatistics syncStats = new SyncStatistics();

    @Override
    public boolean hasStatusPassed() {
        return false;
    }

    @Override
    public boolean hasStatusSucceeded() {
        return false;
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void logSyncStats() {
    }

    @Override
    public boolean isHashRetrievingDone() {
        return false;
    }

    @Override
    public boolean isHashRetrieving() {
        return false;
    }

    @Override
    public boolean isIdle() {
        return true;
    }

    @Override
    public SyncStatistics getStats() {
        return syncStats;
    }

    @Override
    public void disableTransactions() {
    }

    @Override
    public void enableTransactions() {
    }

    @Override
    public void sendTransaction(List<Transaction> tx) {
    }

    @Override
    public void sendNewBlock(Block newBlock) {
    }

    @Override
    public void sendNewBlockHashes(Block block) {

    }

    @Override
    public EthVersion getVersion() {
        return fromCode(UPPER);
    }

    @Override
    public void onSyncDone(boolean done) {
    }

    @Override
    public void sendStatus() {
    }

    @Override
    public void dropConnection() {
    }

    @Override
    public void sendMessage(EthMessage message) {
    }

    @Override
    public boolean isUsingNewProtocol() {
        return false;
    }
}
