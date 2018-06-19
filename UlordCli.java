package tools;

import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.TestNet3Params;

import java.io.IOException;

public class UlordCli {
    private static String getNetworkCommand(NetworkParameters params) {
        if(params instanceof TestNet3Params)
            return NetworkConstants.ULORD_CLI + NetworkConstants.ULORD_TESTNET;
        return NetworkConstants.ULORD_CLI;
    }

    public static String getBlock(NetworkParameters params, String hash, boolean jsonFormat) throws IOException {
        String res = UlordCliExecutor.execute(getNetworkCommand(params) + " getblock " + hash + " "  + String.valueOf(jsonFormat));
        return res;
    }

    public static String getRawTransaction(NetworkParameters params, String txId, boolean jsonFormat) throws IOException {
        int val = jsonFormat == true ? 1 : 0;
        String result = UlordCliExecutor.execute(getNetworkCommand(params) + " getrawtransaction " + txId + " " + val);
        return result;
    }

    public  static String getBlockCount(NetworkParameters params) throws IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " getblockcount");
    }

    public static String sendRawTransaction(NetworkParameters params, String txHex) throws  IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " sendrawtransaction " + txHex);
    }

    public static String decodeRawTransaction(NetworkParameters params, String rawTxHex) throws IOException {
        return UlordCliExecutor.execute(getNetworkCommand(params) + " decoderawtransaction " + rawTxHex);
    }

    public static String getAddressUtxos(NetworkParameters params, String[] addresses) throws IOException {
        String adds = "";
        for (int i = 0; i < addresses.length; ++i) {
            if(i == addresses.length - 1)
               adds += "\"" + addresses[i] + "\"";
            else
                adds +=  "\"" + addresses[i] + "\",";
        }
        return UlordCliExecutor.execute(getNetworkCommand(params) + " getaddressutxos '{\"addresses\":[" + adds + "]}'");
    }
}
