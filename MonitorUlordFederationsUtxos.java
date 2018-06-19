package tools;

import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.TestNet3Params;
import org.json.JSONArray;
import org.json.JSONObject;

public class MonitorUlordFederationsUtxos {
    public static void main(String[]  args) {
        String[] add = {"sY5XfaKEej45QBkw5cQwpiconeg7SqYLYL", "sgwxX756HvCzKrsWSMmEpJDXxXcZhrHg3n"};
        startMonitoring(TestNet3Params.get(), add);
    }

    public static void startMonitoring(NetworkParameters params, String[] address) {
        try {
            JSONArray jsonArray  = new JSONArray(UlordCli.getAddressUtxos(params, address));
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String txid = jsonObject.get("txid").toString();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
