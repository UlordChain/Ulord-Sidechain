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

package org.ethereum.core;

import co.usc.core.Coin;
import co.usc.core.UscAddress;
import co.usc.core.bc.AccountInformationProvider;
import org.ethereum.db.ContractDetails;
import org.ethereum.db.DetailsDataStore;
import org.ethereum.vm.DataWord;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

/**
 * @author Roman Mandeleil
 * @since 08.09.2014
 */
public interface Repository extends AccountInformationProvider {

    /**
     * Create a new account in the database
     *
     * @param addr of the contract
     * @return newly created account state
     */
    AccountState createAccount(UscAddress addr);


    /**
     * @param addr - account to check
     * @return - true if account exist,
     *           false otherwise
     */
    boolean isExist(UscAddress addr);

    /**
     * Retrieve an account
     *
     * @param addr of the account
     * @return account state as stored in the database
     */
    AccountState getAccountState(UscAddress addr);

    /**
     * Deletes the account
     *
     * @param addr of the account
     */
    void delete(UscAddress addr);

    /**
     * Hibernates the account
     *
     * @param addr of the account
     */
    void hibernate(UscAddress addr);

        /**
     * Increase the account nonce of the given account by one
     *
     * @param addr of the account
     * @return new value of the nonce
     */
    BigInteger increaseNonce(UscAddress addr);

    /**
     * Retrieve contract details for a given account from the database
     *
     * @param addr of the account
     * @return new contract details
     */
    ContractDetails getContractDetails(UscAddress addr);

    /**
     * Store code associated with an account
     *
     * @param addr for the account
     * @param code that will be associated with this account
     */
    void saveCode(UscAddress addr, byte[] code);

    /**
     * Put a value in storage of an account at a given key
     *
     * @param addr of the account
     * @param key of the data to store
     * @param value is the data to store
     */
    void addStorageRow(UscAddress addr, DataWord key, DataWord value);

    void addStorageBytes(UscAddress addr, DataWord key, byte[] value);

    byte[] getStorageBytes(UscAddress addr, DataWord key);

    /**
     * Add value to the balance of an account
     *
     * @param addr of the account
     * @param value to be added
     * @return new balance of the account
     */
    Coin addBalance(UscAddress addr, Coin value);

    /**
     * @return Returns set of all the account addresses
     */
    Set<UscAddress> getAccountsKeys();

    /**
     * Dump the full state of the current repository into a file with JSON format
     * It contains all the contracts/account, their attributes and
     *
     * @param block of the current state
     * @param gasUsed the amount of gas used in the block until that point
     * @param txNumber is the number of the transaction for which the dump has to be made
     * @param txHash is the hash of the given transaction.
     * If null, the block state post coinbase reward is dumped.
     */
    void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash);

    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker repository
     */
    Repository startTracking();

    void flush();
    void flushNoReconnect();


    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    void commit();

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    void rollback();

    /**
     * Return to one of the previous snapshots
     * by moving the root.
     *
     * @param root - new root
     */
    void syncToRoot(byte[] root);

    /**
     * Check to see if the current repository has an open connection to the database
     *
     * @return <tt>true</tt> if connection to database is open
     */
    boolean isClosed();

    /**
     * Close the database
     */
    void close();

    /**
     * Reset
     */
    void reset();

    void updateBatch(Map<UscAddress, AccountState> accountStates,
                     Map<UscAddress, ContractDetails> contractDetailes);


    byte[] getRoot();

    void loadAccount(UscAddress addr,
                     Map<UscAddress, AccountState> cacheAccounts,
                     Map<UscAddress, ContractDetails> cacheDetails);

    Repository getSnapshotTo(byte[] root);

    DetailsDataStore getDetailsDataStore();

    void updateContractDetails(UscAddress addr, final ContractDetails contractDetails);

    void updateAccountState(UscAddress addr, AccountState accountState);

    default void transfer(UscAddress fromAddr, UscAddress toAddr, Coin value) {
        addBalance(fromAddr, value.negate());
        addBalance(toAddr, value);
    }
}
