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

package co.usc.peg;

import co.usc.config.UscSystemProperties;
import co.usc.config.TestSystemProperties;
import co.usc.db.RepositoryImpl;
import co.usc.db.TrieStorePoolOnMemory;
import co.usc.trie.TrieStoreImpl;
import org.apache.commons.lang3.StringUtils;
import org.ethereum.config.BlockchainConfig;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Repository;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by adrian.eidelman on 3/15/2016.
 */
public class SamplePrecompiledContractTest {

    private final TestSystemProperties config = new TestSystemProperties();
    private final PrecompiledContracts precompiledContracts = new PrecompiledContracts(config);

    private BlockchainConfig getUscIP93ConfigMock(boolean enabled) {
        BlockchainConfig result = mock(BlockchainConfig.class);
        when(result.isUscIP93()).thenReturn(enabled);
        return result;
    }

    @Test
    public void samplePrecompiledContractMethod1Ok()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'param0','type':'int'}, \n" +
                "               {'name':'param1','type':'bytes'}, \n" +
                "               {'name':'param2','type':'int'}], \n" +
                "    'name':'Method1', \n" +
                "   'outputs':[{'name':'output0','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] bytes = new byte[]{(byte) 0xab, (byte) 0xcd, (byte) 0xef};
        byte[] data = function.encode(111, bytes, 222);

        contract.init(null, null, createRepositoryImpl(config), null, null, new ArrayList<LogInfo>());
        byte[] result = contract.execute(data);

        Object[] results = function.decodeResult(result);
        assertEquals(new BigInteger("1"), results[0]);
    }

    @Test
    public void samplePrecompiledContractMethod1WrongData()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'param0','type':'int'}, \n" +
                "               {'name':'param1','type':'bytes'}, \n" +
                "               {'name':'param2','type':'int'}], \n" +
                "    'name':'Method1', \n" +
                "   'outputs':[{'name':'output0','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] data = new byte[]{(byte) 0xab, (byte) 0xcd, (byte) 0xef};

        contract.init(null, null, createRepositoryImpl(config), null, null, new ArrayList<LogInfo>());
        byte[] result = contract.execute(data);

        assertNull(result);
    }

    @Test
    public void samplePrecompiledContractMethodDoesNotExist()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'param0','type':'int'}, \n" +
                "               {'name':'param1','type':'bytes'}, \n" +
                "               {'name':'param2','type':'int'}], \n" +
                "    'name':'UnexistentMethod', \n" +
                "   'outputs':[{'name':'output0','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] bytes = new byte[]{(byte) 0xab, (byte) 0xcd, (byte) 0xef};
        byte[] data = function.encode(111, bytes, 222);

        contract.init(null, null, createRepositoryImpl(config), null, null, new ArrayList<LogInfo>());
        byte[] result = contract.execute(data);

        assertNull(result);
    }

    @Test
    public void samplePrecompiledContractMethod1LargeData()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'param0','type':'int'}, \n" +
                "               {'name':'param1','type':'string'}], \n" +
                "    'name':'Method1', \n" +
                "   'outputs':[{'name':'output0','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] data = function.encode(111, StringUtils.leftPad("foobar", 1000000, '*'));

        contract.init(null, null, createRepositoryImpl(config), null, null, new ArrayList<LogInfo>());
        byte[] result = contract.execute(data);

        Object[] results = function.decodeResult(result);
        assertEquals(new BigInteger("1"), results[0]);
    }

    @Test
    public void samplePrecompiledContractAddBalanceOk()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[], \n" +
                "    'name':'AddBalance', \n" +
                "   'outputs':[], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] data = function.encode();

        Repository repository = createRepositoryImpl(config);
        contract.init(null, null, repository, null, null, new ArrayList<LogInfo>());
        contract.execute(data);

        int balance = this.GetBalance(repository);
        assertEquals(50000, balance);
    }

    @Test
    public void samplePrecompiledContractGetBalanceInitialBalance()
    {
        int balance = this.GetBalance(createRepositoryImpl(config));
        assertEquals(0, balance);
    }

    @Test
    public void samplePrecompiledContractIncrementResultOk()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[], \n" +
                "    'name':'IncrementResult', \n" +
                "   'outputs':[], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] data = function.encode();

        Repository repository = createRepositoryImpl(config);
        Repository track = repository.startTracking();
        contract.init(null, null, track, null, null, new ArrayList<LogInfo>());
        contract.execute(data);
        track.commit();

        int result = this.GetResult(repository);
        assertEquals(1, result);
    }

    @Test
    public void samplePrecompiledContractGetResultInitialValue()
    {
        int result = this.GetResult(createRepositoryImpl(config));
        assertEquals(0, result);
    }

    private int GetBalance(Repository repository)
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[], \n" +
                "    'name':'GetBalance', \n" +
                "   'outputs':[{'name':'balance','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] data = function.encode();

        contract.init(null, null, repository, null, null, new ArrayList<LogInfo>());
        byte[] result = contract.execute(data);

        Object[] results = function.decodeResult(result);

        return ((BigInteger)results[0]).intValue();
    }

    private int GetResult(Repository repository)
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(false), addr);


        String funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[], \n" +
                "    'name':'GetResult', \n" +
                "   'outputs':[{'name':'result','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n";
        funcJson = funcJson.replaceAll("'", "\"");

        CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);

        byte[] data = function.encode();

        contract.init(null, null, repository, null, null, new ArrayList<LogInfo>());
        byte[] result = contract.execute(data);

        Object[] results = function.decodeResult(result);

        return ((BigInteger)results[0]).intValue();
    }

    @Test
    public void samplePrecompiledContractPostUscIP93DoesntExist()
    {
        DataWord addr = new DataWord(PrecompiledContracts.SAMPLE_ADDR.getBytes());
        SamplePrecompiledContract contract = (SamplePrecompiledContract) precompiledContracts.getContractForAddress(getUscIP93ConfigMock(true), addr);

        Assert.assertNull(contract);
    }

    public static RepositoryImpl createRepositoryImpl(UscSystemProperties config) {
        return new RepositoryImpl(null, new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());
    }
}
