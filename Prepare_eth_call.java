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
import co.usc.ulordj.core.Sha256Hash;
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

public class Prepare_eth_call {

    public static void main(String []args){
        prepareAndCallReceiveHeadersRPC(args);
    }

    public static void prepareAndCallReceiveHeadersRPC(String []args){
        try
        {
            while(true) {
                StringBuilder getBlockCount = new StringBuilder();
                StringBuilder getBlockHash = new StringBuilder();
                StringBuilder getBlockHeader = new StringBuilder();

                getBlockCount.append(NetworkConstants.ULORD_CLI);
                getBlockHash.append(NetworkConstants.ULORD_CLI);
                getBlockHeader.append(NetworkConstants.ULORD_CLI);

                if (args.length > 0 && args[0].equals("testnet")) {
                    getBlockCount.append(NetworkConstants.ULORD_TESTNET);
                    getBlockHash.append(NetworkConstants.ULORD_TESTNET);
                    getBlockHeader.append(NetworkConstants.ULORD_TESTNET);
                }

                getBlockCount.append(" getblockcount");
                getBlockHash.append(" getblockhash");
                getBlockHeader.append(" getblockheader");

                Process proc = null;
                Runtime rt = Runtime.getRuntime();

                proc = rt.exec(getBlockCount.toString());

                InputStream inStr = proc.getInputStream();
                InputStreamReader isr = new InputStreamReader(inStr);
                BufferedReader br = new BufferedReader(isr);

                int startIndex = getUldBlockChainBestChainHeight() + 1;
                int blockCount = Integer.parseInt(br.readLine());

                if(startIndex == blockCount){
                    Thread.sleep(1000*60*60); //sleep for 1hr.
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                String line = null;
                for (int i = startIndex; i <= blockCount; ++i) {
                    proc = rt.exec(getBlockHash.toString() + " " + i);
                    inStr = proc.getInputStream();
                    isr = new InputStreamReader(inStr);
                    br = new BufferedReader(isr);

                    line = br.readLine();
                    proc = rt.exec(getBlockHeader.toString() + " " + line + " false");
                    inStr = proc.getInputStream();
                    isr = new InputStreamReader(inStr);
                    br = new BufferedReader(isr);
                    line = null;

                    line = br.readLine();
                    if (line != null) {
                        builder.append(line);
                        builder.append(" ");
                    }

                    if (i % 100 == 0 || i == blockCount) {
                        builder.insert(0, "receiveHeaders ");
                        try {
                            // Unlock account
                            String payloadUnlockAcc = "{" +
                                    "\"jsonrpc\": \"2.0\", " +
                                    "\"method\": \"personal_unlockAccount\", " +
                                    "\"params\": [" +
                                    "\"674f05e1916abc32a38f40aa67ae6b503b565999\"," +
                                    "\"abcd1234\",\"\"" +
                                    "], " +
                                    "\"id\": \"1\"" +
                                    "}";
                            System.out.println(payloadUnlockAcc);
                            StringEntity entityUnlockAcc = new StringEntity(payloadUnlockAcc,
                                    ContentType.APPLICATION_JSON);

                            HttpClient httpClientUnlockAcc = HttpClientBuilder.create().build();
                            HttpPost requestUnlockAcc = new HttpPost(NetworkConstants.POST_URI);
                            requestUnlockAcc.setEntity(entityUnlockAcc);

                            HttpResponse responseUnlockAcc = httpClientUnlockAcc.execute(requestUnlockAcc);
                            System.out.println(responseUnlockAcc.getStatusLine().getStatusCode());

                            receiveHeaders(builder);

                            //Thread.sleep(1000 * 60 * 15); //Wait 15 min before sending next 100 Ulord Blocks.
                            builder = new StringBuilder();
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static String getReceiveHeadersString(String[] args) {
        if(args.length < 2)
            return "receiveHeaders <headers seperated by space>";

        byte[][] blocks = new byte[args.length - 1][140];
        for(int i = 0; i < args.length - 1; ++i) {
            byte[] b = Sha256Hash.hexStringToByteArray(args[i + 1]);
            blocks[i] = b;
        }
        return Sha256Hash.bytesToHex((Bridge.RECEIVE_HEADERS.encode(new Object[]{blocks})));
    }

    //Call receiveHeaders of Bridge.
    private static void receiveHeaders(StringBuilder builder) throws IOException {
        String payload = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_sendTransaction\", " +
                "\"params\": [{" +
                "\"from\": \"674f05e1916abc32a38f40aa67ae6b503b565999\"," +
                "\"to\": \"0x0000000000000000000000000000000001000006\"," +
                "\"gas\": \"0x3D0900\"," +
                "\"gasPrice\": \"0x9184e72a000\"," +
                "\"data\": \"" + getReceiveHeadersString(builder.toString().split(" ")) + "\"}]," +
                "\"id\": \"1\"" +
                "}";
        System.out.println(payload);
        StringEntity entity = new StringEntity(payload,
                ContentType.APPLICATION_JSON);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(NetworkConstants.POST_URI);
        request.setEntity(entity);

        HttpResponse response = httpClient.execute(request);

        String responseString = EntityUtils.toString(response.getEntity());
        JSONObject jsonObj = new JSONObject(responseString);
        String txHash = jsonObj.get("result").toString();
        while(true){
            if(getTransactionByHash(txHash)){
                txHash = null;
                break;
            }
            try{
                Thread.sleep(1000*30);
            }catch (Exception ex){
                System.err.println(ex.getMessage());
            }
        }
    }

    private static Boolean getTransactionByHash(String txHash){
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
                return false;
            }

            jsonObj = new JSONObject(result);
            String blockHash = jsonObj.get("blockHash").toString();
            String blockNumber = jsonObj.get("blockNumber").toString();


            if(!blockHash.equals("null") || !blockNumber.equals("null")){
                return true;
            }
            return false;
        }catch(Exception ex){
            System.out.println(ex);
            return false;
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

}
