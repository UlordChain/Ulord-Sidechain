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

package co.usc.core;

import co.usc.config.UscSystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionExecutor;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ReceiptStore;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.vm.PrecompiledContracts;
import org.ethereum.vm.program.ProgramResult;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the logic to execute a transaction in an
 * isolated environment (e.g. no persistent state changes).
 */
@Component
public class ReversibleTransactionExecutor {

    private final UscSystemProperties config;
    private final Repository track;
    private final BlockStore blockStore;
    private final ReceiptStore receiptStore;
    private final ProgramInvokeFactory programInvokeFactory;

    @Autowired
    public ReversibleTransactionExecutor(
            UscSystemProperties config,
            Repository track,
            BlockStore blockStore,
            ReceiptStore receiptStore,
            ProgramInvokeFactory programInvokeFactory) {
        this.config = config;
        this.track = track;
        this.blockStore = blockStore;
        this.receiptStore = receiptStore;
        this.programInvokeFactory = programInvokeFactory;
    }

    public ProgramResult executeTransaction(
            Block executionBlock,
            UscAddress coinbase,
            byte[] gasPrice,
            byte[] gasLimit,
            byte[] toAddress,
            byte[] value,
            byte[] data,
            UscAddress fromAddress) {
        Repository repository = track.getSnapshotTo(executionBlock.getStateRoot()).startTracking();

        byte[] nonce = repository.getNonce(fromAddress).toByteArray();
        UnsignedTransaction tx = new UnsignedTransaction(
                nonce,
                gasPrice,
                gasLimit,
                toAddress,
                value,
                data,
                fromAddress
        );

        TransactionExecutor executor = new TransactionExecutor(
                tx, 0, coinbase, repository, blockStore, receiptStore,
                programInvokeFactory, executionBlock, new EthereumListenerAdapter(), 0, config.getVmConfig(),
                config.getBlockchainConfig(), config.playVM(), config.isRemascEnabled(), config.vmTrace(), new PrecompiledContracts(config),
                config.databaseDir(), config.vmTraceDir(), config.vmTraceCompressed()
        ).setLocalCall(true);

        executor.init();
        executor.execute();
        executor.go();
        executor.finalization();
        return executor.getResult();
    }

    private static class UnsignedTransaction extends Transaction {

        private UnsignedTransaction(
                byte[] nonce,
                byte[] gasPrice,
                byte[] gasLimit,
                byte[] receiveAddress,
                byte[] value,
                byte[] data,
                UscAddress fromAddress) {
            super(nonce, gasPrice, gasLimit, receiveAddress, value, data);
            this.sender = fromAddress;
        }

        @Override
        public boolean acceptTransactionSignature(byte chainId) {
            // We only allow executing unsigned transactions
            // in the context of a reversible transaction execution.
            return true;
        }
    }
}
