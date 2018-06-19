package tools;

import co.usc.ulordj.core.NetworkParameters;
import co.usc.ulordj.params.TestNet3Params;

import java.io.IOException;

public class UlordCli {
    private static String getUtNetworkCommand(NetworkParameters params) {
        if(params instanceof TestNet3Params)
            return NetworkConstants.ULORD_CLI + NetworkConstants.ULORD_TESTNET;
        return NetworkConstants.ULORD_CLI;
    }

    public static String getBlock(NetworkParameters params, String hash, boolean jsonFormat) throws IOException {
        String res = UlordCliExecutor.execute(getUtNetworkCommand(params) + " getblock " + hash + " "  + String.valueOf(jsonFormat));
        return res;
    }

    public static String getRawTransaction(NetworkParameters params, String txId, boolean jsonFormat) throws IOException {
        int val = jsonFormat == true ? 1 : 0;
        String result = UlordCliExecutor.execute(getUtNetworkCommand(params) + " getrawtransaction " + txId + " " + val);
        return result;
    }

    public  static String getBlockCount(NetworkParameters params) throws IOException {
        return UlordCliExecutor.execute(getUtNetworkCommand(params) + " getblockcount");
    }
}
