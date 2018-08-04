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

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.Usc;
import co.usc.core.UscAddress;
import co.usc.core.Wallet;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.peg.BridgeUtils;
import co.usc.peg.Federation;
import co.usc.ulordj.core.*;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;
import co.usc.ulordj.script.Script;
import com.google.common.collect.Lists;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static tools.Utils.getMinimumGasPrice;

public class RegisterUlordTransaction {

    private static Logger logger = LoggerFactory.getLogger("Federation");

    public static boolean register(BridgeConstants bridgeConstants, String authorizedAddress, String pwd, String gas, String gasPrice, String utTxId) {
        try {

            // Try to unlock account
            if (!Utils.tryUnlockUscAccount(authorizedAddress, pwd)) {
                throw new PrivateKeyNotFoundException();
            }

            NetworkParameters params;

            if(bridgeConstants instanceof BridgeTestNetConstants)
                params = TestNet3Params.get();
            else if(bridgeConstants instanceof BridgeRegTestConstants)
                params = RegTestParams.get();
            else
                params = MainNetParams.get();

            String getRawTransactionResponse = UlordCli.getRawTransaction(params, utTxId, false);
            if(getRawTransactionResponse.contains("error")) {
                logger.info("Failed to get raw transaction: " + getRawTransactionResponse);
                return false;
            }
            UldTransaction tx = new UldTransaction(params, Hex.decode(getRawTransactionResponse));

            if (isLockTx(tx, getLiveFederations(params), bridgeConstants)) {
                // Taken from BridgeSupport.
                // Check if the First input spends a P2PKH
                // If not then the transaction is going to get rejected in BridgeSupport.
                Script scriptSig = tx.getInput(0).getScriptSig();
                if (scriptSig.getChunks().size() != 2) {
                    logger.warn("[uldlock:{}] First input does not spend a Pay-to-PubkeyHash " + tx.getInput(0), tx.getHash());
                    return false;
                }
            }

            JSONObject getRawTxJSON = new JSONObject(UlordCli.getRawTransaction(params, utTxId, true));
            String blockHash = getRawTxJSON.get("blockhash").toString();

            int height = Integer.parseInt(getRawTxJSON.get("height").toString());

            // Check if there is N block on top of the given transaction in ulord chain
            int blockCount = Integer.parseInt(UlordCli.getBlockCount(params));
            if((blockCount - height) < bridgeConstants.getUld2UscMinimumAcceptableConfirmations()) {
                logger.info("No enough confirmations in Ulord for transaction: " + tx.getHash().toString());
                return false;
            }

            String getBlockResponse = UlordCli.getBlock(params, blockHash, false);
            if(getBlockResponse.contains("error")) {
                logger.info("Failed to get Ulord block: "  + getBlockResponse);
                return false;
            }
            UldBlock block = new UldBlock(params, Hex.decode(getBlockResponse));

            PartialMerkleTree partialMerkleTree = GenerateMerkleTree.buildMerkleBranch(params, block, tx.getHash());

            String data = DataEncoder.encodeRegisterUlordTransaction(tx.ulordSerialize(), height, partialMerkleTree.ulordSerialize());

            return sendTx(authorizedAddress, gas, gasPrice, utTxId, data, 3);

        } catch (Exception e) {
            logger.error("Exception in RegisterUlordTransaction" + e.toString());
            return false;
        }
    }

    private static boolean scriptCorrectlySpendsTx(UldTransaction tx, int index, Script script) {
        try {
            TransactionInput txInput = tx.getInput(index);
            txInput.getScriptSig().correctlySpends(tx, index, script, Script.ALL_VERIFY_FLAGS);
            return true;
        } catch (ScriptException se) {
            return false;
        }
    }

    private static boolean isLockTx(UldTransaction tx, List<Federation> federations, BridgeConstants bridgeConstants) {
        // First, check tx is not a typical release tx (tx spending from the any of the federation addresses and
        // optionally sending some change to any of the federation addresses)
        for (int i = 0; i < tx.getInputs().size(); i++) {
            final int index = i;
            if (federations.stream().anyMatch(federation -> scriptCorrectlySpendsTx(tx, index, federation.getP2SHScript()))) {
                return false;
            }
        }
        return true;
    }

    private static List<Federation> getLiveFederations(NetworkParameters params) throws IOException {
        List<Federation> liveFederations = new ArrayList<>();

        // Get Retiring Federation
        String response = UscRpc.getRetiringFederationSize();
        int fedSize = DataDecoder.decodeGetRetiringFederationSize(response);
        if(fedSize > 0)
        {
            List<UldECKey> fedeationPublicKeys = getRetiringFederationPublicKeys(fedSize);

            response = UscRpc.getRetiringFederationCreationTime();
            Long time = DataDecoder.decodeGetRetiringFederationCreationTime(response);
            Instant federationCreatedAt = Instant.ofEpochMilli(time);

            response = UscRpc.getRetiringFederationCreationBlockNumber();
            Long federationCreationBlockNumber = DataDecoder.decodeGetRetiringFederationCreationBlockNumber(response);

            liveFederations.add(
                    new Federation(
                            fedeationPublicKeys,
                            federationCreatedAt,
                            federationCreationBlockNumber,
                            params
                    )
            );
        }

        // Get Active Federation
        response = UscRpc.getFederationSize();
        fedSize = DataDecoder.decodeGetFederationSize(response);
        if(fedSize > 0)
        {
            List<UldECKey> avtiveFedeationPublicKeys = getFederationPublicKeys(fedSize);

            response = UscRpc.getFederationCreationTime();
            Long time = DataDecoder.decodeGetFederationCreationTime(response);
            Instant federationCreatedAt = Instant.ofEpochMilli(time);

            response = UscRpc.getFederationCreationBlockNumber();
            Long federationCreationBlockNumber = DataDecoder.decodeGetFederationCreationBlockNumber(response);

            liveFederations.add(
                    new Federation(
                            avtiveFedeationPublicKeys,
                            federationCreatedAt,
                            federationCreationBlockNumber,
                            params
                    )
            );
        }

        return liveFederations;
    }

    private static List<UldECKey> getFederationPublicKeys(int fedSize) throws IOException {
        List<UldECKey> activeFederation = new ArrayList<>();
        for (int i = 0; i < fedSize; i++) {
            String response = new JSONObject(UscRpc.getFederatorPublicKey(i)).getString("result");
            String pubKey = DataDecoder.decodeGetFederatorPublicKey(response);
            activeFederation.add(UldECKey.fromPublicOnly(Hex.decode(pubKey)));
        }
        return activeFederation;
    }

    private static List<UldECKey> getRetiringFederationPublicKeys(int fedSize) throws IOException {
        List<UldECKey> retiringFederation = new ArrayList<>();
        for (int i = 0; i < fedSize; i++) {
            String response = new JSONObject(UscRpc.getRetiringFederatorPublicKey(i)).getString("result");
            String pubKey = DataDecoder.decodeGetRetiringFederatorPublicKey(response);
            retiringFederation.add(UldECKey.fromPublicOnly(Hex.decode(pubKey)));
        }
        return retiringFederation;
    }

    private static boolean sendTx(String changeAuthorizedAddress, String gas, String gasPrice, String utTxId,String data, int tries) throws IOException, InterruptedException {

        if (tries == 0)
            return false;

        String sendTransactionResponse = UscRpc.sendTransaction(changeAuthorizedAddress, PrecompiledContracts.BRIDGE_ADDR_STR, gas, gasPrice, null, data, null);
        logger.info(sendTransactionResponse);
        JSONObject jsonObject = new JSONObject(sendTransactionResponse);
        if(jsonObject.toString().contains("error")) {
            logger.error("Error in send Transaction: " + sendTransactionResponse);
            return false;
        }
        String txId = jsonObject.get("result").toString();

        logger.info("Registering Ulord Transaction {}, Usc Tx ID: {}", utTxId, txId);

        Thread.sleep(1000 * 15);

        while (!Utils.isTransactionMined(txId)) {
            Thread.sleep(1000 * 15); // Sleep to stop flooding rpc requests.
            if (!Utils.isTransactionMined(txId)) // Check again because the transaction might have been mined after 15 seconds
                if (!Utils.isTransactionInMemPool(txId))
                    if(!sendTx(changeAuthorizedAddress, gas, gasPrice, utTxId, data, --tries))
                        return false;
        }
        return true;
    }
}
