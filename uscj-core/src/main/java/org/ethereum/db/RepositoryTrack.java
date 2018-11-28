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

import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.db.ContractDetailsImpl;
import co.usc.trie.TrieStore;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.datasource.HashMapDB;
import org.ethereum.vm.DataWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.ethereum.crypto.Keccak256Helper.keccak256;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

/**
 * @author Roman Mandeleil
 * @since 17.11.2014
 */
public class RepositoryTrack implements Repository {
    private static final byte[] EMPTY_DATA_HASH = HashUtil.keccak256(EMPTY_BYTE_ARRAY);
    private static final Logger logger = LoggerFactory.getLogger("repository");

    private final Map<UscAddress, AccountState> cacheAccounts = new HashMap<>();
    private final Map<UscAddress, ContractDetails> cacheDetails = new HashMap<>();

    private final DetailsDataStore dds;
    private final Repository repository;
    private final TrieStore.Pool trieStorePool;
    private final int memoryStorageLimit;

    public RepositoryTrack(Repository repository, TrieStore.Pool trieStorePool, int memoryStorageLimit) {
        this.repository = repository;
        this.trieStorePool = trieStorePool;
        this.memoryStorageLimit = memoryStorageLimit;
        this.dds = new DetailsDataStore(new DatabaseImpl(new HashMapDB()), trieStorePool, memoryStorageLimit);
    }

    @Override
    public AccountState createAccount(UscAddress addr) {

        synchronized (repository) {
            logger.trace("createAccount: [{}]", addr);

            AccountState accountState = new AccountState();
            cacheAccounts.put(addr, accountState);

            ContractDetails contractDetails = new ContractDetailsCacheImpl(null);
            contractDetails.setDirty(true);
            cacheDetails.put(addr, contractDetails);

            return accountState;
        }
    }

    @Override
    public AccountState getAccountState(UscAddress addr) {

        synchronized (repository) {

            AccountState accountState = cacheAccounts.get(addr);

            if (accountState == null) {
                repository.loadAccount(addr, cacheAccounts, cacheDetails);

                accountState = cacheAccounts.get(addr);
            }
            return accountState;
        }
    }

    @Override
    public boolean isExist(UscAddress addr) {

        synchronized (repository) {
            AccountState accountState = cacheAccounts.get(addr);
            if (accountState != null) {
                return !accountState.isDeleted();
            }

            return repository.isExist(addr);
        }
    }

    @Override
    public ContractDetails getContractDetails(UscAddress addr) {

        synchronized (repository) {
            ContractDetails contractDetails = cacheDetails.get(addr);

            if (contractDetails == null) {
                repository.loadAccount(addr, cacheAccounts, cacheDetails);
                contractDetails = cacheDetails.get(addr);
            }

            return contractDetails;
        }
    }

    @Override
    public void loadAccount(UscAddress addr, Map<UscAddress, AccountState> cacheAccounts,
                            Map<UscAddress, ContractDetails> cacheDetails) {

        synchronized (repository) {
            AccountState accountState = this.cacheAccounts.get(addr);
            ContractDetails contractDetails = this.cacheDetails.get(addr);

            if (accountState == null) {
                repository.loadAccount(addr, this.cacheAccounts, this.cacheDetails);
                accountState = this.cacheAccounts.get(addr);
                contractDetails = this.cacheDetails.get(addr);
            }

            cacheAccounts.put(addr, accountState.clone());
            ContractDetails contractDetailsLvl2 = new ContractDetailsCacheImpl(contractDetails);
            cacheDetails.put(addr, contractDetailsLvl2);
        }
    }


    @Override
    public void delete(UscAddress addr) {
        logger.trace("delete account: [{}]", addr);

        synchronized (repository) {
            getAccountState(addr).setDeleted(true);
            getContractDetails(addr).setDeleted(true);
        }
    }

    @Override
    public BigInteger increaseNonce(UscAddress addr) {

        synchronized (repository) {
            AccountState accountState = getAccountState(addr);

            if (accountState == null) {
                accountState = createAccount(addr);
            }

            getContractDetails(addr).setDirty(true);

            BigInteger saveNonce = accountState.getNonce();
            accountState.incrementNonce();

            logger.trace("increase nonce addr: [{}], from: [{}], to: [{}]", addr,
                    saveNonce, accountState.getNonce());

            return accountState.getNonce();
        }
    }

    @Override
    public void hibernate(UscAddress addr) {

        synchronized (repository) {
            AccountState accountState = getAccountState(addr);

            if (accountState == null) {
                accountState = createAccount(addr);
            }

            getContractDetails(addr).setDirty(true);

            accountState.hibernate();
        }
        logger.trace("hibernate addr: [{}]", addr);
    }

    public BigInteger setNonce(UscAddress addr, BigInteger bigInteger) {
        synchronized (repository) {
            AccountState accountState = getAccountState(addr);

            if (accountState == null) {
                accountState = createAccount(addr);
            }

            getContractDetails(addr).setDirty(true);

            BigInteger saveNonce = accountState.getNonce();
            accountState.setNonce(bigInteger);

            logger.trace("increase nonce addr: [{}], from: [{}], to: [{}]", addr,
                    saveNonce, accountState.getNonce());

            return accountState.getNonce();
        }
    }


    @Override
    public BigInteger getNonce(UscAddress addr) {
        AccountState accountState = getAccountState(addr);
        return accountState == null ? new AccountState().getNonce() : accountState.getNonce();
    }

    @Override
    public Coin getBalance(UscAddress addr) {
        AccountState accountState = getAccountState(addr);
        return accountState == null ? new AccountState().getBalance() : accountState.getBalance();
    }

    @Override
    public Coin addBalance(UscAddress addr, Coin value) {

        synchronized (repository) {
            AccountState accountState = getAccountState(addr);
            if (accountState == null) {
                accountState = createAccount(addr);
            }

            getContractDetails(addr).setDirty(true);
            Coin newBalance = accountState.addToBalance(value);

            logger.trace("adding to balance addr: [{}], balance: [{}], delta: [{}]", addr,
                    newBalance, value);

            return newBalance;
        }
    }

    @Override
    public void saveCode(UscAddress addr, byte[] code) {
        logger.trace("saving code addr: [{}], code: [{}]", addr,
                Hex.toHexString(code));
        synchronized (repository) {
            getContractDetails(addr).setCode(code);
            getContractDetails(addr).setDirty(true);
            getAccountState(addr).setCodeHash(Keccak256Helper.keccak256(code));
        }
    }

    @Override
    public byte[] getCode(UscAddress addr) {

        synchronized (repository) {
            if (!isExist(addr)) {
                return EMPTY_BYTE_ARRAY;
            }

            byte[] codeHash = getAccountState(addr).getCodeHash();
            if (Arrays.equals(codeHash, EMPTY_DATA_HASH)) {
                return EMPTY_BYTE_ARRAY;
            }

            return getContractDetails(addr).getCode();
        }
    }

    @Override
    public void addStorageRow(UscAddress addr, DataWord key, DataWord value) {

        logger.trace("add storage row, addr: [{}], key: [{}] val: [{}]", addr,
                key.toString(), value.toString());

        synchronized (repository) {
            getContractDetails(addr).put(key, value);
        }
    }

    @Override
    public void addStorageBytes(UscAddress addr, DataWord key, byte[] value) {

        logger.trace("add storage bytes, addr: [{}], key: [{}]", addr,
                key.toString());

        synchronized (repository) {
            getContractDetails(addr).putBytes(key, value);
        }
    }

    @Override
    public DataWord getStorageValue(UscAddress addr, DataWord key) {
        synchronized (repository) {
            return getContractDetails(addr).get(key);
        }
    }

    @Override
    public byte[] getStorageBytes(UscAddress addr, DataWord key) {
        synchronized (repository) {
            return getContractDetails(addr).getBytes(key);
        }
    }

    @Override
    public Set<UscAddress> getAccountsKeys() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository startTracking() {
        logger.debug("start tracking");

        return new RepositoryTrack(this, trieStorePool, memoryStorageLimit);
    }


    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushNoReconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {

        synchronized (repository) {
            applyCacheDetailsChanges();

            repository.updateBatch(cacheAccounts, cacheDetails);
            cacheAccounts.clear();
            cacheDetails.clear();
            logger.debug("committed changes");
        }
    }

    public void applyCacheDetailsChanges(){
        synchronized (repository) {
            for (ContractDetails contractDetails : cacheDetails.values()) {

                ContractDetailsCacheImpl contractDetailsCache = (ContractDetailsCacheImpl) contractDetails;
                contractDetailsCache.commit();
            }
        }
    }

    @Override
    public void syncToRoot(byte[] root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() {
        logger.debug("rollback changes");

        cacheAccounts.clear();
        cacheDetails.clear();
    }

    public void dumpChanges() {
        HashMap<UscAddress, AccountState> accountStates = new HashMap<>();
        HashMap<UscAddress, ContractDetails> contractDetails= new HashMap<>();
        updateBatch(accountStates,contractDetails);

        StringBuilder buf = new StringBuilder();
        buf.append("accountStates:\n");
        for (HashMap.Entry<UscAddress, AccountState> entry : accountStates.entrySet()) {
            buf.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }

        buf.append("contractDetails:\n");
        for (HashMap.Entry<UscAddress, ContractDetails> entry : contractDetails.entrySet()) {
            buf.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }

        logger.debug(buf.toString());
    }

    @Override
    public void updateBatch(Map<UscAddress, AccountState> accountStates,
                            Map<UscAddress, ContractDetails> contractDetails) {

        synchronized (repository) {
            for (Map.Entry<UscAddress, AccountState> entry : accountStates.entrySet()) {
                cacheAccounts.put(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<UscAddress, ContractDetails> entry : contractDetails.entrySet()) {

                ContractDetailsCacheImpl contractDetailsCache = (ContractDetailsCacheImpl) entry.getValue();
                if (    contractDetailsCache.origContract != null
                        && !(contractDetailsCache.origContract instanceof ContractDetailsImpl)) {
                    cacheDetails.put(entry.getKey(), contractDetailsCache.origContract);
                } else {
                    cacheDetails.put(entry.getKey(), contractDetailsCache);
                }
            }
        }
    }

    @Override // that's the idea track is here not for root calculations
    public byte[] getRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        throw new UnsupportedOperationException();
    }

    public Repository getOriginRepository() {
        return (repository instanceof RepositoryTrack)
                ? ((RepositoryTrack) repository).getOriginRepository()
                : repository;
    }

    @Override
    public DetailsDataStore getDetailsDataStore(){
        return dds;
    }

    @Override
    public void updateContractDetails(
            UscAddress addr,
            ContractDetails contractDetails) {
        synchronized (repository) {
            logger.trace("updateContractDetails: [{}]", addr);
            ContractDetails contractDetailsCache = new ContractDetailsCacheImpl(null);
            contractDetails.setDirty(true);
            cacheDetails.put(addr, contractDetailsCache);
        }
    }

    @Override
    public void updateAccountState(UscAddress addr, AccountState accountState) {
        synchronized (repository) {
            logger.trace("updateAccountState: [{}]", addr);
            cacheAccounts.put(addr, accountState);
        }
    }
}
