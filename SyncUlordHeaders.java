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

import co.usc.ulordj.core.NetworkParameters;
import com.typesafe.config.Config;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.ethereum.db.ContractDetails;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SyncUlordHeaders implements Runnable{

    NetworkParameters params;
    String[] ulordFederationAddress;
    String federationChangeAuthorizedAddress;
    String federationChangeAuthorizedPassword;

    public SyncUlordHeaders(NetworkParameters params, Config config){
        this.params = params;
        this.ulordFederationAddress = config.getStringList("federation.addresses").toArray(new String[0]);
        this.federationChangeAuthorizedAddress = config.getString("federation.changeAuthorizedAddress");
        this.federationChangeAuthorizedPassword = config.getString("federation.changeAuthorizedPassword");
    }

    @Override
    public void run() {
        start();
    }

    private void start(){
        prepareAndCallReceiveHeadersRPC();
    }

    private void prepareAndCallReceiveHeadersRPC(){
        try
        {
            //Do not sync Ulord Headers if USC Blockchain has less than 60 blocks mined.
            while(getUSCBlockNumber() <=60) {
                Thread.sleep(1000*30);
            }
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
                        try {
                            // Unlock account
                            UscRpc.unlockAccount(federationChangeAuthorizedAddress, federationChangeAuthorizedPassword);

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
                            System.out.println("SyncUlordHeaders.prepareAndCallReceiveHeadersRPC: " + ex.getMessage());
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

    //Call receiveHeaders of Bridge.
    private String receiveHeaders(StringBuilder builder) throws IOException {

        String responseString = UscRpc.sendTransaction(
                federationChangeAuthorizedAddress,
                PrecompiledContracts.BRIDGE_ADDR_STR,
                "0x3D0900",
                "0x9184e72a000",
                null,
                DataEncoder.encodeReceiveHeaders(builder.toString().split(" ")),
                null);
        JSONObject jsonObj = new JSONObject(responseString);
        String txHash = jsonObj.get("result").toString();

        return txHash;
    }

    private String getTransactionByHash(String txHash) {
        try {
            String responseString = UscRpc.getTransactionByHash(txHash);
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
            System.out.println("SyncUlordHeaders.getTransactionByHash: " + ex);
            return "error";
        }
    }

    //Call getUldBlockChainBestChainHeight of Bridge.
    private int getUldBlockChainBestChainHeight(){
        try {
            String responseString = UscRpc.getUldBlockChainBestChainHeight();
            JSONObject jsonObj = new JSONObject(responseString);
            return Integer.decode(jsonObj.get("result").toString());
        }catch(Exception ex){
            System.out.println("SyncUlordHeaders.getUldBlockChainBestChainHeight: " + ex);
            return 1;
        }
    }

    public int getUSCBlockNumber() {
        try {
            String responseString = UscRpc.blockNumber();
            JSONObject jsonObj = new JSONObject(responseString);
            return Integer.decode(jsonObj.get("result").toString());
        }catch(Exception ex){
            System.out.println("SyncUlordHeaders.getUSCBlockNumber: " + ex);
            return 0;
        }
    }

    private boolean isBlockHeightDifferenceAtLeast20(int UscBestBlockHeightBeforeReceiveHeaders, int UscBestBlockHeightAfterReceiveHeaders){
        if((UscBestBlockHeightAfterReceiveHeaders - UscBestBlockHeightBeforeReceiveHeaders) >=20){
            return true;
        }
        return false;
    }
}