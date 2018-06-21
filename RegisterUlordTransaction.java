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
import co.usc.config.BridgeTestNetConstants;
import co.usc.core.UscAddress;
import co.usc.peg.AddressBasedAuthorizer;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.core.PartialMerkleTree;
import co.usc.ulordj.core.UldBlock;
import co.usc.ulordj.core.UldTransaction;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterUlordTransaction {

    private static Logger logger = LoggerFactory.getLogger("registerulordtransaction");

//    public static void main(String[]args){
//        //registerUldTransaction <tx hex> <height> <merkletree>
//        register(BridgeTestNetConstants.getInstance(), "674f05e1916abc32a38f40aa67ae6b503b565999", "abcd1234", "29ae1e72a00cdc394e52fa8341f9270a63356ff30e66520bfa75d77a5a1216f2");
//    }

    public static boolean register(BridgeConstants bridgeConstants, String fedAddress, String pwd, String utTxId) {
        try {
            AddressBasedAuthorizer federationChangeAuthorizer = bridgeConstants.getFederationChangeAuthorizer();

            if (!federationChangeAuthorizer.isAuthorized(new UscAddress(fedAddress)))
                return false;

            // Try to unlock account
            if (!Utils.tryUnlockUscAccount(fedAddress, pwd)) {
                throw new PrivateKeyNotFoundException();
            }

            NetworkParameters params;

            if(bridgeConstants instanceof BridgeTestNetConstants)
                params = TestNet3Params.get();
            else
                params = MainNetParams.get();

            String getRawTransactionResponse = UlordCli.getRawTransaction(TestNet3Params.get(), utTxId, false);
            if(getRawTransactionResponse.contains("error")) {
                logger.info(getRawTransactionResponse);
                System.out.println(getRawTransactionResponse);
                return false;
            }
            UldTransaction tx = new UldTransaction(params, Hex.decode(getRawTransactionResponse));

            JSONObject getRawTxJSON = new JSONObject(UlordCli.getRawTransaction(TestNet3Params.get(), utTxId, true));
            String blockHash = getRawTxJSON.get("blockhash").toString();

            int height = Integer.parseInt(getRawTxJSON.get("height").toString());

            // Check if there is N block on top of the given transaction in ulord chain
            int blockCount = Integer.parseInt(UlordCli.getBlockCount(params));
            if((blockCount - height) < bridgeConstants.getUld2UscMinimumAcceptableConfirmations()) {
                System.out.println("No enough confirmations in Ulord for transaction: " + tx.getHash().toString());
                logger.info("No enough confirmations in Ulord for transaction: " + tx.getHash().toString());
                return false;
            }

            String getBlockResponse = UlordCli.getBlock(params, blockHash, false);
            if(getBlockResponse.contains("error")) {
                logger.info(getBlockResponse);
                System.out.println(getBlockResponse);
                return false;
            }
            UldBlock block = new UldBlock(params, Hex.decode(getBlockResponse));

            PartialMerkleTree partialMerkleTree = GenerateMerkleTree.buildMerkleBranch(block, tx.getHash());

            String data = DataEncoder.encodeRegisterUlordTransaction(tx.ulordSerialize(), height, partialMerkleTree.ulordSerialize());

            // TODO: Compute gasPrice, though it is a free transaction from genesis federation
            String txResult = UscRpc.sendTransaction(fedAddress, PrecompiledContracts.BRIDGE_ADDR_STR, "0x3D0900", "0x9184e72a000", null, data, null);
            logger.info(txResult);
            System.out.println("RegisterUlordTransaction: " + txResult);
            if(txResult.contains("error"))
                return false;
            return true;

        } catch (Exception e) {
            logger.error(e.toString());
            System.out.println(e);
            return false;
        }
    }
}
