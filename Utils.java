/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.ulordj.core.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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

    public static boolean isTransactionInMemPool(String txId) {
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

    public static boolean isTransactionMined(String txId) {
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

    public static boolean sendTransaction(String from,
                                           @Nullable String to,
                                           @Nullable String gas,
                                           @Nullable String gasPrice,
                                           @Nullable String value,
                                           @Nullable String data,
                                           @Nullable String nonce,
                                           int tries)
            throws IOException, InterruptedException {

        if(tries < 0)
            return false;

        StringBuilder call = new StringBuilder();
        call.append("{");
        call.append("\"jsonrpc\":\"2.0\",   ");

        if(to != null)
            call.append("\"id\":\"1\",      ");

        call.append("\"method\":\"eth_sendTransaction\",    ");
        call.append("\"params\":[{"     );
        call.append("\"from\":\"" + from + "\",    ");

        if(to != null)
            call.append("\"to\":\"" + to + "\",     ");

        if(gas != null)
            call.append("\"gas\":\"" + gas + "\",     ");

        if(gasPrice != null)
            call.append("\"gasPrice\":\"" + gasPrice +"\",      ");

        if(value != null)
            call.append("\"value\":\"" + value + "\",      ");

        if(nonce != null)
            call.append("\"nonce\":\"" + nonce + "\",      ");

        if(data != null)
            call.append("\"data\":\"" + data);

        call.append("\"}]}");

        System.out.println(call.toString());

        StringEntity entity = new StringEntity(call.toString(), ContentType.APPLICATION_JSON);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(NetworkConstants.POST_URI);
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        String txId = jsonObject.get("result").toString();
        System.out.println(txId);

        Thread.sleep(1000);
        if (!Utils.isTransactionInMemPool(txId))
            sendTransaction(from, to, gas, gasPrice, value, data, nonce, --tries);

        while (!Utils.isTransactionMined(txId)) {
            if(!Utils.isTransactionInMemPool(txId))
                sendTransaction(from, to, gas, gasPrice, value, data, nonce, --tries);
            Thread.sleep(1000 * 10);
        }
        return true;
    }

    public static int getUscBestBlockHeight(){
        String payload = "{" +
                "\"jsonrpc\":\"2.0\", " +
                "\"method\":\"eth_blockNumber\", " +
                "\"params\":[], " +
                "\"id\":83" +
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
            return Integer.decode(jsonObj.get("result").toString());
        }catch(Exception ex){
            System.out.println(ex);
            return 0;
        }
    }
}
