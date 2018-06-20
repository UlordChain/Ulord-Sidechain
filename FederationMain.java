package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;

public class FederationMain implements Runnable {

    //TODO: Move those settings to a config file
    BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
    NetworkParameters params = TestNet3Params.get();

    String[] ulordFederationAddress = {"sY5XfaKEej45QBkw5cQwpiconeg7SqYLYL"};
    String federationChangeAuthorizedAddress = "674f05e1916abc32a38f40aa67ae6b503b565999";
    String pwd = "abcd1234";

    public static void main(String[]  args) {
        FederationMain fedMain = new FederationMain();
        Thread releaseUlordTransactions = new Thread(fedMain);
        releaseUlordTransactions.start();

        Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(fedMain.params));
        syncUlordHeaders.start();
    }

    @Override
    public void run() {
        monitorUtxosAndReleaseUlordTx(bridgeConstants, federationChangeAuthorizedAddress,pwd, ulordFederationAddress);
    }


    public void monitorUtxosAndReleaseUlordTx(BridgeConstants bridgeConstants, String fromFedAddress, String pwd, String[] address) {
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

                    // Check if the ulord transaction is already processed in USC
                    String data = DataEncoder.encodeIsUldTxHashAlreadyProcessed(txid);
                    JSONObject jsObj = new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                    String result = jsObj.get("result").toString();
                    if (result.substring(result.length() - 1, result.length()).equals("1")) {
                        System.out.println("Tx already processed: " + txid);
                        return;
                    }

                    // Here we can register Ulord transactions in USC
                    RegisterUlordTransaction.register(bridgeConstants, fromFedAddress, pwd, txid);
                }
                Thread.sleep(1000 * 1);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }


}
