/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeMainNetConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeSerializationUtils;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.core.Sha256Hash;
import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.ethereum.core.CallTransaction;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

public class ReleaseUT {

    // USC release txs follow these steps: First, they are waiting for coin selection (releaseRequestQueue),
    // then they are waiting for enough confirmations on the USC network (releaseTransactionSet),
    // then they are waiting for federators' signatures (uscTxsWaitingForSignatures),
    // then they are logged into the block that has them as completely signed for uld release
    // and are removed from uscTxsWaitingForSignatures.

    public static void main(String[] args){
        BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
        releaseUT(bridgeConstants);
    }

    // Requires federator's private key to run this function.
    public static void releaseUT(BridgeConstants bridgeConstants){
        NetworkParameters params = null;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            params = TestNet3Params.get();
        else if (bridgeConstants instanceof BridgeMainNetConstants)
            params = MainNetParams.get();

        while(true) {
            try {
                //  web3.eth.call({data:"0B851400",to:"0x0000000000000000000000000000000001000006"})
                CallTransaction.Function function = Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT;
                String rpcCall = "{" +
                        "\"jsonrpc\": \"2.0\", " +
                        "\"method\": \"eth_call\", " +
                        "\"params\": [{" +
                        "\"to\": \"" + PrecompiledContracts.BRIDGE_ADDR_STR + "\"," +
                        "\"data\": \"" + Sha256Hash.bytesToHex(function.encodeSignature()) + "\"},\"latest\"]," +
                        "\"id\": \"1\"" +
                        "}";
                StringEntity entity = new StringEntity(rpcCall, ContentType.APPLICATION_JSON);

                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost request = new HttpPost(NetworkConstants.POST_URI);
                request.setEntity(entity);

                HttpResponse response = httpClient.execute(request);
                String responseStr = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(responseStr);

                String result = jsonObject.get("result").toString();
                String resultSub = result.substring(2, result.length());

                Object[] args = function.decodeResult(Sha256Hash.hexStringToByteArray(resultSub));
                byte[] data = (byte[])args[0];
                RLPList rlpList = (RLPList) RLP.decode2(Sha256Hash.hexStringToByteArray(Sha256Hash.bytesToHex(data))).get(0);
                SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures;
                uscTxsWaitingForSignatures = BridgeSerializationUtils.deserializeMap(rlpList.get(0).getRLPData(), params, false);

                for (Map.Entry<Keccak256, UldTransaction> entry : uscTxsWaitingForSignatures.entrySet()) {

                    Keccak256 key = entry.getKey();
                    // Check there are at least N blocks on top of the supplied transaction
                    boolean isValid = validateTxDepth(key, bridgeConstants);






                    byte[] rawTx = entry.getValue().ulordSerialize();
                    System.out.println(Sha256Hash.bytesToHex(rawTx));

                    // TODO: Use getUldTxHashProcessedHeight to verify that the transaction is minimum 10 blocks deep in testnet
                }
                Thread.sleep(1000 * 60 * 10);
            }
            catch (Exception e) {
                System.out.println(e);
                break;
            }
        }
    }

    private static boolean validateTxDepth(Keccak256 key, BridgeConstants bridgeConstants) throws IOException {

        String rpcCall = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_getTransactionByHash\", " +
                "\"params\": [\"" +
                        key.toHexString() +
                    "\"]," +
                "\"id\": \"1\"" +
                "}";
        StringEntity entity = new StringEntity(rpcCall, ContentType.APPLICATION_JSON);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(NetworkConstants.POST_URI);
        request.setEntity(entity);

        HttpResponse response = httpClient.execute(request);
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsonObject = new JSONObject(responseStr);

        String result = jsonObject.get("result").toString();
        jsonObject = new JSONObject(result);
        int txBlockNumber = Integer.decode(jsonObject.get("blockNumber").toString());

        rpcCall = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_blockNumber\", " +
                "\"params\": []," +
                "\"id\": \"1\"" +
                "}";
        entity = new StringEntity(rpcCall, ContentType.APPLICATION_JSON);
        request = new HttpPost(NetworkConstants.POST_URI);
        request.setEntity(entity);
        response = httpClient.execute(request);
        responseStr = EntityUtils.toString(response.getEntity());

        jsonObject = new JSONObject(responseStr);
        int currentBlockNumber = Integer.decode(jsonObject.get("result").toString());

        if((currentBlockNumber - txBlockNumber) > bridgeConstants.getUsc2UldMinimumAcceptableConfirmations())
            return true;
        return false;
    }

    private static String getGetUldTxHashProcessedHeightString(Sha256Hash key) {
        System.out.println(key.toString());
        return Sha256Hash.bytesToHex(Bridge.GET_ULD_TX_HASH_PROCESSED_HEIGHT.encode(new Object[]{key.toString()}));
    }


}
