/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.UscAddress;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.Bridge;
import co.usc.ulordj.core.Address;
import co.usc.ulordj.core.Coin;
import co.usc.ulordj.params.TestNet3Params;
import com.typesafe.config.Config;
import org.ethereum.util.RLPList;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;

public class WhitelistUlordAddress {

    private static Logger logger = LoggerFactory.getLogger("whitelistaddress");

    public static void main(String[] args) {

        if(args.length < 2) {
            System.out.println("<Ulord address to whitelist> <Max value in Satoshi>");
        }

        FederationConfigLoader configLoader = new FederationConfigLoader();
        Config config = configLoader.getConfigFromFiles();

        BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
        String lockWhitelistChangeAddress = config.getString("federation.lockWhitelistChangeAddress");
        String lockWhitelistChangePassword = config.getString("federation.lockWhitelistChangePassword");

        Address utAddress = new Address(TestNet3Params.get(), args[0]);
        UscAddress whitelistAuthorisedAddress = new UscAddress(lockWhitelistChangeAddress);

        boolean isWhitelisted = false;
        if(whitelistAddress(bridgeConstants, whitelistAuthorisedAddress, lockWhitelistChangePassword, utAddress, Coin.valueOf(Long.valueOf(args[1])))) {
            // Confirm if the whitelist address is stored in the blockchain
            try {
                String callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_LOCK_WHITELIST_SIZE.encodeSignature()));
                JSONObject callResponseJson = new JSONObject(callResponse);
                long whitelistSize = Long.parseLong(callResponseJson.get("result").toString().substring(2));

                for (int i = 0; i < whitelistSize; i++) {
                    callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(Bridge.GET_LOCK_WHITELIST_ADDRESS.encode(new Object[]{i})));
                    callResponseJson = new JSONObject(callResponse);
                    String result = callResponseJson.get("result").toString().substring(2);
                    Object[] objects = Bridge.GET_LOCK_WHITELIST_ADDRESS.decodeResult(Hex.decode(result));

                    String address = objects[0].toString();
                    System.out.println(address);
                    //RLPList rlpElements =
                    if(utAddress.toString().equals(address)) {
                        isWhitelisted = true;
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        if (isWhitelisted)
            System.out.println("Successfully whitelisted");
        else
            System.out.println("Whitelist failed");
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

        // Get gasPrice
        JSONObject getGasPriceJSON = new JSONObject(UscRpc.getBlockByNumber("latest", false));
        String gasPrice = getGasPriceJSON.getJSONObject("result").getString("minimumGasPrice");

        if(gasPrice.equals("0"))
            gasPrice = null;

        String sendTransactionResponse = UscRpc.sendTransaction(whitelistAuthorisedAddress, PrecompiledContracts.BRIDGE_ADDR_STR, null, gasPrice, null, data, null);
        logger.info(sendTransactionResponse);
        JSONObject jsonObject = new JSONObject(sendTransactionResponse);
        String txId = jsonObject.get("result").toString();

        System.out.println("Whitelist tx id: " + txId);

        Thread.sleep(1000 * 15);

        while (!Utils.isTransactionMined(txId)) {
            Thread.sleep(1000 * 15); // Sleep to stop flooding rpc requests.
            if (!Utils.isTransactionInMemPool(txId))
                if(!sendTx(whitelistAuthorisedAddress, data, --tries))
                    return false;
        }
        return true;
    }
}
