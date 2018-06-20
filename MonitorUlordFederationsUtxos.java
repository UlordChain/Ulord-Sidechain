package tools;

import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.TestNet3Params;
import org.ethereum.vm.PrecompiledContracts;
import org.json.JSONArray;
import org.json.JSONObject;

public class MonitorUlordFederationsUtxos {
    public static void main(String[]  args) {
        String[] add = {"sY5XfaKEej45QBkw5cQwpiconeg7SqYLYL", "sgwxX756HvCzKrsWSMmEpJDXxXcZhrHg3n"};
        startMonitoring(TestNet3Params.get(), add);

        Thread syncUlordHeaders = new Thread(new SyncUlordHeaders(TestNet3Params.get()));
        syncUlordHeaders.start();
    }

    public static void startMonitoring(NetworkParameters params, String[] address) {
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


            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
