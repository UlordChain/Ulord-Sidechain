package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.peg.Bridge;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import com.typesafe.config.Config;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class FederationMain implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("federation");

    BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
    NetworkParameters params = TestNet3Params.get();
    Config config;
    String[] ulordFederationAddress;
    String feePerkbAuthorizedAddress;
    String feePerkbAuthorizedPassword;
    String changeAuthorizedAddress;
    String changeAuthorizedPassword;
    boolean isSyncUlordHeadersEnabled;
    boolean isPegEnabled;

    public FederationMain(){
        FederationConfigLoader configLoader = new FederationConfigLoader();
        config = configLoader.getConfigFromFiles();

        this.ulordFederationAddress = config.getStringList("federation.addresses").toArray(new String[0]);

        this.feePerkbAuthorizedAddress = config.getString("federation.feePerkbAuthorizedAddress");
        this.feePerkbAuthorizedPassword = config.getString("federation.feePerkbAuthorizedPassword");

        this.changeAuthorizedAddress = config.getString("federation.changeAuthorizedAddress");
        this.changeAuthorizedPassword = config.getString("federation.changeAuthorizedPassword");

        this.isSyncUlordHeadersEnabled = config.getString("federation.syncUlordHeaders.enabled") == "true" ? true : false;
        this.isPegEnabled = config.getString("federation.peg.enabled") == "true" ? true : false;
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

            Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(fedMain.params, fedMain.feePerkbAuthorizedAddress, fedMain.feePerkbAuthorizedPassword));
            syncUlordHeaders.start();
        }
    }

    @Override
    public void run() {
        startPeg(bridgeConstants, changeAuthorizedAddress, changeAuthorizedPassword, ulordFederationAddress);
    }


    public void startPeg(BridgeConstants bridgeConstants, String authorizedAddress, String pwd, String[] addresses) {
        NetworkParameters params;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            params = TestNet3Params.get();
        else
            params = MainNetParams.get();

        while(true) {

            try {
                String getAddressUtxosResponse = UlordCli.getAddressUtxos(params, addresses);
                if(getAddressUtxosResponse.contains("error")) {
                    logger.error(getAddressUtxosResponse);
                    System.out.println(getAddressUtxosResponse);
                    return;
                }
                JSONArray jsonArray = new JSONArray(getAddressUtxosResponse);

                for (int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String txid = jsonObject.get("txid").toString();
                    int height = Integer.parseInt(jsonObject.get("height").toString());

                    // Check if the ulord transaction is already processed in USC
                    String data = DataEncoder.encodeIsUldTxHashAlreadyProcessed(txid);
                    JSONObject jsObj = new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                    logger.info(jsObj.toString());
                    String result = jsObj.get("result").toString();

                    if (result.substring(result.length() - 1, result.length()).equals("1")) {
                        logger.info("Transaction " + txid + " already processed");
                        System.out.println("Tx already processed: " + txid);
                        continue;
                    }

                    JSONObject jsonObj = new JSONObject(UscRpc.getUldBlockChainBestChainHeight());
                    int chainHeadHeight = Integer.decode(jsonObj.get("result").toString());
                    if (chainHeadHeight < height + bridgeConstants.getUld2UscMinimumAcceptableConfirmations()) {
                        logger.info("Supplied transaction height " + height + " is greater than Chainhead height " + chainHeadHeight);
                        System.out.println("Supplied transaction height " + height + " is greater than Chainhead height " + chainHeadHeight);
                        continue;
                    }

                    // Here we can register Ulord transactions in USC
                    logger.info("Transaction " + txid + " sent to USC");
                    RegisterUlordTransaction.register(bridgeConstants, authorizedAddress, pwd, txid);
                }

                // Get gasPrice
                JSONObject getGasPriceJSON = new JSONObject(UscRpc.gasPrice());
                String gasPrice = getGasPriceJSON.getString("result");

                if(gasPrice.equals("0"))
                    gasPrice = null;

                String sendTxResponse = UscRpc.sendTransaction(authorizedAddress,
                        PrecompiledContracts.BRIDGE_ADDR_STR,
                        "0x3D0900",
                        gasPrice,
                        null,
                        Hex.toHexString(Bridge.UPDATE_COLLECTIONS.encodeSignature()),
                        null
                );
                logger.info(sendTxResponse);
                System.out.println("FederationMain: " + sendTxResponse);

                // Try to unlock account
                Utils.tryUnlockUscAccount(authorizedAddress, pwd);

                // Try to release any pending transaction.
                ReleaseUlordTransaction.release(bridgeConstants, authorizedAddress, pwd);

                Thread.sleep(1000 * 60 * 5);
            } catch (Exception e) {
                logger.warn(e.toString());
                System.out.println("FederationMain: " + e);
            }
        }
    }
}
