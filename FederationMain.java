package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeMainNetConstants;
import co.usc.config.BridgeRegTestConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.RegTestParams;
import co.usc.ulordj.params.TestNet3Params;
import com.typesafe.config.Config;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static tools.Utils.*;

public class FederationMain implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("FederationMain");

    private BridgeConstants bridgeConstants;

    private String paramName;
    private String syncUlordHeadersAddress;
    private String syncUlordHeadersPassword;
    private String pegAddress;
    private String pegPassword;

    private boolean isSyncUlordHeadersEnabled;
    private boolean isPegEnabled;

    private HashMap<String, Boolean> processedTxs = new HashMap<>();

    public FederationMain(){
        FederationConfigLoader configLoader = new FederationConfigLoader();
        Config config = configLoader.getConfigFromFiles();

        this.paramName = config.getString("blockchain.config.name");
        if(this.paramName.equals("testnet")) {
            bridgeConstants = BridgeTestNetConstants.getInstance();
        }else if(this.paramName.equals("regtest")) {
            bridgeConstants = BridgeRegTestConstants.getInstance();
        }else {
            bridgeConstants = BridgeMainNetConstants.getInstance();
        }

        this.isSyncUlordHeadersEnabled = config.getString("federation.syncUlordHeaders.enabled").equals("true");
        if(this.isSyncUlordHeadersEnabled) {
            this.syncUlordHeadersAddress = config.getString("federation.syncUlordHeaders.address");
            this.syncUlordHeadersPassword = config.getString("federation.syncUlordHeaders.userPassword");
        }

        this.isPegEnabled = config.getString("federation.peg.enabled").equals("true");
        if(this.isPegEnabled) {
            this.pegAddress = config.getString("federation.peg.address");
            this.pegPassword = config.getString("federation.peg.userPassword");
        }
    }

    public static void main(String[]  args) {
        FederationMain fedMain = new FederationMain();

        if(fedMain.isPegEnabled) {
            Thread peg = new Thread(fedMain);
            peg.start();
        }

        if(fedMain.isSyncUlordHeadersEnabled) {
            if(fedMain.isPegEnabled) {
                try {
                    Thread.sleep(1000 * 30);  // Sleep for some time before we start second thread. This is to avoid
                    // sending two different transaction in the same time from the same account.
                    // We do this to avoid incorrect nonce value, if one transaction is rejected.
                } catch (Exception e) {
                }
            }

            Thread syncUlordHeaders = new Thread(new SyncUlordHeaders1(fedMain.bridgeConstants, fedMain.syncUlordHeadersAddress, fedMain.syncUlordHeadersPassword));
            syncUlordHeaders.start();
        }
    }

    @Override
    public void run() {
        startPeg(bridgeConstants, pegAddress, pegPassword);
    }


    public void startPeg(BridgeConstants bridgeConstants, String authorizedAddress, String pwd) {
        NetworkParameters params;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            params = TestNet3Params.get();
        else if(bridgeConstants instanceof BridgeRegTestConstants)
            params = RegTestParams.get();
        else
            params = MainNetParams.get();

        while(true) {

            String[] addresses;
            try {
                String res = UscRpc.getFederationAddress();
                if (res.contains("error")) {
                    throw new Exception("Failed to get Federation address: " + res);
                }

                addresses = new String[1];
                addresses[0] = DataDecoder.decodeGetFederationAddress(res);

                if(addresses == null) {
                    throw new Exception("No Federation address found.");
                }

                String getAddressUtxosResponse = UlordCli.getAddressUtxos(params, addresses);
                if(getAddressUtxosResponse.contains("error")) {
                    throw new Exception("Failed to get Federation address UTXOs " + getAddressUtxosResponse);
                }

                JSONArray jsonArray = new JSONArray(getAddressUtxosResponse);

                for ( String key : processedTxs.keySet() ) {
                    processedTxs.replace(key, true, false);
                }

                for (int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String txid = jsonObject.get("txid").toString();
                    int height = Integer.parseInt(jsonObject.get("height").toString());

                    // Check if the ulord transaction is already processed in USC
                    if(processedTxs.containsKey(txid)) {
                        // Mark tx as visited
                        processedTxs.replace(txid, false, true);
                        continue;
                    }

                    String data = DataEncoder.encodeIsUldTxHashAlreadyProcessed(txid);
                    String response = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data);
                    boolean processed = DataDecoder.decodeIsUldTxHashAlreadyProcessed(response);

                    if (processed) {
                        data = DataEncoder.encodeGetUldTxHashProcessedHeight(txid);
                        response = UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data);
                        int uscHeight = DataDecoder.decodeGetUldTxHashProcessedHeight(response);
                        processedTxs.put(txid, true);   // Mark Transaction as processed here as out side this we are still not sure if the transaction was processed successfully
                        logger.info("Ulord Transaction " + txid + " already processed in USC at height: " +  uscHeight );
                        continue;
                    }

                    response = UscRpc.getUldBlockChainBestChainHeight();
                    int chainHeadHeight = DataDecoder.decodeGetUldBlockChainBestChainHeight(response);
                    int minAcceptableConfirmationHeight = height + bridgeConstants.getUld2UscMinimumAcceptableConfirmations();
                    if (chainHeadHeight < minAcceptableConfirmationHeight) {
                        logger.info("Supplied Ulord transaction minimum acceptable confirmation height: " + minAcceptableConfirmationHeight + " is greater than Chainhead height " + chainHeadHeight);
                        continue;
                    }

                    // TODO: Transactions which do not spent from P2PKH and transactions who's value is less than the acceptable amount don't get registered in USC
                    // TODO: These transaction are stuck in the multisignature address
                    // TODO: The problem is since these transaction are not registered in USC, USC bridge doesn't create a return transaction
                    // TODO: Find a way to return these transactions back to the sender.

                    // Here we can register Ulord transactions in USC
                    if(!RegisterUlordTransaction.register(bridgeConstants, authorizedAddress, pwd, txid)) {
                        logger.warn("Failed to register transaction: " + txid);
                    }
                }

                Set<String> key = processedTxs.keySet();
                for (int i = 0; i < key.size(); i++) {
                    // Remove unvisited transactions as the UTXO of this transaction is spent.
                    processedTxs.remove(key, false);
                }

                // Try to unlock account
                Utils.tryUnlockUscAccount(authorizedAddress, pwd);

                // Update Collections Transaction
                UscRpc.sendTransaction(authorizedAddress,
                        PrecompiledContracts.BRIDGE_ADDR_STR,
                        "0x0",
                        getMinimumGasPrice(),
                        null,
                        DataEncoder.encodeUpdateCollections(),
                        null
                );

                // Try to release any pending transaction.
                ReleaseUlordTransaction.release(bridgeConstants, authorizedAddress, pwd);

            } catch (Exception e) {
                logger.error(e.toString());
            } finally {
                try {
                    if (params instanceof RegTestParams)
                        Thread.sleep(1000 * 30);
                    else
                        Thread.sleep((long) (1000 * 60 * 2.5));
                } catch (InterruptedException e) {
                    logger.error("Thread Interrupted: " + e);
                }
            }
        }
    }
}
