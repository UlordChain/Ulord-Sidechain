/*
 * This file is part of Usc
 * Copyright (c) 2016 - 2018 Ulord development team.
 */

package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.crypto.Keccak256;
import co.usc.peg.Bridge;
import co.usc.peg.BridgeSerializationUtils;
import co.usc.peg.Federation;
import co.usc.ulordj.core.*;
import co.usc.ulordj.crypto.TransactionSignature;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;
import co.usc.ulordj.script.Script;
import co.usc.ulordj.script.ScriptBuilder;
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

    private static Logger logger = LoggerFactory.getLogger("Federation");

    public static void release(BridgeConstants bridgeConstants, String federationAuthorizedAddress, String pwd, String gas, String gasPrice) {

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

            if(uscTxsWaitingForSignatures.isEmpty())
                return;

            // Try to unlock account
            if (!Utils.tryUnlockUscAccount(federationAuthorizedAddress, pwd)) {
                throw new PrivateKeyNotFoundException();
            }

            for (Map.Entry<Keccak256, UldTransaction> entry : uscTxsWaitingForSignatures.entrySet()) {
                Keccak256 uscTxHash = entry.getKey();
                UldTransaction utTx = entry.getValue();


                // Check if any vin spends from retiring federation to new federation
//                List<TransactionInput> inputs = utTx.getInputs();
//                boolean isFromReitingToActiveFederation = false;
//                for (TransactionInput input : inputs) {
//
//                    long index = input.getOutpoint().getIndex();
//
//                    Sha256Hash prevTxHash = input.getOutpoint().getHash();
//
//                    String rawTransaction = UlordCli.getRawTransaction(params, prevTxHash.toString(), false);
//
//                    UldTransaction prevTx = new UldTransaction(params, Hex.decode(rawTransaction));
//
//                    Address fromAddress = prevTx.getOutput(index).getAddressFromP2SH(params);
//
//                    List<TransactionOutput> outputs = utTx.getOutputs();
//                    Address toAddress = null;
//                    for (TransactionOutput output : outputs) {
//                         toAddress = output.getAddressFromP2SH(params);
//                    }
//                    if(fromAddress != null && toAddress != null) {
//                        isFromReitingToActiveFederation = true;
//                        logger.warn("Transaction is a migration Transaction");
//                    }
//                }
//
//                if(isFromReitingToActiveFederation)
//                    continue;

                // Check there are at least N blocks on top of the supplied transaction
                if (!validateTxDepth(uscTxHash, bridgeConstants)) {
                    continue;
                }

                List<UldECKey> privateKeys = getPrivateKeys(params, bridgeConstants);

                procesSigning(params, privateKeys, utTx, uscTxHash, federationAuthorizedAddress, gas, gasPrice);

            }
        }
        catch (Exception e) {
            logger.error("Exception in ReleaseUlordTransaction: " + e.toString());
        }
    }

    private static void procesSigning(NetworkParameters params, List<UldECKey> keys, UldTransaction utTx, Keccak256 uscTxHash, String federationAuthorizedAddress, String gas, String gasPrice) {

        for (int k = 0; k < keys.size(); k++) {

            List<Sha256Hash> sighashes = new ArrayList<>();
            List<byte[]> signatures = new ArrayList<>();
            List<TransactionSignature> txSigs = new ArrayList<>();
            int numInputs = utTx.getInputs().size();

            for (int i = 0; i < numInputs; ++i) {
                TransactionInput txIn = utTx.getInput(i);
                Script inputScript = txIn.getScriptSig();
                List<ScriptChunk> chunks = inputScript.getChunks();
                byte[] program = chunks.get(chunks.size() - 1).data;
                Script reedemScript = new Script(program);
                Sha256Hash sigHash = utTx.hashForSignature(i, reedemScript, UldTransaction.SigHash.ALL, false);
                byte[] sig = keys.get(k).sign(sigHash).encodeToDER();
                signatures.add(sig);
                sighashes.add(sigHash);
            }

            // verify signatures
            for (int i = 0; i < numInputs; i++) {
                UldECKey.ECDSASignature sig;
                try {
                    sig = UldECKey.ECDSASignature.decodeFromDER(signatures.get(i));
                } catch (RuntimeException e) {
                    logger.warn("Malformed signature for input {} of tx {}: {}", i, uscTxHash, Hex.toHexString(signatures.get(i)));
                    return;
                }

                Sha256Hash sighash = sighashes.get(i);

                if (!keys.get(k).verify(sighash, sig)) {
                    logger.warn("Signature {} {} is not valid for hash {} and public key {}", i, Hex.toHexString(sig.encodeToDER()), sighash, Hex.toHexString(keys.get(i).getPubKey()));
                    return;
                }

                TransactionSignature txSig = new TransactionSignature(sig, UldTransaction.SigHash.ALL, false);
                txSigs.add(txSig);
                if (!txSig.isCanonical()) {
                    logger.warn("Signature {} {} is not canonical.", i, Hex.toHexString(signatures.get(i)));
                    return;
                }
            }

            // All signatures are correct. Proceed to signing
            for (int i = 0; i < utTx.getInputs().size(); i++) {
                Sha256Hash sighash = sighashes.get(i);
                TransactionInput input = utTx.getInput(i);
                Script inputScript = input.getScriptSig();

                boolean alreadySignedByThisFederator = isInputSignedByThisFederator(keys.get(k), sighash, input);

                if(!alreadySignedByThisFederator) {
                    try {
                        int sigIndex = inputScript.getSigInsertionIndex(sighash, keys.get(k));
                        inputScript = ScriptBuilder.updateScriptWithSignature(inputScript, txSigs.get(i).encodeToUlord(), sigIndex, 1, 1);
                        input .setScriptSig(inputScript);
                        if(!sendTx(signatures, keys.get(k), uscTxHash, federationAuthorizedAddress, gas, gasPrice, 3)) {
                            logger.debug("addSignature transaction failed");
                            return;
                        }
                        logger.debug("Tx input {} for tx {} signed.", i, uscTxHash);
                    } catch (IllegalStateException e) {
                        logger.debug("Failed to sign transaction. Wrong Federation is trying to sign the transaction");
                    } catch (InterruptedException ie) {
                        logger.error("Thread interrupt exception " + ie);
                    } catch (IOException ex) {
                        logger.error("Error in JSON format " + ex);
                    }
                } else {
                    logger.warn("Input {} of tx {} already signed by this federator.", i, uscTxHash);
                    break;
                }
            }

            try {
                // Transaction Fully signed
                if(hasEnoughSignatures(utTx)) {
                    logger.info("Tx fully signed {}. Hex: {}", utTx, Hex.toHexString(utTx.ulordSerialize()));

                    // Broadcast Ulord release transaction
                    String res = UlordCli.sendRawTransaction(params, Hex.toHexString(utTx.ulordSerialize()));
                    logger.debug("Ulord Transaction Broadcasted txId: {}",  res);
                    break;
                } else {
                    // Add the signature to Ulord Transaction
                    logger.debug("Tx not yet fully signed {}", uscTxHash);
                }
            } catch (IOException ex) {
                logger.error("getMinimumGasPrice error: " + ex);
            }
        }
    }

    private static boolean sendTx(List<byte[]> signatures,
                                  UldECKey key,
                                  Keccak256 uscTxHash,
                                  String federationAuthorizedAddress,
                                  String gas,
                                  String gasPrice,
                                  int tries)
            throws IOException, InterruptedException {
        if (tries <= 0)
            return false;

        String sendTransactionResponse = UscRpc.sendTransaction(federationAuthorizedAddress,
                PrecompiledContracts.BRIDGE_ADDR_STR,
                gas,
                gasPrice,
                null,
                Hex.toHexString(Bridge.ADD_SIGNATURE.encode(key.getPubKey(), signatures, uscTxHash.getBytes())),
                null
        );
        JSONObject jsonObject = new JSONObject(sendTransactionResponse);
        String txId = jsonObject.get("result").toString();

        logger.info("Add Signature tx id: {}", txId);
        Thread.sleep(1000 * 15);

        while (!Utils.isTransactionMined(txId)) {
            Thread.sleep(1000 * 15); // Sleep to stop flooding rpc requests.
            if (!Utils.isTransactionMined(txId)) // Check again because the transaction might have been mined after 15 seconds
                if (!Utils.isTransactionInMemPool(txId))
                    if(!sendTx(signatures, key, uscTxHash, federationAuthorizedAddress, gas, gasPrice, --tries))
                        return false;
        }
        return true;
    }

    private static boolean hasEnoughSignatures(UldTransaction utTx) {
        // When the tx is constructed OP_0 are placed where signature should go.
        // Check all OP_0 have been replaced with actual signatures in all inputs
        for (TransactionInput input : utTx.getInputs()) {
            Script scriptSig = input.getScriptSig();
            List<ScriptChunk> chunks = scriptSig.getChunks();
            for (int i = 1; i < chunks.size(); i++) {
                ScriptChunk chunk = chunks.get(i);
                if (!chunk.isOpCode() && chunk.data.length == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isInputSignedByThisFederator(UldECKey federatorKey, Sha256Hash sighash, TransactionInput input) {
        List<ScriptChunk> chunks = input.getScriptSig().getChunks();
        for (int j = 1; j < chunks.size() - 1; j++) {
            ScriptChunk chunk = chunks.get(j);

            if (chunk.data.length == 0) {
                continue;
            }

            TransactionSignature sig2 = TransactionSignature.decodeFromUlord(chunk.data, false, false);

            if (federatorKey.verify(sighash, sig2)) {
                return true;
            }
        }
        return false;
    }

    private static List<UldECKey> getPrivateKeys(NetworkParameters params, BridgeConstants bridgeConstants)
            throws IOException, PrivateKeyNotFoundException {

        int fedSize = 0;

        // Get Retiring Federation PublicKeys
        fedSize = DataDecoder.decodeGetRetiringFederationSize(UscRpc.getRetiringFederationSize());
        List<UldECKey> publicKeys = new ArrayList<>();

        for (int i = 0; i < fedSize; i++) {
            String res = UscRpc.getRetiringFederatorPublicKey(i);
            String publicKey = DataDecoder.decodeGetRetiringFederatorPublicKey(res);
            publicKeys.add(UldECKey.fromPublicOnly(Hex.decode(publicKey)));
        }

        // Get Active Federation PublicKeys
        fedSize = DataDecoder.decodeGetFederationSize(UscRpc.getFederationSize());

        for (int i = 0; i < fedSize; i++) {
            String response = new JSONObject(UscRpc.getFederatorPublicKey(i)).getString("result");
            String pubKey = DataDecoder.decodeGetFederatorPublicKey(response);
            publicKeys.add(UldECKey.fromPublicOnly(Hex.decode(pubKey)));
        }

        // Get PrivateKeys
        boolean privateKeyFound = false;
        List<UldECKey> keys = new ArrayList<>();
        for (int i = 0; i < publicKeys.size(); i++) {
            String res = UlordCli.dumpPrivKey(params, publicKeys.get(i).toAddress(params).toString());
            if(!res.contains("error")) {
                privateKeyFound = true;
                keys.add(Utils.convertWifToPrivateKey(res, params));
            }
        }

        if(privateKeyFound == false)
            throw new PrivateKeyNotFoundException();

        return keys;
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
