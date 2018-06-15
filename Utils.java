/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.ulordj.core.Base58;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.core.Sha256Hash;
import co.usc.ulordj.core.UldECKey;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

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

    public static UldECKey convertWifToPrivateKey(String key, UldECKey pubKey, NetworkParameters params) {
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
        String payloadUnlockAcc = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"personal_unlockAccount\", " +
                "\"params\": [" +
                "\"" + address + "\"," +
                "\"" + pwd + "\",\"\"" +
                "], " +
                "\"id\": \"1\"" +
                "}";

        StringEntity entityUnlockAcc = new StringEntity(payloadUnlockAcc,
                ContentType.APPLICATION_JSON);

        HttpClient httpClientUnlockAcc = HttpClientBuilder.create().build();
        HttpPost requestUnlockAcc = new HttpPost(NetworkConstants.POST_URI);
        requestUnlockAcc.setEntity(entityUnlockAcc);

        HttpResponse responseUnlockAcc = httpClientUnlockAcc.execute(requestUnlockAcc);
        JSONObject response = new JSONObject(EntityUtils.toString(responseUnlockAcc.getEntity()));

        if(response.get("result").toString().equals("true"))
            return true;
        return false;
    }

    public static boolean isTransactionInMemPool(String txId) throws IOException {
        String payload = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_getTransactionByHash\", " +
                "\"params\": [" +
                "\"" + txId + "\"]," +
                "\"id\": \"1\"" +
                "}";

        StringEntity entity = new StringEntity(payload,
                ContentType.APPLICATION_JSON);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(NetworkConstants.POST_URI);
        request.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());
            JSONObject jsonObj = new JSONObject(responseString);
            String result = jsonObj.get("result").toString();
            if(result.equals("null"))
                return false;
            return true;
        } catch(Exception ex){
            System.out.println(ex);
            return false;
        }
    }

    public static boolean isTransactionMined(String txId) throws IOException {
        String payload = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_getTransactionByHash\", " +
                "\"params\": [" +
                "\"" + txId + "\"]," +
                "\"id\": \"1\"" +
                "}";

        StringEntity entity = new StringEntity(payload,
                ContentType.APPLICATION_JSON);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(NetworkConstants.POST_URI);
        request.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());
            JSONObject jsonObj = new JSONObject(responseString);
            String result = jsonObj.getJSONObject("result").get("blockNumber").toString();

            if(!result.equals("null"))
                return true;

            return false;
        }catch(Exception ex){
            System.out.println(ex);
            return false;
        }
    }
}
