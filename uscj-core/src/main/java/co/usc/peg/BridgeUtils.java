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
import co.usc.ulordj.script.Script;
import co.usc.ulordj.store.BlockStoreException;
import co.usc.ulordj.wallet.Wallet;
import co.usc.config.BridgeConstants;
import co.usc.core.UscAddress;
import co.usc.peg.ulord.UscAllowUnconfirmedCoinSelector;
import co.usc.util.MaxSizeHashMap;
import org.ethereum.config.BlockchainNetConfig;
import org.ethereum.core.Transaction;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Oscar Guindzberg
 */
public class BridgeUtils {

    private static final Logger logger = LoggerFactory.getLogger("BridgeUtils");

    // power of 2 size that contains enough hashes to handle one year of hashes
    private static final int MAX_MAP_PARENTS_SIZE = 65535;
    private static Map<Sha256Hash, Sha256Hash> parentMap = new MaxSizeHashMap<>(MAX_MAP_PARENTS_SIZE, false);

    public static StoredBlock getStoredBlockAtHeight(UldBlockstoreWithCache blockStore, int height) throws BlockStoreException {
        StoredBlock storedBlock = blockStore.getChainHead();
        Sha256Hash blockHash = storedBlock.getHeader().getHash();

        int headHeight = storedBlock.getHeight();

        if (height > headHeight) {
            return null;
        }

        for (int i = 0; i < (headHeight - height); i++) {
            if (blockHash == null) {
                return null;
            }

            Sha256Hash prevBlockHash = parentMap.get(blockHash);

            if (prevBlockHash == null) {
                StoredBlock currentBlock = blockStore.getFromCache(blockHash);

                if (currentBlock == null) {
                    return null;
                }

                prevBlockHash = currentBlock.getHeader().getPrevBlockHash();
                parentMap.put(blockHash, prevBlockHash);
            }

            blockHash = prevBlockHash;
        }

        if (blockHash == null) {
            return null;
        }

        storedBlock = blockStore.getFromCache(blockHash);

        if (storedBlock != null) {
            if (storedBlock.getHeight() != height) {
                throw new IllegalStateException("Block height is " + storedBlock.getHeight() + " but should be " + headHeight);
            }
            return storedBlock;
        } else {
            return null;
        }
    }

    public static Wallet getFederationNoSpendWallet(Context uldContext, Federation federation) {
        return getFederationsNoSpendWallet(uldContext, Arrays.asList(federation));
    }

    public static Wallet getFederationsNoSpendWallet(Context uldContext, List<Federation> federations) {
        Wallet wallet = new BridgeUldWallet(uldContext, federations);
        federations.forEach(federation -> wallet.addWatchedAddress(federation.getAddress(), federation.getCreationTime().toEpochMilli()));
        return wallet;
    }

    public static Wallet getFederationSpendWallet(Context uldContext, Federation federation, List<UTXO> utxos) {
        return getFederationsSpendWallet(uldContext, Arrays.asList(federation), utxos);
    }

    public static Wallet getFederationsSpendWallet(Context uldContext, List<Federation> federations, List<UTXO> utxos) {
        Wallet wallet = new BridgeUldWallet(uldContext, federations);

        UscUTXOProvider utxoProvider = new UscUTXOProvider(uldContext.getParams(), utxos);
        wallet.setUTXOProvider(utxoProvider);
        federations.stream().forEach(federation -> {
            wallet.addWatchedAddress(federation.getAddress(), federation.getCreationTime().toEpochMilli());
        });
        wallet.setCoinSelector(new UscAllowUnconfirmedCoinSelector());
        return wallet;
    }

    private static boolean scriptCorrectlySpendsTx(UldTransaction tx, int index, Script script) {
        try {
            TransactionInput txInput = tx.getInput(index);
            txInput.getScriptSig().correctlySpends(tx, index, script, Script.ALL_VERIFY_FLAGS);
            return true;
        } catch (ScriptException se) {
            return false;
        }
    }

    /**
     * Indicates whether a tx is a valid lock tx or not, checking the first input's script sig
     * @param tx
     * @return
     */
    public static boolean isValidLockTx(UldTransaction tx) {
        if (tx.getInputs().size() == 0) {
            return false;
        }
        // This indicates that the tx is a P2PKH transaction which is the only one we support for now
        return tx.getInput(0).getScriptSig().getChunks().size() == 2;
    }

    /**
     * Will return a valid scriptsig for the first input
     * @param tx
     * @return
     */
    public static Optional<Script> getFirstInputScriptSig(UldTransaction tx) {
        if (!isValidLockTx(tx)) {
            return Optional.empty();
        }
        return Optional.of(tx.getInput(0).getScriptSig());
    }

    /**
     * It checks if the tx doesn't spend any of the federations' funds and if it sends more than
     * the minimum ({@see BridgeConstants::getMinimumLockTxValue}) to any of the federations
     * @param tx the BTC transaction to check
     * @param federations the active federations
     * @param btcContext the BTC Context
     * @param bridgeConstants the Bridge constants
     * @return true if this is a valid lock transaction
     */
    public static boolean isLockTx(UldTransaction tx, List<Federation> federations, Context uldContext, BridgeConstants bridgeConstants) {
        // First, check tx is not a typical release tx (tx spending from the any of the federation addresses and
        // optionally sending some change to any of the federation addresses)
        for (int i = 0; i < tx.getInputs().size(); i++) {
            final int index = i;
            if (federations.stream().anyMatch(federation -> scriptCorrectlySpendsTx(tx, index, federation.getP2SHScript()))) {
                return false;
            }
        }

        Wallet federationsWallet = BridgeUtils.getFederationsNoSpendWallet(uldContext, federations);
        Coin valueSentToMe = tx.getValueSentToMe(federationsWallet);

        int valueSentToMeSignum = valueSentToMe.signum();
        if (valueSentToMe.isLessThan(bridgeConstants.getMinimumLockTxValue())) {
            logger.warn("[uldtx:{}]Someone sent to the federation less than {} satoshis", tx.getHash(), bridgeConstants.getMinimumLockTxValue());
        }
        return (valueSentToMeSignum > 0 && !valueSentToMe.isLessThan(bridgeConstants.getMinimumLockTxValue()));
    }

    public static boolean isLockTx(UldTransaction tx, Federation federation, Context uldContext, BridgeConstants bridgeConstants) {
        return isLockTx(tx, Arrays.asList(federation), uldContext, bridgeConstants);
    }

    private static boolean isReleaseTx(UldTransaction tx, Federation federation) {
        return isReleaseTx(tx, Collections.singletonList(federation));
    }

    public static boolean isReleaseTx(UldTransaction tx, List<Federation> federations) {
        int inputsSize = tx.getInputs().size();
        for (int i = 0; i < inputsSize; i++) {
            final int inputIndex = i;
            if (federations.stream().map(Federation::getP2SHScript).anyMatch(federationPayScript -> scriptCorrectlySpendsTx(tx, inputIndex, federationPayScript))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMigrationTx(UldTransaction uldTx, Federation activeFederation, Federation retiringFederation, Context uldContext, BridgeConstants bridgeConstants) {
        if (retiringFederation == null) {
            return false;
        }
        boolean moveFromRetiring = isReleaseTx(uldTx, retiringFederation);
        boolean moveToActive = isLockTx(uldTx, activeFederation, uldContext, bridgeConstants);

        return moveFromRetiring && moveToActive;
    }

    public static Address recoverUldAddressFromEthTransaction(org.ethereum.core.Transaction tx, NetworkParameters networkParameters) {
        org.ethereum.crypto.ECKey key = tx.getKey();
        byte[] pubKey = key.getPubKey(true);
        return UldECKey.fromPublicOnly(pubKey).toAddress(networkParameters);
    }

    public static boolean isFreeBridgeTx(Transaction uscTx, long blockNumber, BlockchainNetConfig netConfig) {
        UscAddress receiveAddress = uscTx.getReceiveAddress();
        if (receiveAddress.equals(UscAddress.nullAddress())) {
            return false;
        }

        BridgeConstants bridgeConstants = netConfig.getCommonConstants().getBridgeConstants();

        // Temporary assumption: if areBridgeTxsFree() is true then the current federation
        // must be the genesis federation.
        // Once the original federation changes, txs are always paid.
        return PrecompiledContracts.BRIDGE_ADDR.equals(receiveAddress) &&
               netConfig.getConfigForBlock(blockNumber).areBridgeTxsFree() &&
               uscTx.acceptTransactionSignature(netConfig.getCommonConstants().getChainId()) &&
               (
                       isFromFederateMember(uscTx, bridgeConstants.getGenesisFederation()) ||
                       isFromFederationChangeAuthorizedSender(uscTx, bridgeConstants) ||
                       isFromLockWhitelistChangeAuthorizedSender(uscTx, bridgeConstants) ||
                       isFromFeePerKbChangeAuthorizedSender(uscTx, bridgeConstants)
               );
    }

    /**
     * Indicates if the provided tx was generated from a contract
     * @param uscTx
     * @return
     */
    public static boolean isContractTx(Transaction uscTx) {
        // TODO: this should be refactored to provide a more robust way of checking the transaction origin
        return uscTx.getClass() == org.ethereum.vm.program.InternalTransaction.class;
    }

    public static boolean isFromFederateMember(org.ethereum.core.Transaction uscTx, Federation federation) {
        return federation.hasMemberWithUscAddress(uscTx.getSender().getBytes());
    }

    private static boolean isFromFederationChangeAuthorizedSender(org.ethereum.core.Transaction uscTx, BridgeConstants bridgeConfiguration) {
        AddressBasedAuthorizer authorizer = bridgeConfiguration.getFederationChangeAuthorizer();
        return authorizer.isAuthorized(uscTx);
    }

    private static boolean isFromLockWhitelistChangeAuthorizedSender(org.ethereum.core.Transaction uscTx, BridgeConstants bridgeConfiguration) {
        AddressBasedAuthorizer authorizer = bridgeConfiguration.getLockWhitelistChangeAuthorizer();
        return authorizer.isAuthorized(uscTx);
    }

    private static boolean isFromFeePerKbChangeAuthorizedSender(org.ethereum.core.Transaction uscTx, BridgeConstants bridgeConfiguration) {
        AddressBasedAuthorizer authorizer = bridgeConfiguration.getFeePerKbChangeAuthorizer();
        return authorizer.isAuthorized(uscTx);
    }
}
