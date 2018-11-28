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

package co.usc.db;

import co.usc.config.TestSystemProperties;
import co.usc.config.UscSystemProperties;
import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.crypto.Keccak256;
import co.usc.trie.TrieImplHashTest;
import co.usc.trie.TrieStore;
import co.usc.trie.TrieStoreImpl;
import org.ethereum.core.AccountState;
import org.ethereum.core.Repository;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.vm.DataWord;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * Created by ajlopez on 29/03/2017.
 */
public class RepositoryImplTest {
    private static Keccak256 emptyHash = TrieImplHashTest.makeEmptyHash();
    private final TestSystemProperties config = new TestSystemProperties();

    @Test
    public void getNonceUnknownAccount() {
        RepositoryImpl repository = createRepositoryImpl(config);
        BigInteger nonce = repository.getNonce(randomAccountAddress());

        Assert.assertEquals(BigInteger.ZERO, nonce);
    }

    @Test
    public void isNotClosedWhenCreated() {
        RepositoryImpl repository = createRepositoryImpl(config);

        Assert.assertFalse(repository.isClosed());
    }

    @Test
    public void hasEmptyHashAsRootWhenCreated() {
        RepositoryImpl repository = createRepositoryImpl(config);

        Assert.assertArrayEquals(emptyHash.getBytes(), repository.getRoot());
    }

    @Test
    public void createAccount() {
        RepositoryImpl repository = createRepositoryImpl(config);

        AccountState accState = repository.createAccount(randomAccountAddress());

        Assert.assertNotNull(accState);
        Assert.assertEquals(BigInteger.ZERO, accState.getNonce());
        Assert.assertEquals(BigInteger.ZERO, accState.getBalance().asBigInteger());

        Assert.assertFalse(Arrays.equals(emptyHash.getBytes(), repository.getRoot()));
    }

    @Test
    public void syncToRootAfterCreatingAnAccount() {
        TrieStore store = new TrieStoreImpl(new HashMapDB());
        RepositoryImpl repository = new RepositoryImpl(store, new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());

        repository.flush();

        UscAddress accAddress = randomAccountAddress();
        byte[] initialRoot = repository.getRoot();

        repository.createAccount(accAddress);
        repository.flush();

        byte[] newRoot = repository.getRoot();

        Assert.assertTrue(repository.isExist(accAddress));

        repository.syncToRoot(initialRoot);

        Assert.assertFalse(repository.isExist(accAddress));

        repository.syncToRoot(newRoot);

        Assert.assertTrue(repository.isExist(accAddress));
    }

    @Test
    public void updateAccountState() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        AccountState accState = repository.createAccount(accAddress);

        accState.incrementNonce();
        accState.addToBalance(Coin.valueOf(1L));

        repository.updateAccountState(accAddress, accState);

        AccountState newAccState = repository.getAccountState(accAddress);
        Assert.assertNotNull(newAccState);
        Assert.assertEquals(BigInteger.ONE, newAccState.getNonce());
        Assert.assertEquals(BigInteger.ONE, newAccState.getBalance().asBigInteger());
    }

    @Test
    public void incrementAccountNonceForNewAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.increaseNonce(accAddress);

        Assert.assertEquals(BigInteger.ONE, repository.getNonce(accAddress));
    }

    @Test
    public void incrementAccountNonceForAlreadyCreatedAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);
        repository.increaseNonce(accAddress);

        Assert.assertEquals(BigInteger.ONE, repository.getNonce(accAddress));
    }

    @Test
    public void incrementAccountNonceTwiceForAlreadyCreatedAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);
        repository.increaseNonce(accAddress);
        repository.increaseNonce(accAddress);

        Assert.assertEquals(2, repository.getNonce(accAddress).longValue());
    }

    @Test
    public void incrementAccountBalanceForNewAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        Assert.assertEquals(BigInteger.ONE, repository.addBalance(accAddress, Coin.valueOf(1L)).asBigInteger());

        Assert.assertEquals(BigInteger.ONE, repository.getBalance(accAddress).asBigInteger());
    }

    @Test
    public void incrementAccountBalanceForAlreadyCreatedAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);
        Assert.assertEquals(BigInteger.ONE, repository.addBalance(accAddress, Coin.valueOf(1L)).asBigInteger());

        Assert.assertEquals(BigInteger.ONE, repository.getBalance(accAddress).asBigInteger());
    }

    @Test
    public void incrementAccountBalanceTwiceForAlreadyCreatedAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);
        Assert.assertEquals(BigInteger.ONE, repository.addBalance(accAddress, Coin.valueOf(1L)).asBigInteger());
        Assert.assertEquals(2, repository.addBalance(accAddress, Coin.valueOf(1L)).asBigInteger().longValue());

        Assert.assertEquals(2, repository.getBalance(accAddress).asBigInteger().longValue());
    }

    @Test
    public void isExistReturnsFalseForUnknownAccount() {
        RepositoryImpl repository = createRepositoryImpl(config);

        Assert.assertFalse(repository.isExist(randomAccountAddress()));
    }

    @Test
    public void isExistReturnsTrueForCreatedAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);

        Assert.assertTrue(repository.isExist(accAddress));
    }

    @Test
    public void getCodeFromUnknownAccount() {
        RepositoryImpl repository = createRepositoryImpl(config);

        byte[] code = repository.getCode(randomAccountAddress());

        Assert.assertNotNull(code);
        Assert.assertEquals(0, code.length);
    }

    @Test
    public void getCodeFromAccountWithoutCode() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);

        byte[] code = repository.getCode(accAddress);

        Assert.assertNotNull(code);
        Assert.assertEquals(0, code.length);
    }

    @Test
    public void saveAndGetCodeFromAccount() {
        UscAddress accAddress = randomAccountAddress();
        byte[] accCode = new byte[] { 0x01, 0x02, 0x03 };

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);

        repository.saveCode(accAddress, accCode);

        byte[] code = repository.getCode(accAddress);

        Assert.assertNotNull(code);
        Assert.assertArrayEquals(accCode, code);
    }

    @Test
    public void hibernateAccount() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);
        repository.hibernate(accAddress);

        AccountState accState = repository.getAccountState(accAddress);

        Assert.assertNotNull(accState);
        Assert.assertTrue(accState.isHibernated());
    }

    @Test
    public void getCodeFromHibernatedAccount() {
        UscAddress accAddress = randomAccountAddress();
        byte[] accCode = new byte[] { 0x01, 0x02, 0x03 };

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);

        repository.saveCode(accAddress, accCode);
        repository.hibernate(accAddress);

        byte[] code = repository.getCode(accAddress);

        Assert.assertNotNull(code);
        Assert.assertEquals(0, code.length);
    }

    @Test
    public void startTracking() {
        RepositoryImpl repository = createRepositoryImpl(config);

        Repository track = repository.startTracking();

        Assert.assertNotNull(track);
    }

    @Test
    public void createAccountInTrackAndCommit() {
        UscAddress accAddress = randomAccountAddress();
        RepositoryImpl repository = createRepositoryImpl(config);

        Repository track = repository.startTracking();

        Assert.assertNotNull(track);
        track.createAccount(accAddress);
        track.commit();

        Assert.assertTrue(repository.isExist(accAddress));
    }

    @Test
    public void createAccountInTrackAndRollback() {
        UscAddress accAddress = randomAccountAddress();
        RepositoryImpl repository = createRepositoryImpl(config);

        Repository track = repository.startTracking();

        Assert.assertNotNull(track);
        track.createAccount(accAddress);
        track.rollback();

        Assert.assertFalse(repository.isExist(accAddress));
    }

    @Test
    public void getEmptyStorageValue() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress);
        DataWord value = repository.getStorageValue(accAddress, DataWord.ONE);

        Assert.assertNull(value);
    }

    @Test
    public void setAndGetStorageValue() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.addStorageRow(accAddress, DataWord.ONE, DataWord.ONE);

        DataWord value = repository.getStorageValue(accAddress, DataWord.ONE);

        // Account state points to previous state, use track to update values
        Assert.assertNull(value);
    }

    @Test
    public void setAndGetStorageValueUsingNewRepositoryForTest() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = new RepositoryImplForTesting();

        repository.addStorageRow(accAddress, DataWord.ONE, DataWord.ONE);

        DataWord value = repository.getStorageValue(accAddress, DataWord.ONE);

        Assert.assertNotNull(value);
        Assert.assertEquals(DataWord.ONE, value);
    }

    @Test
    public void setAndGetStorageValueUsingTrack() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        Repository track = repository.startTracking();

        track.addStorageRow(accAddress, DataWord.ONE, DataWord.ONE);
        track.commit();

        DataWord value = repository.getStorageValue(accAddress, DataWord.ONE);

        Assert.assertNotNull(value);
        Assert.assertEquals(DataWord.ONE, value);
    }

    @Test
    public void getEmptyStorageBytes() {
        UscAddress accAddress = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        byte[] bytes = repository.getStorageBytes(accAddress, DataWord.ONE);

        Assert.assertNull(bytes);
    }

    @Test
    public void setAndGetStorageBytesUsingTrack() {
        UscAddress accAddress = randomAccountAddress();
        byte[] bytes = new byte[] { 0x01, 0x02, 0x03 };

        RepositoryImpl repository = createRepositoryImpl(config);

        Repository track = repository.startTracking();
        track.addStorageBytes(accAddress, DataWord.ONE, bytes);
        track.commit();

        byte[] savedBytes = repository.getStorageBytes(accAddress, DataWord.ONE);

        Assert.assertNotNull(savedBytes);
        Assert.assertArrayEquals(bytes, savedBytes);
    }

    @Test
    public void emptyAccountsKeysOnNonExistentAccount()
    {
        RepositoryImpl repository = createRepositoryImpl(config);

        Set<UscAddress> keys = repository.getAccountsKeys();

        Assert.assertNotNull(keys);
        Assert.assertTrue(keys.isEmpty());
    }

    @Test
    public void getAccountsKeys()
    {
        UscAddress accAddress1 = randomAccountAddress();
        UscAddress accAddress2 = randomAccountAddress();

        RepositoryImpl repository = createRepositoryImpl(config);

        repository.createAccount(accAddress1);
        repository.createAccount(accAddress2);

        Set<UscAddress> keys = repository.getAccountsKeys();

        Assert.assertNotNull(keys);
        Assert.assertFalse(keys.isEmpty());
        Assert.assertEquals(2, keys.size());
    }

    @Test
    public void getAccountsKeysOnSnapshot()
    {
        UscAddress accAddress1 = randomAccountAddress();
        UscAddress accAddress2 = randomAccountAddress();

        TrieStore store = new TrieStoreImpl(new HashMapDB());
        RepositoryImpl repository = new RepositoryImpl(store, new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());

        repository.createAccount(accAddress1);
        repository.flush();

        byte[] root = repository.getRoot();

        repository.createAccount(accAddress2);

        repository.syncToRoot(root);

        Set<UscAddress> keys = repository.getAccountsKeys();

        Assert.assertNotNull(keys);
        Assert.assertFalse(keys.isEmpty());
        Assert.assertEquals(1, keys.size());
    }

    @Test
    public void getDetailsDataStore() {
        RepositoryImpl repository = createRepositoryImpl(config);

        Assert.assertNotNull(repository.getDetailsDataStore());
    }

    @Test
    public void flushNoReconnect() {
        TrieStore store = new TrieStoreImpl(new HashMapDB());
        RepositoryImpl repository = new RepositoryImpl(store, new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());

        UscAddress accAddress = randomAccountAddress();
        byte[] initialRoot = repository.getRoot();

        repository.createAccount(accAddress);
        repository.flushNoReconnect();

        Assert.assertTrue(repository.isExist(accAddress));
    }

    private static UscAddress randomAccountAddress() {
        byte[] bytes = new byte[20];

        new Random().nextBytes(bytes);

        return new UscAddress(bytes);
    }

    public static RepositoryImpl createRepositoryImpl(UscSystemProperties config) {
        return new RepositoryImpl(null, new TrieStorePoolOnMemory(), config.detailsInMemoryStorageLimit());
    }
}
