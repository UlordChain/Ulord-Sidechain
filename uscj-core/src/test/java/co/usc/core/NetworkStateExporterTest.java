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

package co.usc.core;

import co.usc.config.TestSystemProperties;
import co.usc.db.RepositoryImpl;
import co.usc.db.TrieStorePoolOnMemory;
import co.usc.trie.TrieImpl;
import co.usc.trie.TrieStore;
import co.usc.trie.TrieStoreImpl;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.ethereum.core.AccountState;
import org.ethereum.core.Repository;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.db.ContractDetails;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oscar on 13/01/2017.
 */
public class NetworkStateExporterTest {
    private static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};

    static String jsonFileName = "networkStateExporterTest.json";
    private TestSystemProperties config;

    @Before
    public void setup(){
        config = new TestSystemProperties();
    }

    @AfterClass
    public static void cleanup(){
        FileUtils.deleteQuietly(new File(jsonFileName));
    }

    @Test
    public void testEmptyRepo() throws Exception {
        Repository repository = new RepositoryImpl(new TrieStoreImpl(new HashMapDB()), new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());

        Map result = writeAndReadJson(repository);

        Assert.assertEquals(0, result.keySet().size());
    }

    @Test
    public void testNoContracts() throws Exception {
        Repository repository = new RepositoryImpl(new TrieStoreImpl(new HashMapDB()), new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());
        String address1String = "1000000000000000000000000000000000000000";
        UscAddress addr1 = new UscAddress(address1String);
        repository.createAccount(addr1);
        repository.addBalance(addr1, Coin.valueOf(1L));
        repository.increaseNonce(addr1);
        String address2String = "2000000000000000000000000000000000000000";
        UscAddress addr2 = new UscAddress(address2String);
        repository.createAccount(addr2);
        repository.addBalance(addr2, Coin.valueOf(10L));
        repository.increaseNonce(addr2);
        repository.increaseNonce(addr2);

        UscAddress remascSender = UscAddress.nullAddress();
        repository.createAccount(remascSender);
        repository.increaseNonce(remascSender);

        repository.createAccount(PrecompiledContracts.REMASC_ADDR);
        repository.addBalance(PrecompiledContracts.REMASC_ADDR, Coin.valueOf(10L));
        repository.increaseNonce(PrecompiledContracts.REMASC_ADDR);


        Map result = writeAndReadJson(repository);
        Assert.assertEquals(3, result.keySet().size());

        Map address1Value = (Map) result.get(address1String);
        Assert.assertEquals(2, address1Value.keySet().size());
        Assert.assertEquals("1",address1Value.get("balance"));
        Assert.assertEquals("1",address1Value.get("nonce"));

        Map address2Value = (Map) result.get(address2String);
        Assert.assertEquals(2, address2Value.keySet().size());
        Assert.assertEquals("10",address2Value.get("balance"));
        Assert.assertEquals("2",address2Value.get("nonce"));

        Map remascValue = (Map) result.get(PrecompiledContracts.REMASC_ADDR_STR);
        Assert.assertEquals(2, remascValue.keySet().size());
        Assert.assertEquals("10",remascValue.get("balance"));
        Assert.assertEquals("1",remascValue.get("nonce"));
    }

    @Test
    public void testContracts() throws Exception {
        TrieStore.Pool trieStorePool = new TrieStorePoolOnMemory();
        Repository repository = new RepositoryImpl(new TrieStoreImpl(new HashMapDB()), trieStorePool, config.detailsInMemoryStorageLimit());
        String address1String = "1000000000000000000000000000000000000000";
        UscAddress addr1 = new UscAddress(address1String);
        repository.createAccount(addr1);
        repository.addBalance(addr1, Coin.valueOf(1L));
        repository.increaseNonce(addr1);
        ContractDetails contractDetails = new co.usc.db.ContractDetailsImpl(
            null,
            new TrieImpl(new TrieStoreImpl(new HashMapDB()), true),
            null,
                trieStorePool,
            config.detailsInMemoryStorageLimit()
        );
        contractDetails.setCode(new byte[] {1, 2, 3, 4});
        contractDetails.put(DataWord.ZERO, DataWord.ONE);
        contractDetails.putBytes(DataWord.ONE, new byte[] {5, 6, 7, 8});
        repository.updateContractDetails(addr1, contractDetails);
        AccountState accountState = repository.getAccountState(addr1);
        accountState.setStateRoot(contractDetails.getStorageHash());
        repository.updateAccountState(addr1, accountState);

        Map result = writeAndReadJson(repository);

        Assert.assertEquals(1, result.keySet().size());
        Map address1Value = (Map) result.get(address1String);
        Assert.assertEquals(3, address1Value.keySet().size());
        Assert.assertEquals("1",address1Value.get("balance"));
        Assert.assertEquals("1",address1Value.get("nonce"));
        Map contract = (Map) address1Value.get("contract");
        Assert.assertEquals(2, contract.keySet().size());
        Assert.assertEquals("01020304",contract.get("code"));
        Map data = (Map) contract.get("data");
        Assert.assertEquals(2, data.keySet().size());
        Assert.assertEquals("01", data.get(Hex.toHexString(DataWord.ZERO.getData())));
        Assert.assertEquals("05060708", data.get(Hex.toHexString(DataWord.ONE.getData())));
    }


    private Map writeAndReadJson(Repository repository) throws Exception {
        NetworkStateExporter nse = new NetworkStateExporter(repository);
        Assert.assertTrue(nse.exportStatus(jsonFileName));

        InputStream inputStream = new FileInputStream(jsonFileName);
        String json = new String(ByteStreams.toByteArray(inputStream));

        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructType(HashMap.class);
        Map result = new ObjectMapper().readValue(json, type);
        return result;
    }

}
