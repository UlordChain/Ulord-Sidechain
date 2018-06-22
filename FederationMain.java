package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import com.typesafe.config.Config;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FederationMain implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("federation");

    BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
    NetworkParameters params = TestNet3Params.get();
    Config config;
    String[] ulordFederationAddress;
    String federationChangeAuthorizedAddress;
    String federationChangeAuthorizedPassword;

    public FederationMain(){
        FederationConfigLoader configLoader = new FederationConfigLoader();
        config = configLoader.getConfigFromFiles();
        this.ulordFederationAddress = config.getStringList("federation.address").toArray(new String[0]);
        this.federationChangeAuthorizedAddress = config.getString("federation.changeAuthorizedAddress");
        this.federationChangeAuthorizedPassword = config.getString("federation.changeAuthorizedPassword");
    }

    public static void main(String[]  args) {
        FederationMain fedMain = new FederationMain();
        Thread registerUlordTransactions = new Thread(fedMain);
        registerUlordTransactions.start();

        Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(fedMain.params, fedMain.config));
        syncUlordHeaders.start();
    }

    @Override
    public void run() {
        monitorUtxosAndRegisterUlordTx(bridgeConstants, federationChangeAuthorizedAddress, federationChangeAuthorizedPassword, ulordFederationAddress);
    }


    public void monitorUtxosAndRegisterUlordTx(BridgeConstants bridgeConstants, String federationChangeAuthorizedAddress, String pwd, String[] addresses) {
        NetworkParameters params;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            params = TestNet3Params.get();
        else
            params = MainNetParams.get();


        while(true) {
            try {
                JSONArray jsonArray = new JSONArray(UlordCli.getAddressUtxos(params, addresses));
                for (int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String txid = jsonObject.get("txid").toString();
                    int height = Integer.parseInt(jsonObject.get("height").toString());
                    String fedMultisigAddr = jsonObject.get("address").toString();

                    // Check if the ulord transaction is already processed in USC
                    String data = DataEncoder.encodeIsUldTxHashAlreadyProcessed(txid);
                    JSONObject jsObj = new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                    logger.info(jsObj.toString());
                    String result = jsObj.get("result").toString();

                    if (result.substring(result.length() - 1, result.length()).equals("1")) {
                        logger.info("Transaction " + txid + " already processed");
                        System.out.println("Tx already processed: " + txid);
                        // If already processed, try release.
                        ReleaseUlordTransaction.release(bridgeConstants, federationChangeAuthorizedAddress, pwd, fedMultisigAddr);
                        continue;
                    }

                    JSONObject jsonObj = new JSONObject(UscRpc.getUldBlockChainBestChainHeight());
                    int chainHeadHeight = Integer.decode(jsonObj.get("result").toString());
                    if (chainHeadHeight < height + bridgeConstants.getUld2UscMinimumAcceptableConfirmations()) {
                        logger.info("Chainhead height " + chainHeadHeight + " is less than supplied transaction's height " + height);
                        System.out.println("Chainhead height is less than supplied transaction's height");
                        continue;
                    }

                    // Here we can register Ulord transactions in USC
                    logger.info("Transaction " + txid + " sent to USC");
                    RegisterUlordTransaction.register(bridgeConstants, federationChangeAuthorizedAddress, pwd, txid);
                }
                Thread.sleep(1000 * 60 * 5);
            } catch (Exception e) {
                logger.warn(e.toString());
                System.out.println("FederationMain: " + e);
            }
        }
    }
}
