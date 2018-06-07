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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Prepare_eth_call {

    private static final String POST_URI = "http://localhost:44444";
    private static final String ULORD_CLI = "ulord-cli";
    private static final String ULORD_TESTNET = " -testnet";
    public static void main(String []args){
        prepareAndCallReceiveHeadersRPC(args);
    }

    public static void prepareAndCallReceiveHeadersRPC(String []args){
        try
        {

            StringBuilder getBlockCount = new StringBuilder();
            StringBuilder getBlockHash = new StringBuilder();
            StringBuilder getBlockHeader = new StringBuilder();

            getBlockCount.append(ULORD_CLI);
            getBlockHash.append(ULORD_CLI);
            getBlockHeader.append(ULORD_CLI);

            if (args.length > 0 && args[0].equals("testnet")) {
                getBlockCount.append(ULORD_TESTNET);
                getBlockHash.append(ULORD_TESTNET);
                getBlockHeader.append(ULORD_TESTNET);
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

            int startIndex = 1;
            int blockCount = Integer.parseInt(br.readLine());
            if(args.length > 0 && !args[0].equals("testnet")){
                if(args.length == 1){
                    startIndex = Integer.parseInt(args[0]);
                } else if (args.length == 2){
                    startIndex = Integer.parseInt(args[0]);
                    blockCount = Integer.parseInt(args[1]);
                }
            }else{
                if(args.length == 2){
                    startIndex = Integer.parseInt(args[1]);
                } else if (args.length == 3){
                    startIndex = Integer.parseInt(args[1]);
                    blockCount = Integer.parseInt(args[2]);
                }
            }

            StringBuilder builder = new StringBuilder();
            String line = null;
            for(int i=startIndex; i<=blockCount; ++i){
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

                if(i%100==0 || i == blockCount){
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
                        HttpPost requestUnlockAcc = new HttpPost(POST_URI);
                        requestUnlockAcc.setEntity(entityUnlockAcc);

                        HttpResponse responseUnlockAcc = httpClientUnlockAcc.execute(requestUnlockAcc);
                        System.out.println(responseUnlockAcc.getStatusLine().getStatusCode());

                        String payload = "{" +
                                "\"jsonrpc\": \"2.0\", " +
                                "\"method\": \"eth_sendTransaction\", " +
                                "\"params\": [{" +
                                "\"from\": \"674f05e1916abc32a38f40aa67ae6b503b565999\"," +
                                "\"to\": \"0x0000000000000000000000000000000001000006\"," +
                                "\"gas\": \"0x3D0900\"," +
                                "\"gasPrice\": \"0x9184e72a000\"," +
//                                "\"value\": \"0x00\"," +
                                "\"data\": \"" + getReceiveHeadersString(builder.toString().split(" ")) + "\"}]," +
                                "\"id\": \"1\"" +
                                "}";
                        System.out.println(payload);
                        StringEntity entity = new StringEntity(payload,
                                ContentType.APPLICATION_JSON);

                        HttpClient httpClient = HttpClientBuilder.create().build();
                        HttpPost request = new HttpPost(POST_URI);
                        request.setEntity(entity);

                        HttpResponse response = httpClient.execute(request);
                        System.out.println(EntityUtils.toString(response.getEntity()));
                        int statusCode = response.getStatusLine().getStatusCode();

                        while (statusCode != 200) {
                            Thread.sleep(5000);
                        }
                        Thread.sleep(1000 * 60 * 8);
                        builder = new StringBuilder();
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
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
}
