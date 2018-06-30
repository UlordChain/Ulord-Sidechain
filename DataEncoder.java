/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.peg.Bridge;
import co.usc.ulordj.core.Sha256Hash;
import com.sun.istack.internal.NotNull;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class DataEncoder {

    public static String encodeUpdateCollections() {
        return Sha256Hash.bytesToHex(Bridge.UPDATE_COLLECTIONS.encodeSignature());
    }

    public static String encodeReceiveHeaders(String[] headers) {
        byte[][] blocks = new byte[headers.length][140];
        for(int i = 0; i < headers.length; ++i) {
            byte[] b = Sha256Hash.hexStringToByteArray(headers[i]);
            blocks[i] = b;
        }
        return Hex.toHexString(Bridge.RECEIVE_HEADERS.encode(new Object[]{blocks}));
    }

    public static String encodeStateForUldReleaseClient() {
        return Hex.toHexString(Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT.encode(new Object[]{}));
    }

    public static String encodeUldBlockChainBestChainHeight() {
        return Hex.toHexString(Bridge.GET_ULD_BLOCKCHAIN_BEST_CHAIN_HEIGHT.encode(new Object[]{}));
    }

    public static String encodeRemoveLockWhitelistAddress(String address) {
        return Hex.toHexString(Bridge.REMOVE_LOCK_WHITELIST_ADDRESS.encode(new Object[]{address}));
    }



    /**
     * Encodes addLockWhitelistAddress function with its parameters
     * @param address to whitelist
     * @param valueInSatoshi the max amount allowed to transfer
     * @return encoded string of function signature and its parameters
     */
    public static String encodeWhitelist(String address, BigInteger valueInSatoshi) {
        return Hex.toHexString(Bridge.ADD_LOCK_WHITELIST_ADDRESS.encode(new Object[] {address, valueInSatoshi}));
    }

    public static String encodeRegisterUlordTransaction(byte[] utTx, int height, byte[] merkleTree) {
        return Hex.toHexString(Bridge.REGISTER_ULD_TRANSACTION.encode(new Object[]{utTx, height, merkleTree}));
    }

    public static String encodeIsUldTxHashAlreadyProcessed(String txId) {
        return Hex.toHexString(Bridge.IS_ULD_TX_HASH_ALREADY_PROCESSED.encode(new Object[]{txId}));
    }

    public static String encodeGetUldTxHashProcessedHeight(String txId) {
        return Hex.toHexString(Bridge.GET_ULD_TX_HASH_PROCESSED_HEIGHT.encode(new Object[]{txId}));
    }

    public static String encodeSetLockWhitelistDisableBlockDelay(BigInteger value) {
        return Hex.toHexString(Bridge.SET_LOCK_WHITELIST_DISABLE_BLOCK_DELAY.encode(new Object[]{value}));
    }


    // Federation Functions ---------------------------------------------------
    public static String encodeGetFederationAddress() {
        return Hex.toHexString(Bridge.GET_FEDERATION_ADDRESS.encodeSignature());
    }

    public static String encodeGetFederationSize() {
        return Hex.toHexString(Bridge.GET_FEDERATION_SIZE.encodeSignature());
    }

    public static String encodeGetFederationThreshold() {
        return Hex.toHexString(Bridge.GET_FEDERATION_THRESHOLD.encodeSignature());
    }

    public static String encodeGetFederatorPublicKey(int index) {
        return Hex.toHexString(Bridge.GET_FEDERATOR_PUBLIC_KEY.encode(new Object[] {index}));
    }

    public static String encodeGetFederationCreationTime() {
        return Hex.toHexString(Bridge.GET_FEDERATION_CREATION_TIME.encodeSignature());
    }

    public static String encodeGetFederationCreationBlockNumber() {
        return Hex.toHexString(Bridge.GET_FEDERATION_CREATION_BLOCK_NUMBER.encodeSignature());
    }




    public static String encodeGetRetiringFederationAddress() {
        return Hex.toHexString(Bridge.GET_RETIRING_FEDERATION_ADDRESS.encodeSignature());
    }

    public static String encodeGetRetiringFederationSize() {
        return Hex.toHexString(Bridge.GET_RETIRING_FEDERATION_SIZE.encodeSignature());
    }

    public static String encodeGetRetiringFederationThreshold() {
        return Hex.toHexString(Bridge.GET_RETIRING_FEDERATION_THRESHOLD.encodeSignature());
    }

    public static String encodeGetRetiringFederatorPublicKey(int index) {
        return Hex.toHexString(Bridge.GET_RETIRING_FEDERATOR_PUBLIC_KEY.encode(new Object[] {index}));
    }

    public static String encodeGetRetiringFederationCreationTime() {
        return Hex.toHexString(Bridge.GET_RETIRING_FEDERATION_CREATION_TIME.encodeSignature());
    }

    public static String encodeGetRetiringFederationCreationBlockNumber() {
        return Hex.toHexString(Bridge.GET_RETIRING_FEDERATION_CREATION_BLOCK_NUMBER.encodeSignature());
    }




    public static String encodeCreateFederation() {
        return Hex.toHexString(Bridge.CREATE_FEDERATION.encodeSignature());
    }

    public static String encodeAddFederatorPublicKey(String publicKey) {
        return Hex.toHexString(Bridge.ADD_FEDERATOR_PUBLIC_KEY.encode(new Object[]{Hex.decode(publicKey)}));
    }

    public static String encodeCommitFederation(String hash) {
        return Hex.toHexString(Bridge.COMMIT_FEDERATION.encode(new Object[]{Hex.decode(hash)}));
    }

    public static String encodeRollbackFederation() {
        return Hex.toHexString(Bridge.ROLLBACK_FEDERATION.encodeSignature());
    }




    public static String encodeGetPendingFederationHash() {
        return Hex.toHexString(Bridge.GET_PENDING_FEDERATION_HASH.encodeSignature());
    }

    public static String encodeGetPendingFederationSize() {
        return Hex.toHexString(Bridge.GET_PENDING_FEDERATION_SIZE.encodeSignature());
    }

    public static String encodeGetPendingFederatorPublicKey(int index) {
        return Hex.toHexString(Bridge.GET_PENDING_FEDERATOR_PUBLIC_KEY.encode(new Object[]{index}));
    }
}

















