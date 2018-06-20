package tools;

import co.usc.config.BridgeConstants;
import co.usc.config.BridgeTestNetConstants;
import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.MainNetParams;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;

public class FederationMain {
    public static void main(String[]  args) {
        String[] add = {"sY5XfaKEej45QBkw5cQwpiconeg7SqYLYL"};
        BridgeConstants bridgeConstants = BridgeTestNetConstants.getInstance();
        monitorUtxosAndReleaseUlordTx(bridgeConstants,"674f05e1916abc32a38f40aa67ae6b503b565999", "abcd1234", add);

        Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(TestNet3Params.get()));
        syncUlordHeaders.start();
    }

    public static void monitorUtxosAndReleaseUlordTx(BridgeConstants bridgeConstants, String fedAddress, String pwd, String[] address) {
        NetworkParameters params;
        if(bridgeConstants instanceof BridgeTestNetConstants)
            params = TestNet3Params.get();
        else
            params = MainNetParams.get();

        try {
            JSONArray jsonArray  = new JSONArray(UlordCli.getAddressUtxos(params, address));
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String txid = jsonObject.get("txid").toString();

                // Check if the ulord transaction is already processed in USC
                String data = DataEncoder.encodeIsUldTxHashAlreadyProcessed(txid);
                JSONObject jsObj =  new JSONObject(UscRpc.call(PrecompiledContracts.BRIDGE_ADDR_STR, data));
                String result = jsObj.get("result").toString();
                if(result.substring(result.length()-1, result.length()).equals("1"))
                    return;

                // Here we can register Ulord transactions in USC
                boolean res = RegisterUlordTransaction.register(bridgeConstants, fedAddress, pwd, txid);
                if(res)
                    System.out.println("Transaction " + txid + " successfully processed");
                else
                    System.out.println("Transaction " + txid + " failed");

            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
