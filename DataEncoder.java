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

    public static String encodeStateForUldReleaseClient() {
        return Sha256Hash.bytesToHex(Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT.encode(new Object[]{}));
    }

    public static String encodeUldBlockChainBestChainHeight() {
        return Sha256Hash.bytesToHex(Bridge.GET_ULD_BLOCKCHAIN_BEST_CHAIN_HEIGHT.encode(new Object[]{}));
    }

    public static String encodeUpdateCollections() {
        return Sha256Hash.bytesToHex(Bridge.UPDATE_COLLECTIONS.encode(new Object[]{}));
    }

    public static String encodeRemoveLockWhitelistAddress(String address) {
        return Sha256Hash.bytesToHex(Bridge.REMOVE_LOCK_WHITELIST_ADDRESS.encode(new Object[]{address}));
    }

    public static String encodeReceiveHeaders(String[] headers) {
        byte[][] blocks = new byte[headers.length][140];
        for(int i = 0; i < headers.length; ++i) {
            byte[] b = Sha256Hash.hexStringToByteArray(headers[i]);
            blocks[i] = b;
        }
        return Sha256Hash.bytesToHex((Bridge.RECEIVE_HEADERS.encode(new Object[]{blocks})));
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
}

