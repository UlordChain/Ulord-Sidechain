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

package org.ethereum.db;

import co.usc.config.TestSystemProperties;
import co.usc.core.UscAddress;
import co.usc.db.ContractDetailsImpl;
import co.usc.db.TrieStorePoolOnMemory;
import co.usc.trie.TrieImpl;
import co.usc.trie.TrieStore;
import co.usc.trie.TrieStoreImpl;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.vm.DataWord;
import org.junit.Test;
import org.bouncycastle.util.encoders.Hex;

import static org.ethereum.TestUtils.randomAddress;
import static org.ethereum.core.AccountState.EMPTY_DATA_HASH;
import static org.ethereum.util.ByteUtil.toHexString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DetailsDataStoreTest {

    private final TestSystemProperties config = new TestSystemProperties();

    @Test
    public void test1(){
        DatabaseImpl db = new DatabaseImpl(new HashMapDB());
        TrieStorePoolOnMemory trieStorePool = new TrieStorePoolOnMemory();
        DetailsDataStore dds = new DetailsDataStore(db, trieStorePool, config.detailsInMemoryStorageLimit());

        UscAddress c_key = new UscAddress("0000000000000000000000000000000000001a2b");
        byte[] code = Hex.decode("60606060");
        byte[] key =  Hex.decode("11");
        byte[] value =  Hex.decode("aa");

        byte[] contractAddress = randomAddress().getBytes();
        String storeName = "details-storage/" + toHexString(contractAddress);
        ContractDetails contractDetails = new ContractDetailsImpl(
                contractAddress,
                new TrieImpl(trieStorePool.getInstanceFor(storeName), true),
                null,
                trieStorePool,
                config.detailsInMemoryStorageLimit()
        );
        contractDetails.setCode(code);
        contractDetails.put(new DataWord(key), new DataWord(value));

        dds.update(c_key, contractDetails);

        ContractDetails contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);

        String encoded1 = Hex.toHexString(contractDetails.getEncoded());
        String encoded2 = Hex.toHexString(contractDetails_.getEncoded());

        assertEquals(encoded1, encoded2);

        dds.flush();

        contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);
        encoded2 = Hex.toHexString(contractDetails_.getEncoded());
        assertEquals(encoded1, encoded2);
    }

    @Test
    public void test2(){

        DatabaseImpl db = new DatabaseImpl(new HashMapDB());
        TrieStore.Pool trieStorePool = new TrieStorePoolOnMemory();
        DetailsDataStore dds = new DetailsDataStore(db, trieStorePool, config.detailsInMemoryStorageLimit());

        UscAddress c_key = new UscAddress("0000000000000000000000000000000000001a2b");
        byte[] code = Hex.decode("60606060");
        byte[] key =  Hex.decode("11");
        byte[] value =  Hex.decode("aa");

        byte[] contractAddress = randomAddress().getBytes();
        String storeName = "details-storage/" + toHexString(contractAddress);
        ContractDetails contractDetails = new ContractDetailsImpl(
                null,
                new TrieImpl(new TrieStoreImpl(new HashMapDB()), true),
                null,
                trieStorePool,
                config.detailsInMemoryStorageLimit()
        );
        contractDetails.setCode(code);
        contractDetails.put(new DataWord(key), new DataWord(value));

        dds.update(c_key, contractDetails);

        ContractDetails contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);

        String encoded1 = Hex.toHexString(contractDetails.getEncoded());
        String encoded2 = Hex.toHexString(contractDetails_.getEncoded());

        assertEquals(encoded1, encoded2);

        dds.remove(c_key);

        contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);
        assertNull(contractDetails_);

        dds.flush();

        contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);
        assertNull(contractDetails_);
    }

    @Test
    public void test3(){

        HashMapDB store = new HashMapDB();
        DatabaseImpl db = new DatabaseImpl(new HashMapDB());
        TrieStore.Pool trieStorePool = new TrieStorePoolOnMemory(() -> store);
        DetailsDataStore dds = new DetailsDataStore(db, trieStorePool, config.detailsInMemoryStorageLimit());

        UscAddress c_key = new UscAddress("0000000000000000000000000000000000001a2b");
        byte[] code = Hex.decode("60606060");
        byte[] key =  Hex.decode("11");
        byte[] value =  Hex.decode("aa");

        byte[] contractAddress = randomAddress().getBytes();
        String storeName = "details-storage/" + toHexString(contractAddress);
        ContractDetails contractDetails = new ContractDetailsImpl(
                contractAddress,
                new TrieImpl(new TrieStoreImpl(store), true),
                null,
                trieStorePool,
                config.detailsInMemoryStorageLimit()
        );
        contractDetails.setCode(code);
        contractDetails.put(new DataWord(key), new DataWord(value));

        dds.update(c_key, contractDetails);

        ContractDetails contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);

        String encoded1 = Hex.toHexString(contractDetails.getEncoded());
        String encoded2 = Hex.toHexString(contractDetails_.getEncoded());

        assertEquals(encoded1, encoded2);

        dds.remove(c_key);
        dds.update(c_key, contractDetails);

        contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);
        encoded2 = Hex.toHexString(contractDetails_.getEncoded());
        assertEquals(encoded1, encoded2);

        dds.flush();

        contractDetails_ = dds.get(c_key, EMPTY_DATA_HASH);
        encoded2 = Hex.toHexString(contractDetails_.getEncoded());
        assertEquals(encoded1, encoded2);
    }

    @Test
    public void test4() {

        DatabaseImpl db = new DatabaseImpl(new HashMapDB());
        TrieStore.Pool trieStorePool = new TrieStorePoolOnMemory();
        DetailsDataStore dds = new DetailsDataStore(db, trieStorePool, config.detailsInMemoryStorageLimit());

        UscAddress c_key = new UscAddress("0000000000000000000000000000000000001a2b");

        ContractDetails contractDetails = dds.get(c_key, EMPTY_DATA_HASH);
        assertNull(contractDetails);
    }
}
