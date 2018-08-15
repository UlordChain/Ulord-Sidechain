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

import co.usc.ulordj.core.*;
import co.usc.ulordj.wallet.SendRequest;
import co.usc.ulordj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Given a set of UTXOs, a ReleaseTransactionBuilder
 * knows how to build a release transaction
 * of a certain amount to a certain address,
 * and how to signal the used UTXOs so they
 * can be invalidated.
 *
 * @author Ariel Mendelzon
 */
public class ReleaseTransactionBuilder {
    public class BuildResult {
        private final UldTransaction uldTx;
        private final List<UTXO> selectedUTXOs;

        public BuildResult(UldTransaction uldTx, List<UTXO> selectedUTXOs) {
            this.uldTx = uldTx;
            this.selectedUTXOs = selectedUTXOs;
        }

        public UldTransaction getUldTx() {
            return uldTx;
        }

        public List<UTXO> getSelectedUTXOs() {
            return selectedUTXOs;
        }
    }

    private interface SendRequestConfigurator {
        void configure(SendRequest sr);
    }

    private static final Logger logger = LoggerFactory.getLogger("ReleaseTransactionBuilder");

    private final NetworkParameters params;
    private final Wallet wallet;
    private final Address changeAddress;
    private final Coin feePerKb;

    public ReleaseTransactionBuilder(NetworkParameters params, Wallet wallet, Address changeAddress, Coin feePerKb) {
        this.params = params;
        this.wallet = wallet;
        this.changeAddress = changeAddress;
        this.feePerKb = feePerKb;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Address getChangeAddress() {
        return changeAddress;
    }

    public Coin getFeePerKb() {
        return feePerKb;
    }

    public Optional<BuildResult> buildAmountTo(Address to, Coin amount) {
        return buildWithConfiguration((SendRequest sr) -> {
            sr.tx.addOutput(amount, to);
            sr.changeAddress = changeAddress;
        }, String.format("sending %s to %s", amount, to));
    }

    public Optional<BuildResult> buildEmptyWalletTo(Address to) {
        return buildWithConfiguration((SendRequest sr) -> {
            sr.tx.addOutput(Coin.ZERO, to);
            sr.changeAddress = to;
            sr.emptyWallet = true;
        }, String.format("emptying wallet to %s", to));
    }

    private Optional<BuildResult> buildWithConfiguration(
            SendRequestConfigurator sendRequestConfigurator,
            String operationDescription) {

        // Build a tx and send request and configure it
        UldTransaction uldTx = new UldTransaction(params);
        SendRequest sr = SendRequest.forTx(uldTx);
        // Default settings
        defaultSettingsConfigurator.configure(sr);
        // Specific settings
        sendRequestConfigurator.configure(sr);

        try {
            wallet.completeTx(sr);

            // Disconnect input from output because we don't need the reference and it interferes serialization
            for (TransactionInput transactionInput : uldTx.getInputs()) {
                transactionInput.disconnect();
            }

            List<UTXO> selectedUTXOs = wallet
                .getUTXOProvider().getOpenTransactionOutputs(wallet.getWatchedAddresses()).stream()
                .filter(utxo ->
                    uldTx.getInputs().stream().anyMatch(input ->
                        input.getOutpoint().getHash().equals(utxo.getHash()) &&
                        input.getOutpoint().getIndex() == utxo.getIndex()
                    )
                )
                .collect(Collectors.toList());

            return Optional.of(new BuildResult(uldTx, selectedUTXOs));
        } catch (InsufficientMoneyException e) {
            logger.warn(String.format("Not enough ULD in the wallet to complete %s", operationDescription), e);
            // Comment out panic logging for now
            // panicProcessor.panic("nomoney", "Not enough confirmed ULD in the federation wallet to complete " + uscTxHash + " " + uldTx);
            return Optional.empty();
        } catch (Wallet.CouldNotAdjustDownwards e) {
            logger.warn(String.format("A user output could not be adjusted downwards to pay tx fees %s", operationDescription), e);
            // Comment out panic logging for now
            // panicProcessor.panic("couldnotadjustdownwards", "A user output could not be adjusted downwards to pay tx fees " + uscTxHash + " " + uldTx);
            return Optional.empty();
        } catch (Wallet.ExceededMaxTransactionSize e) {
            logger.warn(String.format("Tx size too big %s", operationDescription), e);
            // Comment out panic logging for now
            // panicProcessor.panic("exceededmaxtransactionsize", "Tx size too big " + uscTxHash + " " + uldTx);
            return Optional.empty();
        } catch (UTXOProviderException e) {
            logger.warn(String.format("UTXO provider exception sending %s", operationDescription), e);
            // Comment out panic logging for now
            // panicProcessor.panic("utxoprovider", "UTXO provider exception " + uscTxHash + " " + uldTx);
            return Optional.empty();
        }
    }

    private final SendRequestConfigurator defaultSettingsConfigurator = (SendRequest sr) -> {
        sr.missingSigsMode = Wallet.MissingSigsMode.USE_OP_ZERO;
        sr.feePerKb = getFeePerKb();
        sr.shuffleOutputs = false;
        sr.recipientsPayFees = true;
    };
}
