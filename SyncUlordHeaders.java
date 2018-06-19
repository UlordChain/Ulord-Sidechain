/*
 * This file is part of Usc
 * Copyright (C) 2016 - 2018 Ulord development team.
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

package tools;

import co.usc.peg.Bridge;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.core.Sha256Hash;
import co.usc.ulordj.params.TestNet3Params;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SyncUlordHeaders {

    public static void main(String []args){
        prepareAndCallReceiveHeadersRPC(args);
    }

    public static void prepareAndCallReceiveHeadersRPC(String []args){
        try
        {
            //Do not sync Ulord Headers if USC Blockchain has less than 60 blocks mined.
            while(getUSCBlockNumber() <=60) {
                Thread.sleep(1000*30);
            }
            NetworkParameters params = TestNet3Params.get();
            //Start Syncing Ulord Headers.
            while(true) {
                int startIndex = getUldBlockChainBestChainHeight() + 1;

                //Keep the ulord block headers in USC 24 blocks behind actual Ulord block headers.
                //Approx 1hr behind ulord blockchain.
                //blockCount = gets the ulord best block height - 24.
                int blockCount = Integer.parseInt(UlordCli.getBlockCount(params)) - 24;

                if((blockCount < startIndex)){
                    Thread.sleep((long)(1000*60*2.5)); //sleep for 2.5min.
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                String line = null;
                for (int i = startIndex; i <= blockCount; ++i) {
                    //getBlockHash gets the block hash for the given height.
                    line = UlordCli.getBlockHash(params,i);

                    //getBlockHeader gets the block header for the given block hash.
                    line = UlordCli.getBlockHeader(params, line, false);

                    if (line != null) {
                        builder.append(line);
                        builder.append(" ");
                    }

                    if (i % 100 == 0 || i == blockCount) {
                        builder.insert(0, "receiveHeaders ");
                        try {
                            // Unlock account
                            unlockFederationChangeAuthorizedKeys();

                            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            Date date = new Date();
                            System.out.println(dateFormat.format(date) +": Ulord Block headers from " + (startIndex) + " to " + i + " sent to USC.");

                            int UscBestBlockHeightBeforeReceiveHeaders = Utils.getUscBestBlockHeight();
                            //Receive Ulord Headers and create a transaction on USC.
                            String txHash = receiveHeaders(builder);

                            System.out.println("Transaction ID: " + txHash);
                            while(true){
                                Thread.sleep(1000*30);
                                int UscBestBlockHeightAfterReceiveHeaders = Utils.getUscBestBlockHeight();
                                if(isBlockHeightDifferenceAtLeast20(UscBestBlockHeightBeforeReceiveHeaders, UscBestBlockHeightAfterReceiveHeaders)){
                                    break;
                                }
                            }

                            //send new block headers once previous block headers are mined.
                            while(true){
                                String txStatus = getTransactionByHash(txHash);
                                if(txStatus.equals("notMined")){
                                    //sleep 30sec before checking if transaction is mined.
                                    Thread.sleep(1000*30);
                                }
                                else if(txStatus.equals("mined")){
                                    txHash = null;
                                    //sleep 30sec before sending new headers to be mined.
                                    Thread.sleep(1000*30);
                                    break;
                                }
                                else if(txStatus.equals("rejected")){
                                    //in case transaction is not mined/rejected recreate the transaction.
                                    System.out.println(dateFormat.format(date) +": Ulord Block headers from " + (startIndex) + " to " + i + " sent to USC.");
                                    UscBestBlockHeightBeforeReceiveHeaders = Utils.getUscBestBlockHeight();
                                    txHash = receiveHeaders(builder);
                                    System.out.println("Transaction ID: " + txHash);
                                    while(true){
                                        Thread.sleep(1000*30);
                                        int UscBestBlockHeightAfterReceiveHeaders = Utils.getUscBestBlockHeight();
                                        if(isBlockHeightDifferenceAtLeast20(UscBestBlockHeightBeforeReceiveHeaders, UscBestBlockHeightAfterReceiveHeaders)){
                                            break;
                                        }
                                    }
                                }
                            }
                            startIndex = i+1;
                            builder = new StringBuilder();
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                            break;
                        }
                    }
                }
            }
        }
        catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static void unlockFederationChangeAuthorizedKeys() throws  IOException{
        String payloadUnlockAcc = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"personal_unlockAccount\", " +
                "\"params\": [" +
                "\"674f05e1916abc32a38f40aa67ae6b503b565999\"," +
                "\"abcd1234\",\"\"" +
                "], " +
                "\"id\": \"1\"" +
                "}";

        StringEntity entityUnlockAcc = new StringEntity(payloadUnlockAcc,
                ContentType.APPLICATION_JSON);

        HttpClient httpClientUnlockAcc = HttpClientBuilder.create().build();
        HttpPost requestUnlockAcc = new HttpPost(NetworkConstants.POST_URI);
        requestUnlockAcc.setEntity(entityUnlockAcc);

        HttpResponse responseUnlockAcc = httpClientUnlockAcc.execute(requestUnlockAcc);
    }

    //Call receiveHeaders of Bridge.
    private static String receiveHeaders(StringBuilder builder) throws IOException {
        String payload = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_sendTransaction\", " +
                "\"params\": [{" +
                "\"from\": \"674f05e1916abc32a38f40aa67ae6b503b565999\"," +
                "\"to\": \"0x0000000000000000000000000000000001000006\"," +
                "\"gas\": \"0x3D0900\"," +
                "\"gasPrice\": \"0x9184e72a000\"," +
                "\"data\": \"" + DataEncoder.getReceiveHeadersString(builder.toString().split(" ")) + "\"}]," +
                "\"id\": \"1\"" +
                "}";

        StringEntity entity = new StringEntity(payload,
                ContentType.APPLICATION_JSON);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(NetworkConstants.POST_URI);
        request.setEntity(entity);

        HttpResponse response = httpClient.execute(request);

        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject jsonObj = new JSONObject(responseString);
        String txHash = jsonObj.get("result").toString();
        return txHash;
    }

    private static String getTransactionByHash(String txHash){
        String payload = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_getTransactionByHash\", " +
                "\"params\": [" +
                "\"" + txHash + "\"]," +
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
            if(result.equals("null")){
                return "rejected";
            }

            jsonObj = new JSONObject(result);
            String blockHash = jsonObj.get("blockHash").toString();
            String blockNumber = jsonObj.get("blockNumber").toString();

            if(!blockHash.equals("null") || !blockNumber.equals("null")){
                return "mined";
            }
            return "notMined";
        }catch(Exception ex){
            System.out.println(ex);
            return "error";
        }
    }

    //Call getUldBlockChainBestChainHeight of Bridge.
    private static int getUldBlockChainBestChainHeight(){
        //web3.eth.call({data:"3f4173af", to:"0x0000000000000000000000000000000001000006"});
        String payload = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_call\", " +
                "\"params\": [{" +
                "\"to\": \"0x0000000000000000000000000000000001000006\"," +
                "\"data\": \"3f4173af\"},\"latest\"]," +
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
            return Integer.decode(jsonObj.get("result").toString());
        }catch(Exception ex){
            System.out.println(ex);
            return 1;
        }
    }

    public static int getUSCBlockNumber() throws IOException{
        String payloadUSCBlockNumber = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_blockNumber\", " +
                "\"params\": {}," +
                "\"id\": \"666\"" +
                "}";

        StringEntity entity = new StringEntity(payloadUSCBlockNumber,
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

    private static boolean isBlockHeightDifferenceAtLeast20(int UscBestBlockHeightBeforeReceiveHeaders, int UscBestBlockHeightAfterReceiveHeaders){
        if((UscBestBlockHeightAfterReceiveHeaders - UscBestBlockHeightBeforeReceiveHeaders) >=20){
            return true;
        }
        return false;
    }
}
