package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FederationMain implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("federation");

    //TODO: Move those settings to a config file
    BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
    NetworkParameters params = TestNet3Params.get();

    String[] ulordFederationAddress;
    String federationChangeAuthorizedAddress;
    String pwd;

    public FederationMain(){
        FederationConfigLoader configLoader = new FederationConfigLoader();
        Config config = configLoader.getConfigFromFiles();
        ulordFederationAddress = config.getStringList("federation.address").toArray(new String[0]);
        federationChangeAuthorizedAddress = config.getString("federation.changeAuthorizedAddress");
        pwd = config.getString("federation.password");
    }

    public static void main(String[]  args) {
        FederationMain fedMain = new FederationMain();
        Thread registerUlordTransactions = new Thread(fedMain);
        registerUlordTransactions.start();

        Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(fedMain.params));
        syncUlordHeaders.start();

        Thread releaseUlordTx = new Thread(new ReleaseUlordTransaction(fedMain.bridgeConstants));
        releaseUlordTx.start();
    }

    @Override
    public void run() {
        monitorUtxosAndRegisterUlordTx(bridgeConstants, federationChangeAuthorizedAddress,pwd, ulordFederationAddress);
    }


    public void monitorUtxosAndRegisterUlordTx(BridgeConstants bridgeConstants, String fromFedAddress, String pwd, String[] address) {
        NetworkParameters params;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            params = TestNet3Params.get();
        else
            params = MainNetParams.get();

        try {
            while(true) {
                JSONArray jsonArray  = new JSONArray(UlordCli.getAddressUtxos(params, address));
                for(int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String txid = jsonObject.get("txid").toString();
                    int height = Integer.parseInt(jsonObject.get("height").toString());

                    // Check if the ulord transaction is already processed in USC
                    String data = DataEncoder.encodeIsUldTxHashAlreadyProcessed(txid);
                    JSONObject jsObj = new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                    logger.info(jsObj.toString());
                    String result = jsObj.get("result").toString();

                    if (result.substring(result.length() - 1, result.length()).equals("1")) {
                        logger.info("Tx already processed: " + txid);
                        System.out.println("Tx already processed: " + txid);
                        return;
                    }

                    JSONObject jsonObj = new JSONObject(UscRpc.getUldBlockChainBestChainHeight());
                    int chainHeadHeight = Integer.decode(jsonObj.get("result").toString());
                    if(chainHeadHeight < height + bridgeConstants.getUld2UscMinimumAcceptableConfirmations()) {
                        logger.info("Chainhead height is less than supplied transaction's height");
                        System.out.println("Chainhead height is less than supplied transaction's height");
                        continue;
                    }

                    // Here we can register Ulord transactions in USC
                    logger.info("Transaction " + txid + " sent to USC");
                    RegisterUlordTransaction.register(bridgeConstants, fromFedAddress, pwd, txid);
                }
                Thread.sleep(1000 * 60 * 5);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
