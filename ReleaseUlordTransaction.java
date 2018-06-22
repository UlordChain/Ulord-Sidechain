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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;


// USC release txs follow these steps: First, they are waiting for coin selection (releaseRequestQueue),
// then they are waiting for enough confirmations on the USC network (releaseTransactionSet),
// then they are waiting for federators' signatures (uscTxsWaitingForSignatures),
// then they are logged into the block that has them as completely signed for uld release
// and are removed from uscTxsWaitingForSignatures.

public class ReleaseUlordTransaction {

    private static UldECKey federationKey = null;

    private static Logger logger = LoggerFactory.getLogger("releaseulordtransaction");

    public static void release(BridgeConstants bridgeConstants, String federationChangeAuthorizedAddress, String pwd, String fedMultisigAddress) {

        NetworkParameters params = null;
        if(bridgeConstants instanceof BridgeTestNetConstants) {
            params = TestNet3Params.get();
        }
        else if (bridgeConstants instanceof BridgeMainNetConstants) {
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

                String signResult = signRawTransaction(utTx, bridgeConstants, params, fedMultisigAddress);

                jsonObject = new JSONObject(signResult);
                String complete = jsonObject.get("complete").toString();
                String rawUtTxHex = jsonObject.get("hex").toString();

                String addSigToUscResponse = addSignatureToUSC(key, utTx, federationChangeAuthorizedAddress);
                System.out.println(addSigToUscResponse );   // addSignatures in Bridge.java
                logger.info(addSigToUscResponse);

                if(complete.equals("true")) {

                    // Send Raw Transaction
                    String sendTxResponse = UlordCli.sendRawTransaction(params, rawUtTxHex);

                    if(sendTxResponse.contains("error")) {
                        String[] messages = sendTxResponse.split(":");
                        if(messages[messages.length - 1].contains("transaction already in block chain")) {
                            System.out.println("ReleaseUlordTransaction: Transaction already in blockchain");
                            // Try adding signature again to remove already processed transaction
                            addSignatureToUSC(key, utTx, federationChangeAuthorizedAddress);
                        }
                        else
                            System.out.println("ReleaseUlordTransaction: Transaction failed: " + messages[messages.length - 1]);
                    }
                    else {
                        System.out.println("ReleaseUlordTransaction: Ulord tx successfully processed, Tx id: " + sendTxResponse);
                    }
                }
                else
                {
                    // TODO: Send Transaction for further signing
                }
            }
        }
        catch (Exception e) {
            System.out.println("ReleaseUlordTransaction: " + e);
        }
    }

    private static String addSignatureToUSC(Keccak256 uscTxHash, UldTransaction utTx, String federationChangeAuthorizedAddress) throws IOException {
        // Requires
        // 1. Federator Public Key  -   federationPublicKey
        // 2. Signatures    -   federation Signatures
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

        String res = UscRpc.sendTransaction(federationChangeAuthorizedAddress,
                PrecompiledContracts.BRIDGE_ADDR_STR,
                "0x3D0900", "0x9184e72a000",
                null,
                Hex.toHexString(function.encode(federationKey.getPubKey(), signatures, uscTxHash.getBytes())),
                null
        );
        return res;
    }

    private static String signRawTransaction(UldTransaction tx, BridgeConstants bridgeConstants, NetworkParameters params, String fedMultisigAddr)
            throws IOException, PrivateKeyNotFoundException {
        String txId = getUlordTxId(tx, params);
        int vout = getVout(txId, params, fedMultisigAddr);
        String scriptPubKey = getScriptPubKey(vout, txId, params);

        String[] privKeys = {getPrivateKey(bridgeConstants, params)};

        String signRawTxResponse = UlordCli.signRawTransaction(
                params,
                Hex.toHexString(tx.ulordSerialize()),
                txId,
                vout,
                scriptPubKey,
                Hex.toHexString(tx.getInput(0).getScriptSig().getChunks().get(2).data),
                privKeys, null
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

    private static int getVout(String txId, NetworkParameters params, String fedMultisigAddr) throws IOException {

        String getRawTxJSON = UlordCli.getRawTransaction(params, txId, true);

        JSONObject jsonObject = new JSONObject(getRawTxJSON);
        JSONArray voutObjects = jsonObject.getJSONArray("vout");

        int vout = 0;
        for(int i = 0; i < voutObjects.length(); ++i) {
            jsonObject = new JSONObject(voutObjects.get(i).toString());
            vout = Integer.parseInt(jsonObject.get("n").toString());
            JSONArray addressObjects = jsonObject.getJSONObject("scriptPubKey").getJSONArray("addresses");

            boolean found = false;
            for(int j = 0; j < addressObjects.length(); ++j) {
                if(addressObjects.get(j).toString().equals(fedMultisigAddr)) {
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

        List<UldECKey> publicKeys = bridgeConstants.getGenesisFederation().getPublicKeys();

        boolean privateKeyFound = false;
        String key = null;
        for(UldECKey pubKey : publicKeys) {

            key = UlordCli.dumpPrivKey(params, pubKey.toAddress(params).toString());
            if(!key.contains("error")) {
                privateKeyFound = true;
                federationKey = Utils.convertWifToPrivateKey(key, pubKey, params);
                break;
            }
        }
        if(privateKeyFound == false)
            throw new PrivateKeyNotFoundException();

        return key;
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
