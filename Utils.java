/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.ulordj.core.*;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;

public class Utils {

    public static String convertPrivateKeyToWif(String keyHex, NetworkParameters params) {
        StringBuilder privKey = new StringBuilder();
        privKey.append(BigInteger.valueOf(params.getDumpedPrivateKeyHeader()).toString(16));
        privKey.append(keyHex);
        privKey.append("01");
        String hash = Hex.toHexString(Sha256Hash.hashTwice(Hex.decode(privKey.toString())));
        privKey.append(hash.substring(0,8));
        return Base58.encode(Hex.decode(privKey.toString()));
    }

    public static UldECKey convertWifToPrivateKey(String key, NetworkParameters params) {
        String keyHex = Hex.toHexString(Base58.decode(key));
        keyHex = keyHex.substring(0, keyHex.length() - 8);

        int privKeyHeader = new BigInteger(keyHex.substring(0,2), 16).intValue();
        if(privKeyHeader == params.getDumpedPrivateKeyHeader()) {
            keyHex = keyHex.substring(2, keyHex.length());
        }

        int lastValue = new BigInteger(keyHex.substring(keyHex.length() -2, keyHex.length()), 16).intValue();

        if(lastValue == 1)
            keyHex = keyHex.substring(0, keyHex.length() - 2);

        return UldECKey.fromPrivate(new BigInteger(keyHex, 16));
    }

    public static String BufferedReaderToString(BufferedReader br) throws IOException {
        String tempData, data = "";

        while((tempData = br.readLine()) != null)
            data += tempData;
        return data;
    }

    public static boolean tryUnlockUscAccount(String address, String pwd) throws IOException {
        JSONObject response = new JSONObject(UscRpc.unlockAccount(address, pwd));
        if(response.get("result").toString().equals("true"))
            return true;
        return false;
    }

    public static boolean isTransactionInMemPool(String txId) {
        try {
            JSONObject jsonObj = new JSONObject(UscRpc.getTransactionByHash(txId));
            String result = jsonObj.get("result").toString();
            if(result.equals("null"))
                return false;

            return true;
        } catch(Exception ex){
            return false;
        }
    }

    public static boolean isTransactionMined(String txId) {
        try {
            JSONObject jsonObj = new JSONObject(UscRpc.getTransactionByHash(txId));
            String result = jsonObj.getJSONObject("result").get("blockNumber").toString();

            if(result.equals("null"))
                return false;

            return true;
        } catch(Exception ex){
            return false;
        }
    }

    public static int getUscBestBlockHeight(){
        try {
            JSONObject jsonObj = new JSONObject(UscRpc.blockNumber());
            return Integer.decode(jsonObj.get("result").toString());
        }catch(Exception ex){
            System.out.println(ex);
            return 0;
        }
    }

    public static String getMinimumGasPrice() throws IOException {
        JSONObject getGasPriceJSON = new JSONObject(UscRpc.getBlockByNumber("latest", false));
        return getGasPriceJSON.getJSONObject("result").getString("minimumGasPrice");
    }

    public static String getGasForTx(@Nullable String from,
                                     @Nullable String to,
                                     @Nullable String gas,
                                     @Nullable String gasPrice,
                                     @Nullable String value,
                                     @Nullable String data,
                                     @Nullable String nonce) throws IOException {
        JSONObject jsonObject = new JSONObject(UscRpc.estimateGas(from, to, gas, gasPrice, value, data, nonce));
        return jsonObject.getString("result");
    }
}
