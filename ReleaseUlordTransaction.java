/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeMainNetConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeSerializationUtils;
import co.usc.ulordj.core.*;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;
import co.usc.ulordj.script.Script;
import co.usc.ulordj.script.ScriptChunk;
import org.ethereum.core.CallTransaction;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static tools.Utils.*;


// USC release txs follow these steps: First, they are waiting for coin selection (releaseRequestQueue),
// then they are waiting for enough confirmations on the USC network (releaseTransactionSet),
// then they are waiting for federators' signatures (uscTxsWaitingForSignatures),
// then they are logged into the block that has them as completely signed for uld release
// and are removed from uscTxsWaitingForSignatures.

public class ReleaseUlordTransaction {

    private static List<UldECKey> federationKeys = new ArrayList<>();

    private static Logger logger = LoggerFactory.getLogger("releaseulordtransaction");

    public static void release(BridgeConstants bridgeConstants, String federationChangeAuthorizedAddress, String pwd) {

        NetworkParameters params = null;
        if(bridgeConstants instanceof BridgeTestNetConstants) {
            params = TestNet3Params.get();
        }
        else if (bridgeConstants instanceof BridgeRegTestConstants) {
            params = RegTestParams.get();
        } else {
            params = MainNetParams.get();
        }

        try {

            CallTransaction.Function function = Bridge.GET_STATE_FOR_ULD_RELEASE_CLIENT;

            String callResponse = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, Hex.toHexString(function.encodeSignature()));

            JSONObject jsonObject = new JSONObject(callResponse);

            String result = jsonObject.get("result").toString();
            String resultSub = result.substring(2, result.length());

            Object[] args = function.decodeResult(Hex.decode(resultSub));
            byte[] data = (byte[])args[0];
            RLPList rlpList = (RLPList) RLP.decode2(data).get(0);
            SortedMap<Keccak256, UldTransaction> uscTxsWaitingForSignatures;
            uscTxsWaitingForSignatures = BridgeSerializationUtils.deserializeMap(rlpList.get(0).getRLPData(), params, false);

            logger.info(uscTxsWaitingForSignatures.size() + " transactions waiting for signatures");
            System.out.println(uscTxsWaitingForSignatures.size() + " transactions waiting for signatures");

            if(uscTxsWaitingForSignatures.isEmpty())
                return;

            // Try to unlock account
            if (!Utils.tryUnlockUscAccount(federationChangeAuthorizedAddress, pwd)) {
                throw new PrivateKeyNotFoundException();
            }

            for (Map.Entry<Keccak256, UldTransaction> entry : uscTxsWaitingForSignatures.entrySet()) {
                Keccak256 key = entry.getKey();
                UldTransaction utTx = entry.getValue();

                // Check there are at least N blocks on top of the supplied transaction
                if(!validateTxDepth(key, bridgeConstants)) { continue; }

                String signResult = signRawTransaction(utTx, params);

                if(signResult.contains("error")) {
                    System.out.println(signResult);
                    continue;
                }

                jsonObject = new JSONObject(signResult);
                String complete = jsonObject.get("complete").toString();
                String rawUtTxHex = jsonObject.get("hex").toString();

                addSignatureToUSC(key, utTx, federationChangeAuthorizedAddress);

                if(complete.equals("true")) {

                    // Send Raw Transaction
                    String sendTxResponse = UlordCli.sendRawTransaction(params, rawUtTxHex);

                    if(sendTxResponse.contains("error")) {
                        String[] messages = sendTxResponse.split(":");
                        if(messages[messages.length - 1].contains("transaction already in block chain")) {
                            logger.warn("Transaction already in Ulord blockchain");
                            System.out.println("ReleaseUlordTransaction: Transaction already in Ulord blockchain");
                            // Try adding signature again to remove already processed transaction
                            addSignatureToUSC(key, utTx, federationChangeAuthorizedAddress);
                        }
                        else {
                            logger.info("Transaction failed: " + messages[messages.length - 1]);
                            System.out.println("ReleaseUlordTransaction: Transaction failed: " + messages[messages.length - 1]);
                        }
                    }
                    else {
                        logger.info("Ulord tx successfully processed, Ulord Tx id: " + sendTxResponse);
                        System.out.println("ReleaseUlordTransaction: Ulord tx successfully processed, Ulord Tx id: " + sendTxResponse);
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error(e.toString());
            System.out.println("ReleaseUlordTransaction: " + e);
        }
    }

    private static void addSignatureToUSC(Keccak256 uscTxHash, UldTransaction utTx, String federationChangeAuthorizedAddress) throws IOException {
        // Requires
        // 1. Federator Public Key  -   federationPublicKey
        // 2. Signatures    -   federation Signatures
        // 3. Usc Tx hash   -   key

        for (int nPrivKey = 0; nPrivKey < federationKeys.size(); nPrivKey++) {

            int numInputs = utTx.getInputs().size();
            byte[][] signatures = new byte[numInputs][];
            for (int i = 0; i < numInputs; ++i) {
                TransactionInput txIn = utTx.getInput(i);
                Script inputScript = txIn.getScriptSig();
                List<ScriptChunk> chunks = inputScript.getChunks();
                byte[] program = chunks.get(chunks.size() - 1).data;
                Script reedemScript = new Script(program);
                byte[] sig = federationKeys.get(nPrivKey).sign(utTx.hashForSignature(i, reedemScript, UldTransaction.SigHash.ALL, false)).encodeToDER();
                signatures[i] = sig;
            }

            CallTransaction.Function function = Bridge.ADD_SIGNATURE;

            String res = UscRpc.sendTransaction(federationChangeAuthorizedAddress,
                    PrecompiledContracts.BRIDGE_ADDR_STR,
                    "0x0",
                    getMinimumGasPrice(),
                    null,
                    Hex.toHexString(function.encode(federationKeys.get(nPrivKey).getPubKey(), signatures, uscTxHash.getBytes())),
                    null
            );
            logger.info("addSignatureToUSC response : " + res);
            System.out.println("addSignatureToUSC response : " + res);
        }
    }

    private static String signRawTransaction(UldTransaction tx, NetworkParameters params)
            throws IOException, PrivateKeyNotFoundException {
        String txId = getUlordTxId(tx, params);
        int vout = getVout(txId, params);
        String scriptPubKey = getScriptPubKey(vout, txId, params);

        Object[] keysObj = getPrivateKey(params).toArray();
        String[] keys = new String[keysObj.length];
        for(int i = 0 ; i < keysObj.length ; i ++){
            keys[i] = (String) keysObj[i];
        }

        String redeemScript = "";
        List<ScriptChunk> chunk = tx.getInput(0).getScriptSig().getChunks();
        for (int i = 0; i < chunk.size(); i++) {
            if(Hex.toHexString(chunk.get(i).data).equals(""))
                continue;
            redeemScript = Hex.toHexString(chunk.get(i).data);
            break;
        }

        String signRawTxResponse = UlordCli.signRawTransaction(
                params,
                Hex.toHexString(tx.ulordSerialize()),
                txId,
                vout,
                scriptPubKey,
                redeemScript,
                keys, null
        );

        return signRawTxResponse;
    }

    private static String getScriptPubKey(int vout, String txId, NetworkParameters params) throws IOException {
        String getRawTxJSON = UlordCli.getRawTransaction(params, txId, true);
        JSONObject jsonObject = new JSONObject(getRawTxJSON);
        JSONArray voutObjects = jsonObject.getJSONArray("vout");
        for(int i = 0; i < voutObjects.length(); ++i) {
            int n = Integer.parseInt(voutObjects.getJSONObject(i).get("n").toString());
            if(vout == n) {
                return voutObjects.getJSONObject(i).getJSONObject("scriptPubKey").get("hex").toString();
            }
        }
        return null;
    }

    private static int getVout(String txId, NetworkParameters params) throws IOException {

        String getRawTxJSON = UlordCli.getRawTransaction(params, txId, true);

        JSONObject jsonObject = new JSONObject(getRawTxJSON);
        JSONArray voutObjects = jsonObject.getJSONArray("vout");

        // Get Federation address
        String getFedAddrResponse = UscRpc.getFederationAddress();
        JSONObject getFedAddrJson = new JSONObject(getFedAddrResponse);
        String getFedAddrResult = getFedAddrJson.getString("result").substring(2);
        Object[] fedAddress = Bridge.GET_FEDERATION_ADDRESS.decodeResult(Hex.decode(getFedAddrResult));

        int vout = 0;
        for(int i = 0; i < voutObjects.length(); ++i) {
            jsonObject = new JSONObject(voutObjects.get(i).toString());
            vout = Integer.parseInt(jsonObject.get("n").toString());
            JSONArray addressObjects = jsonObject.getJSONObject("scriptPubKey").getJSONArray("addresses");

            boolean found = false;
            for(int j = 0; j < addressObjects.length(); ++j) {
                for (int k = 0; k < fedAddress.length; k++) {
                    if(addressObjects.get(j).toString().equals(fedAddress[k].toString())) {
                        found = true;
                        break;
                    }
                }
            }
            if(found)
                break;
        }
        return vout;
    }

    private static List<String> getPrivateKey(NetworkParameters params)
            throws IOException, PrivateKeyNotFoundException {

        JSONObject jsonObject = new JSONObject(UscRpc.getFederationSize());
        int fedSize = Integer.valueOf(jsonObject.getString("result").substring(2),16);

        List<UldECKey> publicKeys = new ArrayList<>();
        for (int i = 0; i < fedSize; i++) {
            String response = new JSONObject(UscRpc.getFederatorPublicKey(i)).getString("result");
            String pubKey = DataDecoder.decodeGetFederatorPublicKey(response);
            publicKeys.add(UldECKey.fromPublicOnly(Hex.decode(pubKey)));
        }

        boolean privateKeyFound = false;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < publicKeys.size(); i++) {
            String res = UlordCli.dumpPrivKey(params, publicKeys.get(i).toAddress(params).toString());
            if(!res.contains("error")) {
                privateKeyFound = true;
                keys.add(res);
                federationKeys.add(Utils.convertWifToPrivateKey(res, params));
            }
        }

        if(privateKeyFound == false)
            throw new PrivateKeyNotFoundException();

        return keys;
    }

    private static String getUlordTxId(UldTransaction tx, NetworkParameters params) throws IOException {
        JSONObject jsonObject = new JSONObject(UlordCli.decodeRawTransaction(params, Hex.toHexString(tx.ulordSerialize())));
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

        JSONObject jsonObject = new JSONObject(UscRpc.getTransactionByHash(key.toHexString()));

        String result = jsonObject.get("result").toString();
        jsonObject = new JSONObject(result);
        int txBlockNumber = Integer.decode(jsonObject.get("blockNumber").toString());

        jsonObject = new JSONObject(UscRpc.blockNumber());
        int currentBlockNumber = Integer.decode(jsonObject.get("result").toString());

        if((currentBlockNumber - txBlockNumber) > bridgeConstants.getUsc2UldMinimumAcceptableConfirmations())
            return true;
        return false;
    }
}
