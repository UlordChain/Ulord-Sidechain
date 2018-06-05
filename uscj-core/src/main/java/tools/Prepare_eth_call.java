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
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Prepare_eth_call {
    public static void main(String []args){
        prepareAndCallReceiveHeadersRPC(args);
    }

    public static void prepareAndCallReceiveHeadersRPC(String []args){
        try
        {

            StringBuilder getBlockCount = new StringBuilder();
            StringBuilder getBlockHash = new StringBuilder();
            StringBuilder getBlockHeader = new StringBuilder();

            getBlockCount.append("ulord-cli");
            getBlockHash.append("ulord-cli");
            getBlockHeader.append("ulord-cli");

            if (args.length > 0 && args[0].equals("testnet")) {
                getBlockCount.append(" -testnet");
                getBlockHash.append(" -testnet");
                getBlockHeader.append(" -testnet");
            }

            getBlockCount.append(" getblockcount");
            getBlockHash.append(" getblockhash");
            getBlockHeader.append(" getblockheader");

            Process proc = null;
            Runtime rt = Runtime.getRuntime();

            proc = rt.exec("ulord-cli -testnet getblockcount");

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
            for(int i=1; i<blockCount; ++i){
                proc = rt.exec("ulord-cli -testnet getblockhash "+ i);
                inStr = proc.getInputStream();
                isr = new InputStreamReader(inStr);
                br = new BufferedReader(isr);

                line = br.readLine();
                proc = rt.exec("ulord-cli -testnet getblockheader " + line +" false");
                inStr = proc.getInputStream();
                isr = new InputStreamReader(inStr);
                br = new BufferedReader(isr);
                line = null;

                line = br.readLine();
                if (line != null) {
                    builder.append(line);
                    builder.append(" ");
                }

                if(i%500==0 || i == blockCount){
                    builder.insert(0, "receiveHeaders ");
                    try {
                        String payload = "{" +
                                "\"jsonrpc\": \"2.0\", " +
                                "\"method\": \"eth_sendTransaction\", " +
                                "\"params\": [{" +
                                "\"from\": \"674f05e1916abc32a38f40aa67ae6b503b565999\"," +
                                "\"to\": \"0x0000000000000000000000000000000001000006\"," +
                                "\"gas\": \"0x4C4B40\"," +
                                "\"gasPrice\": \"0x9184e72a000\"," +
//                                "\"value\": \"0x00\"," +
                                "\"data\": \"" + getReceiveHeadersString(builder.toString().split(" ")) + "\"}]," +
                                "\"id\": \"1\"" +
                                "}";
                        System.out.println(payload);
                        StringEntity entity = new StringEntity(payload,
                                ContentType.APPLICATION_JSON);

                        HttpClient httpClient = HttpClientBuilder.create().build();
                        HttpPost request = new HttpPost("http://localhost:44444");
                        request.setEntity(entity);

                        HttpResponse response = httpClient.execute(request);
                        System.out.println(response.getStatusLine().getStatusCode());
                        int statusCode = response.getStatusLine().getStatusCode();
                        while (statusCode != 200) {
                            Thread.sleep(5000);
                        }
                        if (statusCode == 200) {
                            Thread.sleep(60000);
                        }
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
