/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeMainNetConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.UscAddress;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.Bridge;
import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;
import com.typesafe.config.Config;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;

import static tools.Utils.getMinimumGasPrice;

public class WhitelistUlordAddress {

    private static Logger logger = LoggerFactory.getLogger("Federation");
    private static NetworkParameters params;
    private static String lockWhitelistChangeAddress;
    private static String lockWhitelistChangePassword;
    private static BridgeConstants bridgeConstants;

    public static void main(String[] args) {

        FederationConfigLoader configLoader = new FederationConfigLoader();
        Config config = configLoader.getConfigFromFiles();

        String paramName = config.getString("blockchain.config.name");
        if(paramName.equals("testnet")) {
            bridgeConstants = BridgeTestNetConstants.getInstance();
            params = TestNet3Params.get();
        }else if(paramName.equals("regtest")) {
            bridgeConstants = BridgeRegTestConstants.getInstance();
            params = RegTestParams.get();
        }else {
            bridgeConstants = BridgeMainNetConstants.getInstance();
            params = MainNetParams.get();
        }

        lockWhitelistChangeAddress = config.getString("federation.lockWhitelistChangeAddress");
        lockWhitelistChangePassword = config.getString("federation.lockWhitelistChangePassword");

        if(args.length < 1) {
            help();
            return;
        }

        String response = "";
        switch (args[0]) {
            case "addWhitelistAddress":
                if(args.length < 3) {
                    System.out.println("addWhitelistAddress <UlordAddress> <Amount in Satoshi>");
                    return;
                }
                response = addWhitelistAddress(args[1], args[2]);
                break;
            case "removeWhitelistAddress":
                if(args.length < 2) {
                    System.out.println("removeWhitelistAddress <UlordAddress>");
                    return;
                }
                response = removeWhitelistAddress(args[1]);
                break;
            case "listWhitelistAddress":
                response = listWhitelistAddress();
                break;
            case "setWhitelistDisableBlockDelay":
                if(args.length < 2) {
                    System.out.println("setWhitelistDisableBlockDelay <nBlocks>");
                    return;
                }
                response = setWhitelistDisableBlocksDelay(args[1]);
                break;
            default:
                help();
                break;
        }

        if(response.isEmpty()) {
            response = null;
        }
        System.out.println(response);
    }

    private static String setWhitelistDisableBlocksDelay(String nBlock) {
        try {
            if(sendTx(lockWhitelistChangeAddress, DataEncoder.encodeSetLockWhitelistDisableBlockDelay(new BigInteger(nBlock)), 3))
                return "Whitelist disable blocks delay successfully set";
            else
                return "Whitelist disable blocks delay failed";

        } catch (Exception e) {
            return  "Exception in setWhitelistDisableBlocksDelay " + e;
        }
    }

    private static String listWhitelistAddress() {
        try {
            String callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_LOCK_WHITELIST_SIZE.encodeSignature()));
            JSONObject callResponseJson = new JSONObject(callResponse);
            long whitelistSize = Long.parseLong(callResponseJson.get("result").toString().substring(2), 16);

            String addresses = "";
            for (int i = 0; i < whitelistSize; i++) {
                callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_LOCK_WHITELIST_ADDRESS.encode(new Object[]{i})));
                callResponseJson = new JSONObject(callResponse);
                String result = callResponseJson.get("result").toString().substring(2);
                Object[] objects = Bridge.GET_LOCK_WHITELIST_ADDRESS.decodeResult(Hex.decode(result));

                addresses += objects[0].toString() + "\n";
            }
            return addresses;
        } catch (Exception ex) {
            return "Exception ins listWhitelistAddress" + ex;
        }
    }

    private static String removeWhitelistAddress(String addressToRemove) {
        try {
            // Try to unlock account
            if(!Utils.tryUnlockUscAccount(lockWhitelistChangeAddress, lockWhitelistChangePassword))
                throw new PrivateKeyNotFoundException();

            if(sendTx(lockWhitelistChangeAddress, DataEncoder.encodeRemoveLockWhitelistAddress(addressToRemove), 3)) {
                return "Successfully to removed " +  addressToRemove +" from whitelist";
            } else {
                return "Failed to remove " +  addressToRemove +" from whitelist";
            }
        } catch (Exception e) {
            return "Exception in removeWhitelistAddress: " + e;
        }
    }

    private static String addWhitelistAddress(String addressToWhitelist, String amountInSatoshi) {
        Address utAddress = new Address(params, addressToWhitelist);
        UscAddress whitelistAuthorisedAddress = new UscAddress(lockWhitelistChangeAddress);

        boolean isWhitelisted = false;
        if(whitelistAddress(bridgeConstants, whitelistAuthorisedAddress, lockWhitelistChangePassword, utAddress, Coin.valueOf(Long.valueOf(amountInSatoshi)))) {
            // Confirm if the whitelist address is stored in the blockchain
            try {
                String callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_LOCK_WHITELIST_SIZE.encodeSignature()));
                JSONObject callResponseJson = new JSONObject(callResponse);
                long whitelistSize = Long.parseLong(callResponseJson.get("result").toString().substring(2), 16);

                for (int i = 0; i < whitelistSize; i++) {
                    callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_LOCK_WHITELIST_ADDRESS.encode(new Object[]{i})));
                    callResponseJson = new JSONObject(callResponse);
                    String result = callResponseJson.get("result").toString().substring(2);
                    Object[] objects = Bridge.GET_LOCK_WHITELIST_ADDRESS.decodeResult(Hex.decode(result));

                    String address = objects[0].toString();
                    if(utAddress.toString().equals(address)) {
                        isWhitelisted = true;
                        break;
                    }
                }
            } catch (Exception e) {
                return "Exception in addWhitelistAddress: " + e;
            }
        }

        if (isWhitelisted)
            return "Successfully whitelisted";
        else
            return "Whitelist failed";
    }

    private static void help() {
        System.out.println("<function name> <param1> <param2> ...");
        System.out.println("Whitelist Management:" + "\n"
                + "------------------------------------------------------------------------------------------------------------------------------------" + "\n"
                + "   Functions:                            Parameters" + "\n"
                + "addWhitelistAddress                      <UlordAddress> <Amount in Satoshi>" + "\n"
                + "removeWhitelistAddress                   <UlordAddress>" + "\n"
                + "setWhitelistDisableBlockDelay            <nBlocks>" + "\n"
                + "------------------------------------------------------------------------------------------------------------------------------------" + "\n"
        );
    }

    public static boolean whitelistAddress(BridgeConstants bridgeConstants, UscAddress whitelistAuthorisedAddress, String pwd, Address utAddress, Coin valueInSatoshi) {
        return whitelistAddress(bridgeConstants, whitelistAuthorisedAddress.toString(), pwd, utAddress.toString(), BigInteger.valueOf(valueInSatoshi.value));
    }

    public static boolean whitelistAddress(BridgeConstants bridgeConstants, String whitelistAuthorisedAddress, String pwd, String utAddress, BigInteger valueInSatoshi) {
        try {
            AddressBasedAuthorizer lockWhitelistChangeAuthorizer = bridgeConstants.getLockWhitelistChangeAuthorizer();
            if (!lockWhitelistChangeAuthorizer.isAuthorized(new UscAddress(whitelistAuthorisedAddress)))
                return false;

            // Try to unlock account
            if(!Utils.tryUnlockUscAccount(whitelistAuthorisedAddress, pwd))
                throw new PrivateKeyNotFoundException();

            String data = DataEncoder.encodeWhitelist(utAddress, valueInSatoshi);

            return sendTx(whitelistAuthorisedAddress, data, 3);

        } catch (Exception e) {
            logger.error(e.toString());
            System.out.println("WhitelistUlordAddress: " + e);
            return false;
        }
    }

    private static boolean sendTx(String whitelistAuthorisedAddress, String data, int tries) throws IOException, InterruptedException {

        if (tries <= 0)
            return false;

        String sendTransactionResponse = UscRpc.sendTransaction(
                whitelistAuthorisedAddress,
                PrecompiledContracts.BRIDGE_ADDR_STR,
                "0x0",
                getMinimumGasPrice(),
                null,
                data,
                null);
        logger.info(sendTransactionResponse);
        JSONObject jsonObject = new JSONObject(sendTransactionResponse);
        String txId = jsonObject.get("result").toString();

        System.out.println("Whitelist tx id: " + txId);

        Thread.sleep(1000 * 15);

        while (!Utils.isTransactionMined(txId)) {
            Thread.sleep(1000 * 15); // Sleep to stop flooding rpc requests.
            if (!Utils.isTransactionMined(txId)) // Check again because the transaction might have been mined after 15 seconds
                if (!Utils.isTransactionInMemPool(txId))
                    if(!sendTx(whitelistAuthorisedAddress, data, --tries))
                        return false;
        }
        return true;
    }
}
