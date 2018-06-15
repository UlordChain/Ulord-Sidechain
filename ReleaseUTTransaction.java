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
import co.usc.ulordj.core.*;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import co.usc.ulordj.script.Script;
import co.usc.ulordj.script.ScriptChunk;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;


public class ReleaseUTTransaction {

    private static String ulordCommand = "";
    private static UldECKey federationKey = null;

    // USC release txs follow these steps: First, they are waiting for coin selection (releaseRequestQueue),
    // then they are waiting for enough confirmations on the USC network (releaseTransactionSet),
    // then they are waiting for federators' signatures (uscTxsWaitingForSignatures),
    // then they are logged into the block that has them as completely signed for uld release
    // and are removed from uscTxsWaitingForSignatures.

    public static void main(String[] args){
        BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
        releaseUT(bridgeConstants);
    }

    // NOTE: Requires federator's private key to run this function.
    public static void releaseUT(BridgeConstants bridgeConstants) {

        ulordCommand += NetworkConstants.ULORD_CLI;

        NetworkParameters params = null;
        if(bridgeConstants instanceof BridgeTestNetConstants) {
            params = TestNet3Params.get();
            ulordCommand += NetworkConstants.ULORD_TESTNET;
        }
        else if (bridgeConstants instanceof BridgeMainNetConstants) {
            params = MainNetParams.get();
        }


        while(true) {
            try {
                //  web3.eth.call({data:"0B851400",to:"0x0000000000000000000000000000000001000006"})
                CallTransaction.Function function = Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT;
                String rpcCall = "{" +
                        "\"jsonrpc\": \"2.0\", " +
                        "\"method\": \"eth_call\", " +
                        "\"params\": [{" +
                        "\"to\": \"" + PrecompiledContracts.BRIDGE_ADDR_STR + "\"," +
                        "\"data\": \"" + Hex.toHexString(function.encodeSignature()) + "\"},\"latest\"]," +
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
                    UldTransaction utTx = entry.getValue();

                    // Check there are at least N blocks on top of the supplied transaction
                    if(!validateTxDepth(key, bridgeConstants)) { continue; }

                    String signResult = signRawTransaction(utTx, bridgeConstants, params);

                    jsonObject = new JSONObject(signResult);
                    String complete = jsonObject.get("complete").toString();
                    String rawUtTxHex = jsonObject.get("hex").toString();

                    System.out.println(addSignatureToUSC(key, utTx, params));   // addSignatures in Bridge.java

                    if(complete.equals("true")) {

                        // Send Raw Transaction
                        String sendTxResult = sendRawTransaction(rawUtTxHex);

                        if(sendTxResult.contains("error")) {
                            String[] messages = sendTxResult.split(":");
                            if(messages[messages.length - 1].contains("transaction already in block chain")) {
                                System.out.println("Transaction already in blockchain");
                                // Its safe to remove processed transactions here.
                                // TODO: Remove transaction from uscTxsWaitingForSignatures
                            }
                            else
                                System.out.println("Transaction failed: " + messages[messages.length - 1]);
                        }
                        else {
                            System.out.println("Ulord tx successfully processed, Tx id: " + sendTxResult);
                        }
                    }
                    else
                    {
                        // TODO: Send Transaction for further signing
                    }
                }
            }
            catch (Exception e) {
                System.out.println(e);
                //break;
            }
            finally {
                try {
                    Thread.sleep(1000 * 60 * 15);   // Sleep for 15 minutes
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }

    private static String addSignatureToUSC(Keccak256 uscTxHash, UldTransaction utTx, NetworkParameters params) throws IOException {
        // Requires
        // 1. Federator Public Key  -   federationPublicKey
        // 2. Signatures
        // 3. Usc Tx hash   -   key

        int numInputs = utTx.getInputs().size();
        byte[][] signatures = new byte[numInputs][];
        for(int i = 0; i < numInputs; ++i) {
            TransactionInput txIn = utTx.getInput(i);
            Script inputScript = txIn.getScriptSig();
            List<ScriptChunk> chunks = inputScript.getChunks();
            byte[] program = chunks.get(chunks.size() - 1).data;
            Script reedemScript = new Script(program);
            byte[] sig = federationKey.sign(utTx.hashForSignature(i, reedemScript, UldTransaction.SigHash.ALL, false)).encodeToDER();
            signatures[i] = sig;
        }

        CallTransaction.Function function = Bridge.ADD_SIGNATURE;
        String rpcCall = "{" +
                "\"jsonrpc\": \"2.0\", " +
                "\"method\": \"eth_sendTransaction\", " +  // Change it to eth_sendTransaction after testing.
                "\"params\": [{" +
                "\"from\": \"" + "674f05e1916abc32a38f40aa67ae6b503b565999" + "\"," +
                "\"to\": \"" + PrecompiledContracts.BRIDGE_ADDR_STR + "\"," +
                "\"gas\": \"0x3D0900\"," +
                "\"gasPrice\": \"0x9184e72a000\"," +
                "\"data\": \"" + Hex.toHexString(function.encode(federationKey.getPubKey(), signatures, uscTxHash.getBytes())) + "\"}]," +
                "\"id\": \"1\"" +
                "}";
        System.out.println(rpcCall);

        StringEntity entity = new StringEntity(rpcCall, ContentType.APPLICATION_JSON);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(NetworkConstants.POST_URI);
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost);
        return EntityUtils.toString(response.getEntity());
    }

    private static String signRawTransaction(UldTransaction tx, BridgeConstants bridgeConstants, NetworkParameters params)
            throws IOException, PrivateKeyNotFoundException {

        String signRawTransaction = ulordCommand + " signrawtransaction";

        String txId = getUtTxId(tx, params);
        int vout = getVout(txId, params);
        String scriptPubKey = getScriptPubKey(vout, txId, params);
        signRawTransaction +=
                " '" + Hex.toHexString(tx.ulordSerialize()) + "'" +
                        " '[{" +
                            " \"txid\":"         + "\"" + txId + "\"," +
                            " \"vout\":"         + vout + ","+
                            " \"scriptPubKey\":" + "\"" + scriptPubKey + "\"," +
                            " \"redeemScript\":" + "\"" + Hex.toHexString(tx.getInput(0).getScriptSig().getChunks().get(2).data) + "\"" +
                        "}]'"  +
                        " '["  +
                            "\"" + getPrivateKey(bridgeConstants, params) +"\"" +
                        "]'";

        Process proc = null;
        Runtime runtime = Runtime.getRuntime();
        proc = runtime.exec(new String[] { "bash", "-c", signRawTransaction });

        InputStream inputStream = proc.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        return Utils.BufferedReaderToString(bufferedReader);
    }

    private static String sendRawTransaction(String hex) throws IOException {
        String sendRawTx = ulordCommand + " sendrawtransaction " + hex;
        System.out.println(sendRawTx);

        Process proc = null;
        Runtime runtime = Runtime.getRuntime();
        proc = runtime.exec(new String[] { "bash", "-c", sendRawTx});

        InputStream inputStream = proc.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String result = Utils.BufferedReaderToString(bufferedReader);

        if(result.equals("")) {
            inputStream = proc.getErrorStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            result = Utils.BufferedReaderToString(bufferedReader);
        }

        return result;
    }

    private static String getScriptPubKey(int vout, String txId, NetworkParameters params) throws IOException {
        String txJSONString = getRawTransaction(txId, params);
        JSONObject jsonObject = new JSONObject(decodeTxToJSONString(Hex.decode(txJSONString), params));
        JSONArray voutObjects = jsonObject.getJSONArray("vout");
        for(int i = 0; i < voutObjects.length(); ++i) {
            int n = Integer.parseInt(voutObjects.getJSONObject(i).get("n").toString());
            if(vout == n) {
                return voutObjects.getJSONObject(i).getJSONObject("scriptPubKey").get("hex").toString();
            }
        }
        return null;
    }



    private static String getRawTransaction(String txId, NetworkParameters params) throws IOException {
        String getRawTransaction = ulordCommand + " getrawtransaction " + txId;
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(getRawTransaction);

        InputStream inputStream = proc.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        return Utils.BufferedReaderToString(bufferedReader);
    }

    private static int getVout(String txId, NetworkParameters params) throws IOException {

        String txJSONString = decodeTxToJSONString(Sha256Hash.hexStringToByteArray(getRawTransaction(txId, params)), params);

        JSONObject jsonObject = new JSONObject(txJSONString);
        JSONArray voutObjects = jsonObject.getJSONArray("vout");

        int vout = 0;
        for(int i = 0; i < voutObjects.length(); ++i) {
            jsonObject = new JSONObject(voutObjects.get(i).toString());
            vout = Integer.parseInt(jsonObject.get("n").toString());
            JSONArray addressObjects = jsonObject.getJSONObject("scriptPubKey").getJSONArray("addresses");

            boolean found = false;
            for(int j = 0; j < addressObjects.length(); ++j) {
                // TODO: Find a better way to get Federation multisig address
                if(addressObjects.get(j).toString().equals(NetworkConstants.MULTISIG_ADDRESS)) {
                    found = true;
                    break;
                }
            }
            if(found)
                break;
        }
        return vout;
    }

    private static String getPrivateKey(BridgeConstants bridgeConstants, NetworkParameters params)
            throws IOException, PrivateKeyNotFoundException {

        String dumpPrivKey = ulordCommand + " dumpprivkey ";

        List<UldECKey> publicKeys = bridgeConstants.getGenesisFederation().getPublicKeys();
        BufferedReader bufferedReader = null;

        boolean privateKeyFound = false;
        String key = null;
        for(UldECKey pubKey : publicKeys) {
            pubKey.toAddress(params);
            String rpc = dumpPrivKey + pubKey.toAddress(params).toString();
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(rpc);

            InputStream inputStream = proc.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            key = bufferedReader.readLine();
            if(key != null) {
                privateKeyFound = true;
                federationKey = Utils.convertWifToPrivateKey(key, pubKey, params);
                break;
            }
        }
        if(privateKeyFound == false)
            throw new PrivateKeyNotFoundException();

        return key;
    }

    private static String decodeTxToJSONString(byte[] tx, NetworkParameters params) throws IOException {
        String decodeRawTx = ulordCommand + " decoderawtransaction " + Sha256Hash.bytesToHex(tx);

        Process proc = null;
        Runtime rt  = Runtime.getRuntime();
        proc = rt.exec(decodeRawTx);

        InputStream inputStream = proc.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        return Utils.BufferedReaderToString(bufferedReader);
    }

    private static String getUtTxId(UldTransaction tx, NetworkParameters params) throws IOException {
        JSONObject jsonObject = new JSONObject(decodeTxToJSONString(tx.ulordSerialize(), params));
        String vin = jsonObject.get("vin").toString();
        jsonObject = new JSONObject(vin.substring(1, vin.length() - 1));

        return jsonObject.get("txid").toString();
    }

    /**
     * Checks if there is at least N blocks on top of the supplied transaction
     * @param key   Transaction hash to validate
     * @param bridgeConstants   USC Network parameter constant
     * @return  true: if there is at least N blocks on top of the supplied txn, false: otherwise
     * @throws IOException
     */
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
        httpClient = HttpClientBuilder.create().build();
        response = httpClient.execute(request);
        responseStr = EntityUtils.toString(response.getEntity());

        jsonObject = new JSONObject(responseStr);
        int currentBlockNumber = Integer.decode(jsonObject.get("result").toString());

        if((currentBlockNumber - txBlockNumber) > bridgeConstants.getUsc2UldMinimumAcceptableConfirmations())
            return true;
        return false;
    }
}
