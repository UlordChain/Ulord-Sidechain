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

package org.ethereum.vm.program;

import co.usc.core.Coin;
import co.usc.core.UscAddress;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.DetailsDataStore;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.listener.ProgramListener;
import org.ethereum.vm.program.listener.ProgramListenerAware;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

/*
 * A Storage is a proxy class for Repository. It encapsulates a repository providing tracing services.
 * It is only used by Program.
 * It does not provide any other functionality different from tracing.
 */
public class Storage implements Repository, ProgramListenerAware {

    private final Repository repository;
    private final UscAddress addr;
    private ProgramListener traceListener;

    public Storage(ProgramInvoke programInvoke) {
        this.addr = new UscAddress(programInvoke.getOwnerAddress());
        this.repository = programInvoke.getRepository();
    }

    @Override
    public void setTraceListener(ProgramListener listener) {
        this.traceListener = listener;
    }

    @Override
    public AccountState createAccount(UscAddress addr) {
        return repository.createAccount(addr);
    }

    @Override
    public boolean isExist(UscAddress addr) {
        return repository.isExist(addr);
    }

    @Override
    public AccountState getAccountState(UscAddress addr) {
        return repository.getAccountState(addr);
    }

    @Override
    public void delete(UscAddress addr) {
        if (canListenTrace(addr)) {
            traceListener.onStorageClear();
        }
        repository.delete(addr);
    }

    @Override
    public void hibernate(UscAddress addr) {
        repository.hibernate(addr);
    }

    @Override
    public BigInteger increaseNonce(UscAddress addr) {
        return repository.increaseNonce(addr);
    }

    @Override
    public BigInteger getNonce(UscAddress addr) {
        return repository.getNonce(addr);
    }

    @Override
    public ContractDetails getContractDetails(UscAddress addr) {
        return repository.getContractDetails(addr);
    }

    @Override
    public void saveCode(UscAddress addr, byte[] code) {
        repository.saveCode(addr, code);
    }

    @Override
    public byte[] getCode(UscAddress addr) {
        return repository.getCode(addr);
    }

    @Override
    public void addStorageRow(UscAddress addr, DataWord key, DataWord value) {
        if (canListenTrace(addr)) {
            traceListener.onStoragePut(key, value);
        }
        repository.addStorageRow(addr, key, value);
    }

    @Override
    public void addStorageBytes(UscAddress addr, DataWord key, byte[] value) {
        if (canListenTrace(addr)) {
            traceListener.onStoragePut(key, value);
        }
        repository.addStorageBytes(addr, key, value);
    }

    private boolean canListenTrace(UscAddress addr) {
        return this.addr.equals(addr) && traceListener != null;
    }

    @Override
    public DataWord getStorageValue(UscAddress addr, DataWord key) {
        return repository.getStorageValue(addr, key);
    }

    @Override
    public byte[] getStorageBytes(UscAddress addr, DataWord key) {
        return repository.getStorageBytes(addr, key);
    }

    @Override
    public Coin getBalance(UscAddress addr) {
        return repository.getBalance(addr);
    }

    @Override
    public Coin addBalance(UscAddress addr, Coin value) {
        return repository.addBalance(addr, value);
    }

    @Override
    public Set<UscAddress> getAccountsKeys() {
        return repository.getAccountsKeys();
    }

    @Override
    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {
        repository.dumpState(block, gasUsed, txNumber, txHash);
    }

    @Override
    public Repository startTracking() {
        return repository.startTracking();
    }

    @Override
    public void flush() {
        repository.flush();
    }

    @Override
    public void flushNoReconnect() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void commit() {
        repository.commit();
    }

    @Override
    public void rollback() {
        repository.rollback();
    }

    @Override
    public void syncToRoot(byte[] root) {
        repository.syncToRoot(root);
    }

    @Override
    public boolean isClosed() {
        return repository.isClosed();
    }

    @Override
    public void close() {
        repository.close();
    }

    @Override
    public void reset() {
        repository.reset();
    }

    @Override
    public void updateBatch(Map<UscAddress, AccountState> accountStates, Map<UscAddress, ContractDetails> contractDetails) {
        for (UscAddress addr : contractDetails.keySet()) {
            if (!canListenTrace(addr)) {
                return;
            }

            ContractDetails details = contractDetails.get(addr);
            if (details.isDeleted()) {
                traceListener.onStorageClear();
            } else if (details.isDirty()) {
                for (Map.Entry<DataWord, DataWord> entry : details.getStorage().entrySet()) {
                    traceListener.onStoragePut(entry.getKey(), entry.getValue());
                }
            }
        }
        repository.updateBatch(accountStates, contractDetails);
    }

    @Override
    public byte[] getRoot() {
        return repository.getRoot();
    }

    @Override
    public void loadAccount(UscAddress addr, Map<UscAddress, AccountState> cacheAccounts, Map<UscAddress, ContractDetails> cacheDetails) {
        repository.loadAccount(addr, cacheAccounts, cacheDetails);
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DetailsDataStore getDetailsDataStore() {
        return this.repository.getDetailsDataStore();
    }

    @Override
    public void updateContractDetails(UscAddress addr, ContractDetails contractDetails) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAccountState(UscAddress addr, AccountState accountState) {
        throw new UnsupportedOperationException();
    }


}
