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

import java.util.*;

import static tools.Utils.*;

public class FederationMain implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("federation");

    private BridgeConstants bridgeConstants;

    private String paramName;
    private String feePerkbAuthorizedAddress;
    private String feePerkbAuthorizedPassword;
    private String changeAuthorizedAddress;
    private String changeAuthorizedPassword;
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


        this.feePerkbAuthorizedAddress = config.getString("federation.feePerkbAuthorizedAddress");
        this.feePerkbAuthorizedPassword = config.getString("federation.feePerkbAuthorizedPassword");

        this.changeAuthorizedAddress = config.getString("federation.changeAuthorizedAddress");
        this.changeAuthorizedPassword = config.getString("federation.changeAuthorizedPassword");

        this.isSyncUlordHeadersEnabled = config.getString("federation.syncUlordHeaders.enabled").equals("true");
        this.isPegEnabled = config.getString("federation.peg.enabled").equals("true");
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

            Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(fedMain.bridgeConstants, fedMain.feePerkbAuthorizedAddress, fedMain.feePerkbAuthorizedPassword));
            syncUlordHeaders.start();
        }
    }

    @Override
    public void run() {
        startPeg(bridgeConstants, changeAuthorizedAddress, changeAuthorizedPassword);
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
                    logger.error(res);
                    System.out.println("getAddressUtxosResponse: " + res);
                    return;
                }
                addresses = new String[1];
                addresses[0] = DataDecoder.decodeGetFederationAddress(res);

                if(addresses == null) {
                    logger.error("No Federation address found.");
                    System.out.println("No Federation address found.");
                    return;
                }

                String getAddressUtxosResponse = UlordCli.getAddressUtxos(params, addresses);
                if(getAddressUtxosResponse.contains("error")) {
                    logger.error(getAddressUtxosResponse);
                    System.out.println(getAddressUtxosResponse);
                    return;
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
                    JSONObject jsObj = new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                    logger.info(jsObj.toString());
                    String result = jsObj.get("result").toString();

                    if (result.substring(result.length() - 1, result.length()).equals("1")) {
                        data = DataEncoder.encodeGetUldTxHashProcessedHeight(txid);
                        jsObj = new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                        processedTxs.put(txid, true);   // Mark Transaction as processed here as out side this we are still not sure if the transaction was processed successfully
                        logger.info("Transaction " + txid + " already processed at height: " +  Long.parseLong(jsObj.getString("result").substring(2),16));
                        System.out.println("Transaction " + txid + " already processed at height: " +  Long.parseLong(jsObj.getString("result").substring(2),16));
                        continue;
                    }

                    JSONObject jsonObj = new JSONObject(UscRpc.getUldBlockChainBestChainHeight());
                    int chainHeadHeight = Integer.decode(jsonObj.get("result").toString());
                    int minAcceptableConfirmationHeight = height + bridgeConstants.getUld2UscMinimumAcceptableConfirmations();
                    if (chainHeadHeight < minAcceptableConfirmationHeight) {
                        logger.info("Supplied transaction minimum acceptable confirmation height: " + minAcceptableConfirmationHeight + " is greater than Chainhead height " + chainHeadHeight);
                        System.out.println("Supplied transaction minimum acceptable confirmation height: " + minAcceptableConfirmationHeight + " is greater than Chainhead height " + chainHeadHeight);
                        continue;
                    }

                    // Here we can register Ulord transactions in USC
                    //logger.info("Transaction " + txid + " sent to USC");
                    if(!RegisterUlordTransaction.register(bridgeConstants, authorizedAddress, pwd, txid)) {
                        logger.warn("Failed to register transaction: " + txid);
                        System.out.println("Failed to register transaction: " + txid);
                    }
                }

                for ( String key : processedTxs.keySet() ) {
                    // Remove unvisited transactions as the UTXO of this transaction is spent.
                    processedTxs.remove(key, false);
                }

                // Try to unlock account
                Utils.tryUnlockUscAccount(authorizedAddress, pwd);

                String sendTxResponse = UscRpc.sendTransaction(authorizedAddress,
                        PrecompiledContracts.BRIDGE_ADDR_STR,
                        "0x0",
                        getMinimumGasPrice(),
                        null,
                        DataEncoder.encodeUpdateCollections(),
                        null
                );
                logger.info(sendTxResponse);
                System.out.println("UpdateCollections: " + sendTxResponse);

                // Try to release any pending transaction.
                ReleaseUlordTransaction.release(bridgeConstants, authorizedAddress, pwd);

                if(params instanceof RegTestParams)
                    Thread.sleep(1000 * 60 );
                else
                    Thread.sleep(1000 * 60 * 5);
            } catch (Exception e) {
                logger.warn(e.toString());
                System.out.println("FederationMain: " + e);
                break;
            }
        }
    }
}
