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

package org.ethereum.jsontestsuite.builder;

import co.usc.config.TestSystemProperties;
import co.usc.core.UscAddress;
import co.usc.db.RepositoryImpl;
import co.usc.db.TrieStorePoolOnMemory;
import co.usc.trie.TrieStoreImpl;
import org.ethereum.core.AccountState;
import org.ethereum.core.Repository;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.ContractDetailsCacheImpl;
import org.ethereum.jsontestsuite.model.AccountTck;

import java.util.HashMap;
import java.util.Map;

public class RepositoryBuilder {

    public static Repository build(Map<String, AccountTck> accounts){
        HashMap<UscAddress, AccountState> stateBatch = new HashMap<>();
        HashMap<UscAddress, ContractDetails> detailsBatch = new HashMap<>();
        HashMapDB store = new HashMapDB();
        TrieStorePoolOnMemory pool = new TrieStorePoolOnMemory(() -> store);

        for (String address : accounts.keySet()) {
            UscAddress addr = new UscAddress(address);

            AccountTck accountTCK = accounts.get(address);
            AccountBuilder.StateWrap stateWrap = AccountBuilder.build(accountTCK, store);

            AccountState state = stateWrap.getAccountState();
            ContractDetails details = stateWrap.getContractDetails();

            stateBatch.put(addr, state);

            ContractDetailsCacheImpl detailsCache = new ContractDetailsCacheImpl(details);
            detailsCache.setDirty(true);

            detailsBatch.put(addr, detailsCache);
        }

        final TestSystemProperties testSystemProperties = new TestSystemProperties();
        RepositoryImpl repositoryDummy = new RepositoryImpl(new TrieStoreImpl(store), pool, testSystemProperties.detailsInMemoryStorageLimit());
        Repository track = repositoryDummy.startTracking();
        track.updateBatch(stateBatch, detailsBatch);
        track.commit();

        return repositoryDummy;
    }
}
