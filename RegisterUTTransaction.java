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
import co.usc.config.UscSystemProperties;
import co.usc.core.UscAddress;
import co.usc.peg.*;
import co.usc.ulordj.core.*;
import co.usc.ulordj.store.BlockStoreException;
import co.usc.ulordj.store.UldBlockStore;
import org.ethereum.config.DefaultConfig;
import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
/*
 * Set VM Option to
 * -Dusc.conf.file=<federation-config-file>.conf
 */

@Component
public class RegisterUTTransaction {

    Repository repository;
    UscSystemProperties config;
    BridgeConstants bridgeConstants;
    BridgeStorageProvider provider;

    private enum StorageFederationReference { NONE, NEW, OLD, GENESIS }

    @Autowired
    RegisterUTTransaction(Repository repository,
                          UscSystemProperties config) throws BlockStoreException {
        this.repository = repository;
        this.config = config;

        this.bridgeConstants = this.config.getBlockchainConfig().getCommonConstants().getBridgeConstants();
        provider = new BridgeStorageProvider(repository, new UscAddress(PrecompiledContracts.BRIDGE_ADDR_STR), config.getBlockchainConfig().getCommonConstants().getBridgeConstants());
    }

    public static void main(String[]args){
        ApplicationContext ctx = new AnnotationConfigApplicationContext(DefaultConfig.class);
        RegisterUTTransaction runner = ctx.getBean(RegisterUTTransaction.class);
        runner.registerUTTransaction(args);
    }

    public void registerUTTransaction(String[]args){
        Federation federation = getActiveFederation();
        List<UldECKey> publicKeys = federation.getPublicKeys();
        //TODO: Check if transaction is initiated for the multisig address on ulord.

        try {
            getUTBlocks(args);
        }catch (IOException ex){

        }catch(Exception ex){

        }
        //String []args = new String[]{""};
        //Encode_eth_call.getRegisterUldTransactionString(args);
    }

    public void getUTBlocks(String []args) throws IOException {
        StringBuilder getBlockCount = new StringBuilder();
        StringBuilder getBlockHash = new StringBuilder();
        StringBuilder getBlock = new StringBuilder();

        getBlockCount.append(NetworkConstants.ULORD_CLI);
        getBlockHash.append(NetworkConstants.ULORD_CLI);
        getBlock.append(NetworkConstants.ULORD_CLI);

        if (args.length > 0 && args[0].equals("testnet")) {
            getBlockCount.append(NetworkConstants.ULORD_TESTNET);
            getBlockHash.append(NetworkConstants.ULORD_TESTNET);
            getBlock.append(NetworkConstants.ULORD_TESTNET);
        }

        getBlockCount.append(" getblockcount");
        getBlockHash.append(" getblockhash");
        getBlock.append(" getblock");

        Process proc = null;
        Runtime rt = Runtime.getRuntime();

        //Get the ulord best block height.
        int bestBlockHeight = getBestBlockHeight(getBlockCount.toString());

        //Get Block hash of the best block in ulord blockchain
        String line = getBlockHashByHeight(getBlockHash.toString() + " " + bestBlockHeight);

        //Get Block from ulord blockchain using the block hash.
        String blockAsJsonString = getBlockAsJsonString(getBlock.toString() + " " + line);

        JSONObject json = new JSONObject(blockAsJsonString);
        JSONArray tx = json.getJSONArray("tx");
        System.out.println(tx.get(0));
        System.out.println(json.getString("previousblockhash"));
    }

    public int getBestBlockHeight(String getBlockCount) throws IOException{
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(getBlockCount);

        InputStream inStr = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(inStr);
        BufferedReader br = new BufferedReader(isr);
        return Integer.parseInt(br.readLine());
    }

    public String getBlockHashByHeight(String getBlockHash) throws IOException{
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(getBlockHash);
        InputStream inStr = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(inStr);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        return line;
    }

    public String getBlockAsJsonString(String getBlock) throws IOException{
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(getBlock);
        InputStream inStr = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(inStr);
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        String blockAsJsonString = "";
        while(line != null){
            blockAsJsonString += line;
            line = br.readLine();
        }
        return blockAsJsonString;
    }

    /**
     * Returns the currently active federation.
     * See getActiveFederationReference() for details.
     * @return the currently active federation.
     */
    public Federation getActiveFederation() {
        switch (getActiveFederationReference()) {
            case NEW:
                return provider.getNewFederation();
            case OLD:
                return provider.getOldFederation();
            case GENESIS:
            default:
                return bridgeConstants.getGenesisFederation();
        }
    }

    /**
     * Returns the currently active federation reference.
     * Logic is as follows:
     * When no "new" federation is recorded in the blockchain, then return GENESIS
     * When a "new" federation is present and no "old" federation is present, then return NEW
     * When both "new" and "old" federations are present, then
     * 1) If the "new" federation is at least bridgeConstants::getFederationActivationAge() blocks old,
     * return the NEW
     * 2) Otherwise, return OLD
     * @return a reference to where the currently active federation is stored.
     */
    private StorageFederationReference getActiveFederationReference() {
        Federation newFederation = provider.getNewFederation();

        // No new federation in place, then the active federation
        // is the genesis federation
        if (newFederation == null) {
            return StorageFederationReference.GENESIS;
        }

        Federation oldFederation = provider.getOldFederation();

        // No old federation in place, then the active federation
        // is the new federation
        if (oldFederation == null) {
            return StorageFederationReference.NEW;
        }

        // Both new and old federations in place
        // If the minimum age has gone by for the new federation's
        // activation, then that federation is the currently active.
        // Otherwise, the old federation is still the currently active.
        if (shouldFederationBeActive(newFederation)) {
           return StorageFederationReference.NEW;
        }

        return StorageFederationReference.OLD;
    }

    private boolean shouldFederationBeActive(Federation federation) {
        //long federationAge = uscExecutionBlock.getNumber() - federation.getCreationBlockNumber();
        //return federationAge >= bridgeConstants.getFederationActivationAge();
        return true;
    }
}
